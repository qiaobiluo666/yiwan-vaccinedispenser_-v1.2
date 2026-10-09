package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoDrugReservationManagerTest {

    private StringRedisTemplate redisTemplate;
    private HashOperations<String, String, String> hashOperations;
    private ObjectMapper objectMapper;
    private AutoDrugReservationManager manager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        objectMapper = new ObjectMapper();
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        manager = new AutoDrugReservationManager(redisTemplate, objectMapper);
    }

    @Test
    void databaseNineOfTenAllowsOnlyOnePendingBox() {
        RedisScript<String> reserveScript = script("RESERVE_SCRIPT");
        when(redisTemplate.execute(
                eq(reserveScript), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn("RESERVED:1", "CAPACITY_FULL:1");

        AutoDrugReservationResult first = manager.reserve(request("box-1", 9, 10));
        AutoDrugReservationResult second = manager.reserve(request("box-2", 9, 10));

        assertThat(first.getOutcome()).isEqualTo(AutoDrugReservationOutcome.RESERVED);
        assertThat(first.getPendingCount()).isEqualTo(1);
        assertThat(second.getOutcome()).isEqualTo(AutoDrugReservationOutcome.CAPACITY_FULL);
        assertThat(second.getPendingCount()).isEqualTo(1);
        assertThat(second.isReserved()).isFalse();
    }

    @Test
    void retryingSameReservationIdDoesNotIncreasePendingAgain() throws Exception {
        RedisScript<String> reserveScript = script("RESERVE_SCRIPT");
        when(redisTemplate.execute(
                eq(reserveScript), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn("RESERVED:1", "ALREADY_RESERVED:1");
        AutoDrugReservation existing = reservation(
                "same-box", 26L, AutoDrugReservationState.RESERVED);
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "same-box"))
                .thenReturn(objectMapper.writeValueAsString(existing));

        AutoDrugReservationResult first = manager.reserve(request("same-box", 5, 10));
        AutoDrugReservationResult retry = manager.reserve(request("same-box", 5, 10));

        assertThat(first.getOutcome()).isEqualTo(AutoDrugReservationOutcome.RESERVED);
        assertThat(retry.getOutcome()).isEqualTo(AutoDrugReservationOutcome.ALREADY_RESERVED);
        assertThat(retry.getPendingCount()).isEqualTo(1);
        assertThat(retry.getReservation().getReservationId()).isEqualTo("same-box");
    }

    @Test
    void releasesTwoBoxesFromSameMachineOneByOne() throws Exception {
        RedisScript<String> releaseScript = script("RELEASE_SCRIPT");
        AutoDrugReservation first = reservation("box-1", 26L, AutoDrugReservationState.IN_TRANSIT);
        AutoDrugReservation second = reservation("box-2", 26L, AutoDrugReservationState.IN_TRANSIT);
        String firstJson = objectMapper.writeValueAsString(first);
        String secondJson = objectMapper.writeValueAsString(second);
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "box-1"))
                .thenReturn(firstJson);
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "box-2"))
                .thenReturn(secondJson);
        when(redisTemplate.execute(
                eq(releaseScript), anyList(), eq("box-1"), eq(firstJson)))
                .thenReturn("RELEASED:1");
        when(redisTemplate.execute(
                eq(releaseScript), anyList(), eq("box-2"), eq(secondJson)))
                .thenReturn("RELEASED:0");

        AutoDrugReleaseResult firstRelease = manager.release("box-1");
        AutoDrugReleaseResult secondRelease = manager.release("box-2");

        assertThat(firstRelease.getOutcome()).isEqualTo(AutoDrugReleaseOutcome.RELEASED);
        assertThat(firstRelease.getPendingCount()).isEqualTo(1);
        assertThat(secondRelease.getOutcome()).isEqualTo(AutoDrugReleaseOutcome.RELEASED);
        assertThat(secondRelease.getPendingCount()).isZero();
    }

    @Test
    void uncertainAndCommitFailedReservationsCannotBeNormallyReleased() throws Exception {
        AutoDrugReservation uncertain = reservation(
                "uncertain-box", 26L, AutoDrugReservationState.UNCERTAIN);
        AutoDrugReservation commitFailed = reservation(
                "commit-failed-box", 27L, AutoDrugReservationState.COMMIT_FAILED);
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "uncertain-box"))
                .thenReturn(objectMapper.writeValueAsString(uncertain));
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "commit-failed-box"))
                .thenReturn(objectMapper.writeValueAsString(commitFailed));
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING, "26"))
                .thenReturn("1");
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING, "27"))
                .thenReturn("1");

        AutoDrugReleaseResult uncertainResult = manager.release("uncertain-box");
        AutoDrugReleaseResult commitFailedResult = manager.release("commit-failed-box");

        assertThat(uncertainResult.getOutcome())
                .isEqualTo(AutoDrugReleaseOutcome.BLOCKED_PROBLEM_STATE);
        assertThat(commitFailedResult.getOutcome())
                .isEqualTo(AutoDrugReleaseOutcome.BLOCKED_PROBLEM_STATE);
        verify(redisTemplate, never()).execute(
                eq(script("RELEASE_SCRIPT")), anyList(), anyString(), anyString());
    }

    @Test
    void reconciliationCanExplicitlyReleaseAProblemReservation() throws Exception {
        RedisScript<String> releaseScript = script("RELEASE_SCRIPT");
        AutoDrugReservation commitFailed = reservation(
                "commit-failed-box", 26L, AutoDrugReservationState.COMMIT_FAILED);
        String json = objectMapper.writeValueAsString(commitFailed);
        when(hashOperations.get(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS, "commit-failed-box"))
                .thenReturn(json);
        when(redisTemplate.execute(
                eq(releaseScript), anyList(), eq("commit-failed-box"), eq(json)))
                .thenReturn("RELEASED:0");

        AutoDrugReleaseResult result = manager.resolveAndRelease(
                "commit-failed-box", "26号仓位盘点已确认");

        assertThat(result.getOutcome()).isEqualTo(AutoDrugReleaseOutcome.RELEASED);
        assertThat(result.getPendingCount()).isZero();
    }

    @Test
    void residualReservationIsAutomaticallyClearedWhenNewSessionStarts() {
        when(hashOperations.size(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS))
                .thenReturn(1L);
        AutoDrugReservationManager recreated =
                new AutoDrugReservationManager(redisTemplate, objectMapper);
        when(redisTemplate.execute(
                eq(script("BEGIN_SESSION_SCRIPT")), anyList(), eq("new-session")))
                .thenReturn("CLEARED_STALE_RESERVATIONS:1");

        assertThat(recreated.hasResidualReservations()).isTrue();
        AutoDrugSessionResult result = recreated.beginSession("new-session");
        assertThat(result.getOutcome())
                .isEqualTo(AutoDrugSessionOutcome.CLEARED_STALE_RESERVATIONS);
        assertThat(result.isUsable()).isTrue();
        assertThat(result.getResidualCount()).isEqualTo(1);
    }

    @Test
    void missingOrReplacedRedisSessionStopsCurrentRunButNextStartRecovers() {
        RedisScript<String> assertSessionScript = script("ASSERT_SESSION_SCRIPT");
        when(redisTemplate.execute(
                eq(assertSessionScript), anyList(), eq("session-1")))
                .thenReturn("SESSION_MISSING:0");
        when(redisTemplate.execute(
                eq(assertSessionScript), anyList(), eq("session-2")))
                .thenReturn("SESSION_MISMATCH:0");

        assertThatThrownBy(() -> manager.assertSession("session-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis可能重启或被清空");
        assertThatThrownBy(() -> manager.assertSession("session-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("会话不匹配");

        assertThat(manager.isSafetyInterlockTripped()).isTrue();
        assertThat(manager.getSafetyInterlockReason()).contains("会话Key已丢失");
        when(redisTemplate.execute(
                eq(script("BEGIN_SESSION_SCRIPT")), anyList(), eq("session-after-redis-loss")))
                .thenReturn("STARTED:0");

        AutoDrugSessionResult restarted = manager.beginSession("session-after-redis-loss");
        assertThat(restarted.isUsable()).isTrue();
        assertThat(manager.isSafetyInterlockTripped()).isFalse();
    }

    @Test
    void stoppingSessionReleasesAllPendingBoxes() {
        when(redisTemplate.execute(
                eq(script("FINISH_SESSION_SCRIPT")), anyList(), eq("session-1")))
                .thenReturn("FINISHED:2");

        AutoDrugSessionResult result = manager.finishSessionAndReleaseAll("session-1");

        assertThat(result.getOutcome())
                .isEqualTo(AutoDrugSessionOutcome.FINISHED);
        assertThat(result.getResidualCount()).isEqualTo(2);
    }

    @Test
    void inconsistentAggregateHashesStopSelectionInsteadOfGuessing() {
        when(hashOperations.entries(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_ITEMS))
                .thenReturn(Map.of());
        when(hashOperations.entries(RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_OWNERS))
                .thenReturn(Map.of());
        when(hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_PENDING))
                .thenReturn(Map.of("26", "1"));
        when(hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_CAPACITY))
                .thenReturn(Map.of());
        when(hashOperations.entries(
                RedisKeyConstant.JAVA_AUTO_DRUG_RESERVATION_MACHINE_IDENTITIES))
                .thenReturn(Map.of());

        assertThatThrownBy(manager::findAllMachineSnapshots)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis预留异常")
                .hasMessageContaining("pending");
    }

    @Test
    void luaScriptsUseAtomicPendingAndSessionGuardsWithoutTtl() {
        String reserve = scriptText("RESERVE_SCRIPT");
        String release = scriptText("RELEASE_SCRIPT");
        String beginSession = scriptText("BEGIN_SESSION_SCRIPT");
        String finishSession = scriptText("FINISH_SESSION_SCRIPT");

        assertThat(reserve)
                .contains("local activeSession = redis.call('GET', KEYS[6])")
                .contains("if dbCount + currentCount >= effectiveCapacity then")
                .contains("redis.call('HINCRBY', KEYS[4], machineId, 1)");
        assertThat(release)
                .contains("redis.call('HDEL', KEYS[1], reservationId)")
                .contains("redis.call('HINCRBY', KEYS[4], machineId, -1)")
                .contains("if newCount == 0 then");
        assertThat(beginSession)
                .contains("local residualCount = redis.call('HLEN', KEYS[2])")
                .contains("redis.call('DEL', KEYS[2], KEYS[3], KEYS[4], KEYS[5], KEYS[6])")
                .contains("return 'CLEARED_STALE_RESERVATIONS:'");
        assertThat(finishSession)
                .contains("redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4], KEYS[5], KEYS[6])")
                .contains("return 'FINISHED:'");

        assertThat(reserve.toLowerCase()).doesNotContain("expire", "pexpire");
        assertThat(release.toLowerCase()).doesNotContain("expire", "pexpire");
        assertThat(beginSession.toLowerCase()).doesNotContain("expire", "pexpire");
        assertThat(finishSession.toLowerCase()).doesNotContain("expire", "pexpire");
    }

    private AutoDrugReservationRequest request(String reservationId,
                                               int databaseCount,
                                               int maxCapacity) {
        return AutoDrugReservationRequest.builder()
                .reservationId(reservationId)
                .sessionId("session-1")
                .machineId(26L)
                .boxNo("A01")
                .productNo("P-001")
                .productName("测试疫苗")
                .batchNo("B-001")
                .expiredAt(new Date(1_800_000_000_000L))
                .supervisedCode(reservationId)
                .databaseCount(databaseCount)
                .maxCapacity(maxCapacity)
                .build();
    }

    private AutoDrugReservation reservation(String reservationId,
                                             Long machineId,
                                             AutoDrugReservationState state) {
        return AutoDrugReservation.builder()
                .reservationId(reservationId)
                .sessionId("session-1")
                .machineId(machineId)
                .boxNo("A01")
                .productNo("P-001")
                .productName("测试疫苗")
                .batchNo("B-001")
                .expiredAtEpochMillis(1_800_000_000_000L)
                .supervisedCode(reservationId)
                .databaseCountAtReserve(5)
                .maxCapacity(10)
                .state(state)
                .stateReason("测试状态")
                .createdAtEpochMillis(1L)
                .updatedAtEpochMillis(1L)
                .build();
    }

    @SuppressWarnings("unchecked")
    private RedisScript<String> script(String fieldName) {
        return (RedisScript<String>) ReflectionTestUtils.getField(
                AutoDrugReservationManager.class, fieldName);
    }

    private String scriptText(String fieldName) {
        return script(fieldName).getScriptAsString();
    }
}
