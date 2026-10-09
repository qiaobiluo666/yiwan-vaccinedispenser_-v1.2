package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Java自动上药逐盒Redis预留管理器。
 *
 * <p>所有写操作都使用StringRedisTemplate和Lua脚本，确保仓位身份校验、容量校验、
 * 逐盒写入及pending增减在Redis内一次完成。这里的数据只是在一次自动上药运行期间
 * 防止findBox重复占用仓位的临时容量锁，不是真实库存，也不参与库存盘点。</p>
 */
@Slf4j
@Component
public class AutoDrugReservationManager {

    private static final int MAX_CAS_RETRY = 5;

    private static final List<String> RESERVATION_KEYS = List.of(
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_OWNERS,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_IDENTITIES,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_CAPACITY,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ACTIVE_SESSION
    );

    private static final List<String> SESSION_KEYS = List.of(
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ACTIVE_SESSION,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_OWNERS,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_IDENTITIES,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING,
            RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_CAPACITY
    );

    private static final DefaultRedisScript<String> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local reservationId = ARGV[1]
            local expectedOwner = ARGV[2]
            local machineId = ARGV[3]
            local expectedIdentity = ARGV[4]
            local itemJson = ARGV[5]
            local dbCount = tonumber(ARGV[6])
            local requestedCapacity = tonumber(ARGV[7])
            local expectedSession = ARGV[8]

            local activeSession = redis.call('GET', KEYS[6])
            if not activeSession then
                return 'SESSION_MISSING:0'
            end
            if activeSession ~= expectedSession then
                return 'SESSION_MISMATCH:0'
            end

            local currentOwner = redis.call('HGET', KEYS[2], reservationId)
            local currentItem = redis.call('HGET', KEYS[1], reservationId)
            if currentOwner then
                if currentOwner ~= expectedOwner then
                    return 'RESERVATION_CONFLICT:0'
                end
                if not currentItem then
                    return 'INCONSISTENT:0'
                end
                local existingCount = tonumber(redis.call('HGET', KEYS[4], machineId) or '-1')
                local existingIdentity = redis.call('HGET', KEYS[3], machineId)
                local existingCapacity = redis.call('HGET', KEYS[5], machineId)
                if existingCount <= 0 or existingIdentity ~= expectedIdentity or not existingCapacity then
                    return 'INCONSISTENT:0'
                end
                return 'ALREADY_RESERVED:' .. tostring(existingCount)
            end
            if currentItem then
                return 'INCONSISTENT:0'
            end

            local currentIdentity = redis.call('HGET', KEYS[3], machineId)
            local currentCountValue = redis.call('HGET', KEYS[4], machineId)
            local currentCapacityValue = redis.call('HGET', KEYS[5], machineId)
            local currentCount = tonumber(currentCountValue or '0')

            if currentIdentity then
                if currentIdentity ~= expectedIdentity then
                    return 'MACHINE_CONFLICT:' .. tostring(currentCount)
                end
                if currentCount <= 0 or not currentCapacityValue then
                    return 'INCONSISTENT:0'
                end
            elseif currentCount ~= 0 or currentCapacityValue then
                return 'INCONSISTENT:0'
            end

            local effectiveCapacity = requestedCapacity
            local currentCapacity = tonumber(currentCapacityValue or tostring(requestedCapacity))
            if currentCapacity < effectiveCapacity then
                effectiveCapacity = currentCapacity
            end
            if dbCount + currentCount >= effectiveCapacity then
                return 'CAPACITY_FULL:' .. tostring(currentCount)
            end

            if not currentIdentity then
                redis.call('HSET', KEYS[3], machineId, expectedIdentity)
                redis.call('HSET', KEYS[5], machineId, tostring(requestedCapacity))
            elseif requestedCapacity < currentCapacity then
                redis.call('HSET', KEYS[5], machineId, tostring(requestedCapacity))
            end
            redis.call('HSET', KEYS[2], reservationId, expectedOwner)
            redis.call('HSET', KEYS[1], reservationId, itemJson)
            local newCount = redis.call('HINCRBY', KEYS[4], machineId, 1)
            return 'RESERVED:' .. tostring(newCount)
            """, String.class);

    private static final DefaultRedisScript<String> BEGIN_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local requestedSession = ARGV[1]
            local residualCount = redis.call('HLEN', KEYS[2])
            local ownerCount = redis.call('HLEN', KEYS[3])
            local identityCount = redis.call('HLEN', KEYS[4])
            local pendingCount = redis.call('HLEN', KEYS[5])
            local capacityCount = redis.call('HLEN', KEYS[6])
            local currentSession = redis.call('GET', KEYS[1])

            -- 新一轮开始时，上一轮所有预增都已经失效。只清理本模块的临时锁，
            -- 不触碰真实库存、发药、盘点或其他Redis数据。
            redis.call('DEL', KEYS[2], KEYS[3], KEYS[4], KEYS[5], KEYS[6])
            redis.call('SET', KEYS[1], requestedSession)

            if residualCount > 0 or ownerCount > 0 or identityCount > 0
                    or pendingCount > 0 or capacityCount > 0 then
                return 'CLEARED_STALE_RESERVATIONS:' .. tostring(residualCount)
            end
            if not currentSession then
                return 'STARTED:0'
            end
            if currentSession == requestedSession then
                return 'ALREADY_ACTIVE:0'
            end
            return 'REPLACED_ORPHAN_SESSION:0'
            """, String.class);

    private static final DefaultRedisScript<String> ASSERT_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local currentSession = redis.call('GET', KEYS[1])
            if not currentSession then
                return 'SESSION_MISSING:0'
            end
            if currentSession ~= ARGV[1] then
                return 'SESSION_MISMATCH:0'
            end
            return 'ACTIVE:0'
            """, String.class);

    private static final DefaultRedisScript<String> FINISH_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local expectedSession = ARGV[1]
            local currentSession = redis.call('GET', KEYS[1])
            if currentSession and currentSession ~= expectedSession then
                return 'SESSION_MISMATCH:0'
            end

            local residualCount = redis.call('HLEN', KEYS[2])
            redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4], KEYS[5], KEYS[6])
            if currentSession then
                return 'FINISHED:' .. tostring(residualCount)
            end
            return 'ALREADY_FINISHED:' .. tostring(residualCount)
            """, String.class);

    private static final DefaultRedisScript<String> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            local reservationId = ARGV[1]
            local expectedItem = ARGV[2]
            local currentItem = redis.call('HGET', KEYS[1], reservationId)
            local currentOwner = redis.call('HGET', KEYS[2], reservationId)

            if not currentItem and not currentOwner then
                return 'ALREADY_RELEASED:0'
            end
            if not currentItem or not currentOwner then
                return 'INCONSISTENT:0'
            end
            if currentItem ~= expectedItem then
                return 'RETRY:0'
            end

            local separator = string.find(currentOwner, '|', 1, true)
            if not separator then
                return 'INCONSISTENT:0'
            end
            local machineId = string.sub(currentOwner, 1, separator - 1)
            local currentCount = tonumber(redis.call('HGET', KEYS[4], machineId) or '-1')
            local currentIdentity = redis.call('HGET', KEYS[3], machineId)
            local currentCapacity = redis.call('HGET', KEYS[5], machineId)
            if currentCount <= 0 or not currentIdentity or not currentCapacity then
                return 'INCONSISTENT:0'
            end

            redis.call('HDEL', KEYS[1], reservationId)
            redis.call('HDEL', KEYS[2], reservationId)
            local newCount = redis.call('HINCRBY', KEYS[4], machineId, -1)
            if newCount == 0 then
                redis.call('HDEL', KEYS[4], machineId)
                redis.call('HDEL', KEYS[3], machineId)
                redis.call('HDEL', KEYS[5], machineId)
            elseif newCount < 0 then
                return 'INCONSISTENT:0'
            end
            return 'RELEASED:' .. tostring(newCount)
            """, String.class);

    private static final DefaultRedisScript<String> UPDATE_ITEM_SCRIPT = new DefaultRedisScript<>("""
            local currentItem = redis.call('HGET', KEYS[1], ARGV[1])
            if not currentItem then
                return 'ABSENT'
            end
            if currentItem ~= ARGV[2] then
                return 'RETRY'
            end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])
            return 'UPDATED'
            """, String.class);

    private static final DefaultRedisScript<String> CLEAR_ALL_SCRIPT = new DefaultRedisScript<>("""
            local residualCount = redis.call('HLEN', KEYS[1])
            redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4], KEYS[5], KEYS[6])
            return 'CLEARED:' .. tostring(residualCount)
            """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final HashOperations<String, String, String> hashOperations;
    private final ObjectMapper objectMapper;

    /** 当前运行期间Redis异常的诊断信息；成功开始下一轮后自动复位。 */
    private volatile String safetyInterlockReason;

    public AutoDrugReservationManager(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.objectMapper = objectMapper;
    }

    /** 当前运行期间主动停止自动上药；不会跨越下一次成功启动永久闭锁。 */
    public synchronized void tripSafetyInterlock(String reason, Throwable cause) {
        String normalizedReason = requireText(reason, "reason");
        if (safetyInterlockReason == null) {
            safetyInterlockReason = normalizedReason;
            if (cause == null) {
                log.error("自动上药Redis安全闭锁已触发：{}", normalizedReason);
            } else {
                log.error("自动上药Redis安全闭锁已触发：{}", normalizedReason, cause);
            }
        } else if (cause != null) {
            log.error("自动上药Redis安全闭锁期间再次发生异常：{}", normalizedReason, cause);
        }
    }

    public void assertSafetyInterlockClear() {
        String reason = safetyInterlockReason;
        if (reason != null) {
            throw new IllegalStateException(
                    "自动上药Redis运行保护未解除：" + reason);
        }
    }

    public boolean isSafetyInterlockTripped() {
        return safetyInterlockReason != null;
    }

    public String getSafetyInterlockReason() {
        return safetyInterlockReason;
    }

    /**
     * 开启一次自动上药Redis会话。新会话建立前原子清除上一轮临时预增，
     * 因此程序异常退出留下的数据不会阻止操作人员再次启动。
     */
    public synchronized AutoDrugSessionResult beginSession(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        String result = redisTemplate.execute(
                BEGIN_SESSION_SCRIPT,
                SESSION_KEYS,
                normalizedSessionId);
        ScriptResult scriptResult = parseScriptResult(result, "开启会话");
        AutoDrugSessionOutcome outcome;
        String message;
        switch (scriptResult.code()) {
            case "STARTED" -> {
                outcome = AutoDrugSessionOutcome.STARTED;
                message = "自动上药Redis会话已建立";
            }
            case "ALREADY_ACTIVE" -> {
                outcome = AutoDrugSessionOutcome.ALREADY_ACTIVE;
                message = "同一自动上药Redis会话已经生效";
            }
            case "REPLACED_ORPHAN_SESSION" -> {
                outcome = AutoDrugSessionOutcome.REPLACED_ORPHAN_SESSION;
                message = "无逐盒残留，已替换孤儿Redis会话";
                log.warn("自动上药无逐盒残留，替换孤儿Redis会话：newSessionId={}", normalizedSessionId);
            }
            case "CLEARED_STALE_RESERVATIONS" -> {
                outcome = AutoDrugSessionOutcome.CLEARED_STALE_RESERVATIONS;
                message = "已自动释放上一轮自动上药临时预增";
                log.warn("开始自动上药前已自动释放上一轮临时预增：newSessionId={}, reservationCount={}",
                        normalizedSessionId, scriptResult.pendingCount());
            }
            default -> throw inconsistent("未知开启会话脚本结果：" + scriptResult.code());
        }
        // Redis脚本已成功建立新会话，上一轮运行期间的Redis保护不应继续影响本轮。
        safetyInterlockReason = null;
        return AutoDrugSessionResult.builder()
                .outcome(outcome)
                .sessionId(normalizedSessionId)
                .residualCount(scriptResult.pendingCount())
                .message(message)
                .build();
    }

    /** Redis被flush/重启或会话被其他流程替换时抛错，调用方必须失败关闭。 */
    public void assertSession(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        String result;
        try {
            result = redisTemplate.execute(
                    ASSERT_SESSION_SCRIPT,
                    List.of(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ACTIVE_SESSION),
                    normalizedSessionId);
        } catch (RuntimeException redisError) {
            throw inconsistent("校验会话时Redis访问失败，必须停止上药", redisError);
        }
        ScriptResult scriptResult = parseScriptResult(result, "校验会话");
        if ("ACTIVE".equals(scriptResult.code())) {
            return;
        }
        if ("SESSION_MISSING".equals(scriptResult.code())) {
            throw inconsistent("自动上药会话Key已丢失，Redis可能重启或被清空，必须停止上药");
        }
        if ("SESSION_MISMATCH".equals(scriptResult.code())) {
            throw inconsistent("自动上药会话不匹配，必须停止上药");
        }
        throw inconsistent("未知校验会话脚本结果：" + scriptResult.code());
    }

    /**
     * 停止自动上药并且所有本轮线程都退出后，原子释放本轮全部临时预增和会话Key。
     */
    public synchronized AutoDrugSessionResult finishSessionAndReleaseAll(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        String result;
        try {
            result = redisTemplate.execute(
                    FINISH_SESSION_SCRIPT,
                    SESSION_KEYS,
                    normalizedSessionId);
        } catch (RuntimeException redisError) {
            throw inconsistent("停止自动上药时Redis访问失败，临时预增自动释放失败", redisError);
        }
        ScriptResult scriptResult = parseScriptResult(result, "结束会话");
        AutoDrugSessionOutcome outcome;
        String message;
        switch (scriptResult.code()) {
            case "FINISHED" -> {
                outcome = AutoDrugSessionOutcome.FINISHED;
                message = "自动上药已停止，本轮临时预增已全部释放";
            }
            case "ALREADY_FINISHED" -> {
                outcome = AutoDrugSessionOutcome.ALREADY_FINISHED;
                message = "自动上药临时预增此前已经释放";
            }
            case "SESSION_MISMATCH" -> throw inconsistent("结束会话时sessionId不匹配，拒绝删除会话Key");
            default -> throw inconsistent("未知结束会话脚本结果：" + scriptResult.code());
        }
        return AutoDrugSessionResult.builder()
                .outcome(outcome)
                .sessionId(normalizedSessionId)
                .residualCount(scriptResult.pendingCount())
                .message(message)
                .build();
    }

    /** 兼容旧调用；现在结束会话必定释放本轮所有临时预增。 */
    public synchronized AutoDrugSessionResult finishSessionIfIdle(String sessionId) {
        return finishSessionAndReleaseAll(sessionId);
    }

    /**
     * 当前Java进程没有活动会话时，停止按钮仍可兜底清除程序异常退出留下的临时预增。
     */
    public synchronized long clearAllTemporaryReservations(String reason) {
        String normalizedReason = requireText(reason, "reason");
        String result;
        try {
            result = redisTemplate.execute(CLEAR_ALL_SCRIPT, RESERVATION_KEYS);
        } catch (RuntimeException redisError) {
            throw inconsistent("清除自动上药临时预增时Redis访问失败", redisError);
        }
        ScriptResult scriptResult = parseScriptResult(result, "清除全部临时预增");
        if (!"CLEARED".equals(scriptResult.code())) {
            throw inconsistent("未知清除全部临时预增脚本结果：" + scriptResult.code());
        }
        safetyInterlockReason = null;
        log.info("自动上药临时预增已全部清除：reservationCount={}, reason={}",
                scriptResult.pendingCount(), normalizedReason);
        return scriptResult.pendingCount();
    }

    /**
     * 原子预留一盒药。相同reservationId和相同业务身份的重试返回ALREADY_RESERVED，
     * 不会重复增加pending。
     */
    public synchronized AutoDrugReservationResult reserve(AutoDrugReservationRequest request) {
        AutoDrugReservation reservation = buildReservation(request);
        String identity = buildMachineIdentity(reservation);
        String owner = buildOwnerFingerprint(reservation, identity);
        String result;
        try {
            result = redisTemplate.execute(
                    RESERVE_SCRIPT,
                    RESERVATION_KEYS,
                    reservation.getReservationId(),
                    owner,
                    String.valueOf(reservation.getMachineId()),
                    identity,
                    toJson(reservation),
                    String.valueOf(reservation.getDatabaseCountAtReserve()),
                    String.valueOf(reservation.getMaxCapacity()),
                    reservation.getSessionId()
            );
        } catch (RuntimeException redisError) {
            throw inconsistent("逐盒预留时Redis访问失败，拒绝继续上药", redisError);
        }
        ScriptResult scriptResult = parseScriptResult(result, "预留");

        return switch (scriptResult.code()) {
            case "RESERVED" -> {
                log.info("自动上药仓位预留成功：reservationId={}, 仓位={}({}), 监管码={}, DB库存={}, pending={}, 最大容量={}",
                        reservation.getReservationId(), reservation.getBoxNo(), reservation.getMachineId(),
                        reservation.getSupervisedCode(), reservation.getDatabaseCountAtReserve(),
                        scriptResult.pendingCount(), reservation.getMaxCapacity());
                yield result(AutoDrugReservationOutcome.RESERVED, reservation,
                        scriptResult.pendingCount(), "仓位预留成功");
            }
            case "ALREADY_RESERVED" -> {
                AutoDrugReservation existing = findById(reservation.getReservationId())
                        .orElseThrow(() -> inconsistent("幂等预留返回成功但逐盒记录不存在"));
                yield result(AutoDrugReservationOutcome.ALREADY_RESERVED, existing,
                        scriptResult.pendingCount(), "该药盒已经预留，未重复计数");
            }
            case "CAPACITY_FULL" -> result(AutoDrugReservationOutcome.CAPACITY_FULL, reservation,
                    scriptResult.pendingCount(), "数据库库存加在途数量已达到仓位最大容量");
            case "MACHINE_CONFLICT" -> result(AutoDrugReservationOutcome.MACHINE_PRODUCT_CONFLICT, reservation,
                    scriptResult.pendingCount(), "仓位已被其他产品、批次或有效期预留");
            case "RESERVATION_CONFLICT" -> result(AutoDrugReservationOutcome.RESERVATION_ID_CONFLICT, reservation,
                    scriptResult.pendingCount(), "reservationId已被另一盒药占用");
            case "SESSION_MISSING" -> throw inconsistent(
                    "自动上药会话Key已丢失，Redis可能重启或被清空，拒绝预留并必须停止上药");
            case "SESSION_MISMATCH" -> throw inconsistent("自动上药会话不匹配，拒绝预留并必须停止上药");
            case "INCONSISTENT" -> throw inconsistent("预留时发现Redis聚合数据不一致");
            default -> throw inconsistent("未知预留脚本结果：" + scriptResult.code());
        };
    }

    public Optional<AutoDrugReservation> markInTransit(String reservationId) {
        return updateState(reservationId, AutoDrugReservationState.IN_TRANSIT, "药盒已进入A柜机械手");
    }

    /** 异常后物理位置不明确，保留pending且不允许普通release。 */
    public Optional<AutoDrugReservation> markUncertain(String reservationId, String reason) {
        return updateState(reservationId, AutoDrugReservationState.UNCERTAIN, requireReason(reason));
    }

    /** 机械手底部传感器已确认药盒离手但数据库提交失败，保留pending且不允许普通release。 */
    public Optional<AutoDrugReservation> markCommitFailed(String reservationId, String reason) {
        return updateState(reservationId, AutoDrugReservationState.COMMIT_FAILED, requireReason(reason));
    }

    /**
     * 幂等释放。只能在数据库提交成功或药盒已确认退回后调用；
     * UNCERTAIN/COMMIT_FAILED必须走resolveAndRelease。
     */
    public synchronized AutoDrugReleaseResult release(String reservationId) {
        return releaseInternal(reservationId, false, null);
    }

    /**
     * 人工检查或可靠对账完成后的显式解除入口，不应作为普通异常finally清理使用。
     */
    public synchronized AutoDrugReleaseResult resolveAndRelease(String reservationId, String resolution) {
        return releaseInternal(reservationId, true, requireReason(resolution));
    }

    public Optional<AutoDrugReservation> findById(String reservationId) {
        String normalizedId = requireText(reservationId, "reservationId");
        String json = hashOperations.get(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, normalizedId);
        return json == null ? Optional.empty() : Optional.of(fromJson(json));
    }

    /** 查询全部未结预留；包括UNCERTAIN和COMMIT_FAILED。 */
    public List<AutoDrugReservation> findResidualReservations() {
        Map<String, String> entries = hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS);
        return entries.values().stream()
                .map(this::fromJson)
                .sorted(Comparator.comparing(AutoDrugReservation::getCreatedAtEpochMillis,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public List<AutoDrugReservation> findResidualReservationsByMachine(Long machineId) {
        requireMachineId(machineId);
        return findResidualReservations().stream()
                .filter(item -> Objects.equals(machineId, item.getMachineId()))
                .toList();
    }

    public List<AutoDrugReservation> findResidualReservationsBySession(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId");
        return findResidualReservations().stream()
                .filter(item -> normalizedSessionId.equals(item.getSessionId()))
                .toList();
    }

    public List<AutoDrugReservation> findProblemReservations() {
        return findResidualReservations().stream()
                .filter(item -> item.getState() == AutoDrugReservationState.UNCERTAIN
                        || item.getState() == AutoDrugReservationState.COMMIT_FAILED)
                .toList();
    }

    /**
     * 返回同产品、同批次、同有效期的已预留仓位，供选仓时优先复用。
     * null批次与空批次按一致处理。
     */
    public List<AutoDrugMachineReservationSnapshot> findMatchingMachineReservations(
            String productNo, String batchNo, Date expiredAt) {
        String expectedProductNo = requireText(productNo, "productNo");
        String expectedBatchNo = normalizeOptional(batchNo);
        Long expectedExpiredAt = expiredAt == null ? null : expiredAt.getTime();
        return findAllMachineSnapshots().stream()
                .filter(snapshot -> expectedProductNo.equals(snapshot.getProductNo()))
                .filter(snapshot -> expectedBatchNo.equals(normalizeOptional(snapshot.getBatchNo())))
                .filter(snapshot -> Objects.equals(expectedExpiredAt, snapshot.getExpiredAtEpochMillis()))
                .toList();
    }

    /** 聚合并校验每个仓位的pending计数。Redis内部不一致时直接抛错，调用方应停止上药。 */
    public synchronized List<AutoDrugMachineReservationSnapshot> findAllMachineSnapshots() {
        List<AutoDrugReservation> reservations = findResidualReservations();
        Map<Long, List<AutoDrugReservation>> grouped = reservations.stream()
                .collect(Collectors.groupingBy(AutoDrugReservation::getMachineId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, String> pendingMap = hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING);
        Map<String, String> capacityMap = hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_CAPACITY);
        Map<String, String> identityMap = hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_IDENTITIES);
        Map<String, String> ownerMap = hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_OWNERS);

        assertSameFields("逐盒记录与owner索引", reservations.stream()
                        .map(AutoDrugReservation::getReservationId).collect(Collectors.toSet()),
                ownerMap.keySet());
        Set<String> machineFields = grouped.keySet().stream()
                .map(String::valueOf).collect(Collectors.toSet());
        assertSameFields("仓位pending", machineFields, pendingMap.keySet());
        assertSameFields("仓位identity", machineFields, identityMap.keySet());
        assertSameFields("仓位capacity", machineFields, capacityMap.keySet());

        List<AutoDrugMachineReservationSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<Long, List<AutoDrugReservation>> entry : grouped.entrySet()) {
            Long machineId = entry.getKey();
            List<AutoDrugReservation> items = entry.getValue();
            AutoDrugReservation first = items.get(0);
            String machineField = String.valueOf(machineId);
            long pending = parsePositiveLong(pendingMap.get(machineField), "pending", machineField);
            int capacity = parsePositiveInt(capacityMap.get(machineField), "capacity", machineField);
            if (pending != items.size()) {
                throw inconsistent("仓位" + machineField + "的pending=" + pending
                        + "，但逐盒记录数=" + items.size());
            }
            String expectedIdentity = buildMachineIdentity(first);
            if (!expectedIdentity.equals(identityMap.get(machineField))) {
                throw inconsistent("仓位" + machineField + "的身份索引与逐盒记录不一致");
            }
            for (AutoDrugReservation item : items) {
                if (!expectedIdentity.equals(buildMachineIdentity(item))) {
                    throw inconsistent("同一仓位存在不同产品、批次或有效期的预留：" + machineField);
                }
                String expectedOwner = buildOwnerFingerprint(item, expectedIdentity);
                if (!expectedOwner.equals(ownerMap.get(item.getReservationId()))) {
                    throw inconsistent("逐盒owner索引不一致：" + item.getReservationId());
                }
            }

            snapshots.add(AutoDrugMachineReservationSnapshot.builder()
                    .machineId(machineId)
                    .boxNo(first.getBoxNo())
                    .productNo(first.getProductNo())
                    .productName(first.getProductName())
                    .batchNo(first.getBatchNo())
                    .expiredAtEpochMillis(first.getExpiredAtEpochMillis())
                    .pendingCount(pending)
                    .maxCapacity(capacity)
                    .reservationIds(items.stream().map(AutoDrugReservation::getReservationId).toList())
                    .states(items.stream().map(AutoDrugReservation::getState)
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AutoDrugReservationState.class))))
                    .build());
        }
        snapshots.sort(Comparator.comparing(AutoDrugMachineReservationSnapshot::getBoxNo,
                Comparator.nullsLast(String::compareTo)));
        return snapshots;
    }

    public long getPendingCount(Long machineId) {
        requireMachineId(machineId);
        String value = hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING,
                String.valueOf(machineId));
        return value == null ? 0L : parsePositiveLong(value, "pending", String.valueOf(machineId));
    }

    public boolean hasResidualReservations() {
        Long size = hashOperations.size(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS);
        return size != null && size > 0;
    }

    private synchronized Optional<AutoDrugReservation> updateState(
            String reservationId, AutoDrugReservationState targetState, String reason) {
        String normalizedId = requireText(reservationId, "reservationId");
        for (int retry = 0; retry < MAX_CAS_RETRY; retry++) {
            String currentJson = hashOperations.get(
                    RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, normalizedId);
            if (currentJson == null) {
                return Optional.empty();
            }
            AutoDrugReservation current = fromJson(currentJson);
            AutoDrugReservationState currentState = current.getState();
            if (currentState == targetState && Objects.equals(current.getStateReason(), reason)) {
                return Optional.of(current);
            }
            validateStateTransition(currentState, targetState);
            current.setState(targetState);
            current.setStateReason(reason);
            current.setUpdatedAtEpochMillis(System.currentTimeMillis());
            String updatedJson = toJson(current);
            String result;
            try {
                result = redisTemplate.execute(
                        UPDATE_ITEM_SCRIPT,
                        List.of(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS),
                        normalizedId, currentJson, updatedJson);
            } catch (RuntimeException redisError) {
                throw inconsistent("更新逐盒预留状态时Redis访问失败：" + normalizedId,
                        redisError);
            }
            if ("UPDATED".equals(result)) {
                log.warn("自动上药预留状态变更：reservationId={}, 仓位={}({}), state={}, reason={}",
                        current.getReservationId(), current.getBoxNo(), current.getMachineId(),
                        targetState, reason);
                return Optional.of(current);
            }
            if ("ABSENT".equals(result)) {
                return Optional.empty();
            }
            if (!"RETRY".equals(result)) {
                throw inconsistent("未知状态更新脚本结果：" + result);
            }
        }
        throw inconsistent("预留状态连续并发变更，超过重试上限：" + normalizedId);
    }

    private AutoDrugReleaseResult releaseInternal(
            String reservationId, boolean allowProblemState, String resolution) {
        String normalizedId = requireText(reservationId, "reservationId");
        for (int retry = 0; retry < MAX_CAS_RETRY; retry++) {
            String currentJson = hashOperations.get(
                    RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, normalizedId);
            AutoDrugReservation current = currentJson == null ? null : fromJson(currentJson);
            if (current != null && isProblemState(current.getState()) && !allowProblemState) {
                return AutoDrugReleaseResult.builder()
                        .outcome(AutoDrugReleaseOutcome.BLOCKED_PROBLEM_STATE)
                        .reservationId(normalizedId)
                        .machineId(current.getMachineId())
                        .pendingCount(getPendingCount(current.getMachineId()))
                        .message("异常预留必须人工确认或可靠对账后显式解除")
                        .build();
            }

            String result;
            try {
                result = redisTemplate.execute(
                        RELEASE_SCRIPT,
                        RESERVATION_KEYS,
                        normalizedId,
                        currentJson == null ? "" : currentJson);
            } catch (RuntimeException redisError) {
                throw inconsistent("释放逐盒预留时Redis访问失败：" + normalizedId,
                        redisError);
            }
            ScriptResult scriptResult = parseScriptResult(result, "释放");
            if ("RELEASED".equals(scriptResult.code())) {
                if (allowProblemState) {
                    log.warn("人工/对账解除自动上药异常预留：reservationId={}, 仓位={}({}), resolution={}",
                            normalizedId, current == null ? null : current.getBoxNo(),
                            current == null ? null : current.getMachineId(), resolution);
                } else {
                    log.info("自动上药仓位预留已释放：reservationId={}, 仓位={}({}), 剩余pending={}",
                            normalizedId, current == null ? null : current.getBoxNo(),
                            current == null ? null : current.getMachineId(), scriptResult.pendingCount());
                }
                return AutoDrugReleaseResult.builder()
                        .outcome(AutoDrugReleaseOutcome.RELEASED)
                        .reservationId(normalizedId)
                        .machineId(current == null ? null : current.getMachineId())
                        .pendingCount(scriptResult.pendingCount())
                        .message("预留已释放")
                        .build();
            }
            if ("ALREADY_RELEASED".equals(scriptResult.code())) {
                return AutoDrugReleaseResult.builder()
                        .outcome(AutoDrugReleaseOutcome.ALREADY_RELEASED)
                        .reservationId(normalizedId)
                        .pendingCount(0)
                        .message("预留此前已释放")
                        .build();
            }
            if ("RETRY".equals(scriptResult.code())) {
                continue;
            }
            if ("INCONSISTENT".equals(scriptResult.code())) {
                throw inconsistent("释放时发现Redis聚合数据不一致：" + normalizedId);
            }
            throw inconsistent("未知释放脚本结果：" + scriptResult.code());
        }
        throw inconsistent("预留连续并发变更，释放超过重试上限：" + normalizedId);
    }

    private AutoDrugReservation buildReservation(AutoDrugReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("预留请求不能为空");
        }
        String reservationId = hasText(request.getReservationId())
                ? request.getReservationId().trim() : UUID.randomUUID().toString();
        String sessionId = requireText(request.getSessionId(), "sessionId");
        Long machineId = requireMachineId(request.getMachineId());
        String productNo = requireText(request.getProductNo(), "productNo");
        int databaseCount = requireNonNegative(request.getDatabaseCount(), "databaseCount");
        int maxCapacity = requirePositive(request.getMaxCapacity(), "maxCapacity");
        long now = System.currentTimeMillis();
        return AutoDrugReservation.builder()
                .reservationId(reservationId)
                .sessionId(sessionId)
                .machineId(machineId)
                .boxNo(normalizeNullable(request.getBoxNo()))
                .productNo(productNo)
                .productName(normalizeNullable(request.getProductName()))
                .batchNo(normalizeOptional(request.getBatchNo()))
                .expiredAtEpochMillis(request.getExpiredAt() == null ? null : request.getExpiredAt().getTime())
                .supervisedCode(normalizeNullable(request.getSupervisedCode()))
                .databaseCountAtReserve(databaseCount)
                .maxCapacity(maxCapacity)
                .state(AutoDrugReservationState.RESERVED)
                .stateReason("已选择仓位")
                .createdAtEpochMillis(now)
                .updatedAtEpochMillis(now)
                .build();
    }

    private AutoDrugReservationResult result(AutoDrugReservationOutcome outcome,
                                             AutoDrugReservation reservation,
                                             long pendingCount,
                                             String message) {
        return AutoDrugReservationResult.builder()
                .outcome(outcome)
                .reservation(reservation)
                .pendingCount(pendingCount)
                .message(message)
                .build();
    }

    private String buildMachineIdentity(AutoDrugReservation reservation) {
        return encode(reservation.getProductNo()) + "."
                + encode(normalizeOptional(reservation.getBatchNo())) + "."
                + encode(reservation.getExpiredAtEpochMillis() == null
                ? "" : String.valueOf(reservation.getExpiredAtEpochMillis()));
    }

    private String buildOwnerFingerprint(AutoDrugReservation reservation, String identity) {
        return reservation.getMachineId() + "|"
                + encode(reservation.getSessionId()) + "."
                + identity + "."
                + encode(normalizeOptional(reservation.getSupervisedCode()));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizeOptional(value).getBytes(StandardCharsets.UTF_8));
    }

    private ScriptResult parseScriptResult(String result, String operation) {
        if (!hasText(result)) {
            throw inconsistent(operation + "脚本没有返回结果");
        }
        int separator = result.lastIndexOf(':');
        if (separator <= 0 || separator == result.length() - 1) {
            throw inconsistent(operation + "脚本返回格式错误：" + result);
        }
        String code = result.substring(0, separator);
        try {
            return new ScriptResult(code, Long.parseLong(result.substring(separator + 1)));
        } catch (NumberFormatException e) {
            throw inconsistent(operation + "脚本pending格式错误：" + result, e);
        }
    }

    private String toJson(AutoDrugReservation reservation) {
        try {
            return objectMapper.writeValueAsString(reservation);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("自动上药预留序列化失败", e);
        }
    }

    private AutoDrugReservation fromJson(String json) {
        try {
            AutoDrugReservation reservation = objectMapper.readValue(json, AutoDrugReservation.class);
            if (reservation.getReservationId() == null || reservation.getMachineId() == null
                    || reservation.getSessionId() == null || reservation.getProductNo() == null
                    || reservation.getDatabaseCountAtReserve() == null
                    || reservation.getMaxCapacity() == null || reservation.getState() == null) {
                throw inconsistent("逐盒预留缺少关键字段：" + json);
            }
            return reservation;
        } catch (JsonProcessingException e) {
            throw inconsistent("自动上药残留预留JSON损坏", e);
        }
    }

    private void validateStateTransition(AutoDrugReservationState current,
                                         AutoDrugReservationState target) {
        if (current == null) {
            throw inconsistent("逐盒预留状态为空");
        }
        if (current == AutoDrugReservationState.COMMIT_FAILED
                && target != AutoDrugReservationState.COMMIT_FAILED) {
            throw new IllegalStateException("COMMIT_FAILED只能经对账后解除，不能改为" + target);
        }
        if (current == AutoDrugReservationState.UNCERTAIN
                && target == AutoDrugReservationState.IN_TRANSIT) {
            throw new IllegalStateException("UNCERTAIN不能恢复为IN_TRANSIT，只能人工确认或标记提交失败");
        }
        if (current == AutoDrugReservationState.IN_TRANSIT
                && target == AutoDrugReservationState.RESERVED) {
            throw new IllegalStateException("IN_TRANSIT不能回退为RESERVED");
        }
    }

    private boolean isProblemState(AutoDrugReservationState state) {
        return state == AutoDrugReservationState.UNCERTAIN
                || state == AutoDrugReservationState.COMMIT_FAILED;
    }

    private void assertSameFields(String name, Set<String> expected, Set<String> actual) {
        if (!new HashSet<>(expected).equals(new HashSet<>(actual))) {
            throw inconsistent(name + "字段不一致，expected=" + expected + "，actual=" + actual);
        }
    }

    private long parsePositiveLong(String value, String name, String machineId) {
        try {
            long number = Long.parseLong(value);
            if (number <= 0) {
                throw new NumberFormatException("not positive");
            }
            return number;
        } catch (RuntimeException e) {
            throw inconsistent("仓位" + machineId + "的" + name + "不是正整数：" + value, e);
        }
    }

    private int parsePositiveInt(String value, String name, String machineId) {
        long number = parsePositiveLong(value, name, machineId);
        if (number > Integer.MAX_VALUE) {
            throw inconsistent("仓位" + machineId + "的" + name + "超过整数范围：" + value);
        }
        return (int) number;
    }

    private Long requireMachineId(Long machineId) {
        if (machineId == null || machineId <= 0) {
            throw new IllegalArgumentException("machineId必须为正整数");
        }
        return machineId;
    }

    private int requireNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + "不能小于0");
        }
        return value;
    }

    private int requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + "必须大于0");
        }
        return value;
    }

    private String requireReason(String value) {
        return requireText(value, "reason");
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private IllegalStateException inconsistent(String message) {
        tripSafetyInterlock(message, null);
        return new IllegalStateException("自动上药Redis预留异常：" + message);
    }

    private IllegalStateException inconsistent(String message, Throwable cause) {
        tripSafetyInterlock(message, cause);
        return new IllegalStateException("自动上药Redis预留异常：" + message, cause);
    }

    private record ScriptResult(String code, long pendingCount) {
    }
}
