package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugSessionResult;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/15 9:32
 */
@Slf4j
@Component
public class SendDrugThreadManager {


    @Autowired
    private SendDrugFunction  sendDrugFunction;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private AutoDrugReservationManager autoDrugReservationManager;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    private final TaskExecutor taskExecutor;
    @Autowired
    public SendDrugThreadManager(@Qualifier("DispensingThreadPool") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    private static final String STARTUP_TASK = "startup";
    private static final String MAIN_TASK = "main";
    private static final String DISTANCE_TASK = "distance";
    private static final String HAND_TASK_PREFIX = "hand:";
    private static final String B_SERVO_RETURN_TASK_PREFIX = "b-servo-return:";

    /**
     * 一轮自动上药只能由自己的RunContext驱动。不能再用一个可被下一轮重新置true的
     * 全局boolean作为旧线程的循环条件，否则停止后快速重启会让旧线程“复活”。
     */
    private final Object runLifecycleMonitor = new Object();
    private volatile boolean running = false;
    private volatile String currentSessionId;
    private volatile AutoDrugRunContext currentRun;
    /**
     * stop() 可能被前端、主线程和A柜机械手子线程同时调用。
     * 用计数而不是布尔值，避免先结束的stop把另一个仍在收尾的stop误标为已完成。
     */
    private int stopOperationsInProgress;

    public void goTable(){
        AutoDrugRunContext run = requireCurrentRunForTask(DISTANCE_TASK);
        try {
            String isNotError = valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR);
            if ("true".equals(isNotError)) {
                throw new ServiceException("B柜伺服报警，测距线程无法启动");
            }
            valueOperations.set(RedisKeyConstant.CABINET_B_ERROR_COUNT,"0");
            taskExecutor.execute(() -> runDistanceTask(run));
        } catch (Throwable dispatchError) {
            failRun(run,
                    "自动上药测距线程提交或启动失败，已停止设备，sessionId=" + run.sessionId,
                    dispatchError, null, true);
            completeRunTask(run, DISTANCE_TASK, "测距线程未能启动");
            if (dispatchError instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceException("自动上药测距线程启动失败", dispatchError);
        }
    }

    private void runDistanceTask(AutoDrugRunContext run) {
        ConfigData configData = null;
        try {
            configData = configFunction.getAutoDrugConfigData();
            while (isRunActive(run)) {
                try {
                    assertRequiredDevicesConnectedDuringRun();
                    // 测距线程也必须绑定同一Redis会话，Redis被清空后不能独自继续驱动设备。
                    autoDrugReservationManager.assertSession(run.sessionId);
                    sendDrugFunction.goTable(configData);
                } catch (Throwable taskError) {
                    failRun(run,
                            "自动上药测距线程异常，已失败关闭，sessionId=" + run.sessionId
                                    + "，原因=" + Objects.toString(
                                    taskError.getMessage(), taskError.getClass().getSimpleName()),
                            taskError, configData, true);
                    break;
                }
                VacUntil.sleep(500);
            }
        } catch (Throwable startupError) {
            failRun(run,
                    "自动上药测距线程初始化异常，已失败关闭，sessionId=" + run.sessionId,
                    startupError, configData, true);
        } finally {
            completeRunTask(run, DISTANCE_TASK, "自动上药测距线程退出");
        }
    }

    //开始自动上药
    public synchronized void sendDrug() throws IOException {
        assertNoRunStillClosing();
        if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
            throw new ServiceException("正在自动对仓！");
        }


        if("true".equals(valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START))){
            throw new ServiceException("正在库存盘点！");
        }

        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            throw new ServiceException("正在自动上药");
        }

        validateAutoDrugStart();
        validateHandEmptyBeforeInit();
        String sessionId = UUID.randomUUID().toString();
        AutoDrugRunContext run = installNewRun(sessionId);
        ConfigData configData = null;
        boolean sessionEstablished = false;
        boolean hardwareInitialized = false;
        boolean failureAlreadyReported = false;
        boolean mainTaskRegistered = false;
        boolean mainTaskDispatched = false;
        try {
            AutoDrugSessionResult sessionResult;
            try {
                sessionResult = autoDrugReservationManager.beginSession(sessionId);
            } catch (RuntimeException sessionError) {
                String message = "自动上药Redis保护检查失败，禁止启动："
                        + Objects.toString(sessionError.getMessage(), "Redis状态未知");
                failureAlreadyReported = true;
                notifyAutoDrugProtection(message, sessionError);
                throw new ServiceException(message, sessionError);
            }
            run.sessionEstablished.set(true);
            sessionEstablished = true;

            log.info("开始自动上药，sessionId={}，Redis会话结果={}",
                    sessionId, sessionResult.getOutcome());
            configData = configFunction.getAutoDrugConfigData();
            ConfigSetting configSetting = configFunction.getSettingConfigData();
            init(configData);
            hardwareInitialized = true;

            // stop()可能在初始化期间到达。初始化结束后必须重新确认本会话仍有效，
            // 不能把stop刚写入的false再次覆盖并启动异步线程。
            if (!isRunActive(run)) {
                throw new ServiceException("自动上药初始化期间已收到停止指令，本次启动已取消");
            }

            //开始发药状态
            valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"true");
            registerRunTask(run, MAIN_TASK, false);
            mainTaskRegistered = true;
            final ConfigData taskConfigData = configData;
            taskExecutor.execute(() -> runMainTask(run, taskConfigData, configSetting));
            mainTaskDispatched = true;
        } catch (Throwable startError) {
            if (mainTaskRegistered && !mainTaskDispatched) {
                completeRunTask(run, MAIN_TASK, "自动上药主线程未能提交");
            }
            cancelRunLocally(run, "自动上药启动失败");
            safeSetValue(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false",
                    "启动失败复位AUTO_DRUG_START");
            safeSetValue(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START, "false",
                    "启动失败复位AUTO_DRUG_BELT_START");
            if (!failureAlreadyReported) {
                String message = "自动上药启动失败，已停止设备，sessionId=" + sessionId
                        + "，原因=" + Objects.toString(
                        startError.getMessage(), startError.getClass().getSimpleName());
                notifyAutoDrugProtection(message, startError);
            }
            if (hardwareInitialized) {
                safeServoTableReturn(configData, "自动上药启动失败退回");
                safeStopAutoDrugCommand("自动上药启动失败");
            }
            if (startError instanceof IOException ioException) {
                throw ioException;
            }
            if (startError instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceException("自动上药启动失败：" + startError.getMessage(), startError);
        } finally {
            // startup本身也计入活动任务，防止stop在init期间把会话提前收尾并允许新一轮启动。
            completeRunTask(run, STARTUP_TASK,
                    sessionEstablished ? "自动上药启动入口结束" : "自动上药会话未建立");
        }

    }

    private void runMainTask(AutoDrugRunContext run,
                             ConfigData configData,
                             ConfigSetting configSetting) {
        try {
            while (isRunActive(run)) {
                try {
                    assertRequiredDevicesConnectedDuringRun();
                    // Redis重启、flush或会话被替换时，在任何新一轮机械动作前失败关闭。
                    autoDrugReservationManager.assertSession(run.sessionId);
                    sendDrugFunction.sendDrug(configData, configSetting, run.sessionId);
                } catch (Throwable taskError) {
                    failRun(run,
                            "自动上药主线程异常，已失败关闭，sessionId=" + run.sessionId
                                    + "，原因=" + Objects.toString(
                                    taskError.getMessage(), taskError.getClass().getSimpleName()),
                            taskError, configData, true);
                    break;
                }
                VacUntil.sleep(500);
            }
        } finally {
            safeStopAutoDrugCommand("自动上药主线程退出");
            completeRunTask(run, MAIN_TASK, "自动上药主线程退出");
        }
    }


    public void  init(ConfigData configData) throws IOException {
        log.info("========================自动上药初始化=========================");
        //线程机械手运动结束
        valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");
        valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
        //A柜步进电机回原
        sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);

        //等待夹爪步进电机运动完成
        sendDrugFunction.waitCabinetAStepEnd(1);
        sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
        sendDrugFunction.waitCabinetAStepEnd(2);

        //A柜伺服 回原
        sendDrugFunction.moveHandServoInit(configData);

        //B柜步进电机回原
        sendDrugFunction.cabinetBStepInit();
        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            //B柜伺服回原到初始上方扫码位置
            sendDrugFunction.cabinetBServoInit();
        }


        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");
        //判断机器是否正在发药
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"true");
        //开始上药 设置皮带没有开启
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
        //设置滑套传感器没触发
        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
        //发药流程是否结束
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH,"false");
        //是否可以开始扫码
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_SCAN,"false");
        //是否可以开始检测有多个药
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"false");
        //检测结果
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"false");
        //
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_RUN,"true");

        //伺服报警清楚
        valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_ERROR,"false");
    }


    public void stop() throws IOException {
        stop(false);
    }

    /**
     * 自动上药初始化前检查机械手。这里只读取传感器，不执行任何回原动作。
     * 有苗时直接报警并终止启动，避免初始化动作夹伤或带走遗留药盒。
     */
    public void validateHandEmptyBeforeInit() {
        if (!sendDrugFunction.handHasDrug()) {
            log.info("自动上药初始化检查通过：机械手上无苗");
            return;
        }

        String errorMsg = "初始化异常：机械手上有苗";
        log.error(errorMsg);

        // 启动尚未完成，不调用 stop()，防止 stop() 内部的回退动作带来额外机械运动。
        running = false;
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"false");
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
        // 与运行中自动上药保护使用同一报警出口：写入异常管理并实时推送前端。
        notifyAutoDrugProtection(errorMsg, null);
        throw new ServiceException(errorMsg);
    }

    /**
     * 扫码保护触发时当前药盒已经完成回退，停止流程时不再重复执行滑台回退。
     */
    public void stopAfterScanReturn() throws IOException {
        stop(true);
    }

    private void stop(boolean tableAlreadyReturned) throws IOException {
        AutoDrugRunContext run;
        synchronized (runLifecycleMonitor) {
            stopOperationsInProgress++;
            run = currentRun;
        }
        Throwable firstStopError = null;
        try {
            // 任何配置、Redis或网络读取之前先完成本地失败关闭。这一行不能后移。
            cancelRunLocally(run, "收到停止自动上药指令");

            boolean autoFlagCleared = safeSetValue(
                    RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false", "停止自动上药");
            boolean beltFlagCleared = safeSetValue(
                    RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START, "false", "停止自动上药皮带");
            if ((!autoFlagCleared || !beltFlagCleared) && run != null) {
                try {
                    autoDrugReservationManager.tripSafetyInterlock(
                            "停止自动上药时Redis不可用；已先停止本地线程，下次启动时将再次自动清理临时预增", null);
                } catch (Throwable interlockError) {
                    firstStopError = interlockError;
                    log.error("停止自动上药时无法写入Redis安全闭锁", interlockError);
                }
            }

            log.info("===============停止自动上药，sessionId={}=================",
                    run == null ? null : run.sessionId);
            ConfigData configData = null;
            try {
                configData = configFunction.getAutoDrugConfigData();
            } catch (Throwable configError) {
                if (firstStopError == null) {
                    firstStopError = configError;
                }
                log.error("停止自动上药时读取配置失败", configError);
            }

            String servoError = null;
            try {
                servoError = valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR);
            } catch (Throwable redisError) {
                if (firstStopError == null) {
                    firstStopError = redisError;
                }
                log.error("停止自动上药时无法读取B柜伺服状态，按未知状态关闭气泵", redisError);
            }

            try {
                if ("true".equals(servoError) || servoError == null || configData == null) {
                    // 伺服状态未知时不盲目驱动滑台，只关闭气泵并发送总停止命令。
                    sendDrugFunction.xiPangEnd();
                } else if (!tableAlreadyReturned) {
                    sendDrugFunction.servoTableReturnForStop(configData,"停止上药");
                }
            } catch (Throwable mechanicalError) {
                if (firstStopError == null) {
                    firstStopError = mechanicalError;
                }
                log.error("停止自动上药机械收尾失败", mechanicalError);
            }

            safeStopAutoDrugCommand("收到停止自动上药指令");
            safeNotifyFrontendFinished();
            if (run != null) {
                tryFinishRunIfIdle(run, "收到停止自动上药指令");
            } else {
                // 服务异常退出后本地会话已经不存在，停止按钮仍要能一键清掉旧的临时预增。
                try {
                    autoDrugReservationManager.clearAllTemporaryReservations(
                            "停止自动上药时未发现本地活动会话，执行兜底释放");
                } catch (Throwable clearError) {
                    if (firstStopError == null) {
                        firstStopError = clearError;
                    }
                    log.error("停止自动上药兜底释放临时预增失败", clearError);
                }
            }

            if (firstStopError != null) {
                notifyAutoDrugProtection("停止自动上药时部分设备收尾失败；本地线程已停止，原因="
                        + Objects.toString(firstStopError.getMessage(),
                        firstStopError.getClass().getSimpleName()), firstStopError);
                if (firstStopError instanceof IOException ioException) {
                    throw ioException;
                }
                if (firstStopError instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IOException("停止自动上药设备收尾失败", firstStopError);
            }
        } finally {
            synchronized (runLifecycleMonitor) {
                stopOperationsInProgress--;
                if (stopOperationsInProgress < 0) {
                    // 理论上不应发生；防止计数异常造成永久误锁。
                    log.error("自动上药stop收尾计数异常，已强制归零");
                    stopOperationsInProgress = 0;
                }
                runLifecycleMonitor.notifyAll();
            }
        }
    }


    //查看设备是否正常 能开始上药
    public Boolean status(){
        return "true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_B)) &&
                "true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_A)) &&
                "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.ABOVE)) &&
                "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.SIDE)) &&
                "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.BELOW));
    }

    /**
     * 自动上药启动前校验真实设备状态，并返回可直接展示给前端的具体原因。
     */
    private void validateAutoDrugStart() {
        if ("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR))) {
            throw new ServiceException("B柜伺服报警未清除，无法开启自动上药");
        }

        if (!"true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_B))) {
            throw new ServiceException("B柜未连接，无法开启自动上药");
        }

        if (!"true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_A))) {
            throw new ServiceException("A柜未连接，无法开启自动上药");
        }

        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.ABOVE))) {
            throw new ServiceException("上方扫码相机未连接，无法开启自动上药");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.SIDE))) {
            throw new ServiceException("侧方扫码相机未连接，无法开启自动上药");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.BELOW))) {
            throw new ServiceException("下方扫码相机未连接，无法开启自动上药");
        }
    }

    /** 运行过程中任一自动上药必需设备掉线，都必须失败关闭，不能原地循环等待。 */
    private void assertRequiredDevicesConnectedDuringRun() {
        if ("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR))) {
            throw new ServiceException("B柜伺服报警，自动上药已停止");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_B))) {
            throw new ServiceException("B柜控制板连接断开，自动上药已停止");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_A))) {
            throw new ServiceException("A柜控制板连接断开，自动上药已停止");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.ABOVE))) {
            throw new ServiceException("上方扫码相机连接断开，自动上药已停止");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.SIDE))) {
            throw new ServiceException("侧方扫码相机连接断开，自动上药已停止");
        }
        if (!"true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.BELOW))) {
            throw new ServiceException("下方扫码相机连接断开，自动上药已停止");
        }
    }

    /** Netty实时掉线回调：先停止本地会话，再写异常管理并通知前端。 */
    public void stopForRequiredDeviceDisconnect(String deviceName) {
        AutoDrugRunContext run;
        synchronized (runLifecycleMonitor) {
            run = currentRun;
            if (run == null || run.cancelled.get()) {
                return;
            }
        }
        String message = deviceName + "连接断开，自动上药已停止";
        failRun(run, message, null, null, false);
    }

    private void notifyAutoDrugProtection(String message, Throwable cause) {
        if (cause == null) {
            log.error(message);
        } else {
            log.error(message, cause);
        }
        try {
            vacMachineExceptionService.sendException(
                    SettingConstants.MachineException.SEND.code, "", message);
        } catch (Throwable exceptionError) {
            log.error("自动上药保护报警写入异常管理失败", exceptionError);
        }
        try {
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.MACHINE_STATUS_COMMAND.getCode());
            commandData.put("data", message);
            websocketService.sendInfo(CommandEnums.ON_CABINET_WEB.getCode(), commandData);
        } catch (Throwable websocketError) {
            log.error("自动上药保护报警推送前端失败", websocketError);
        }
    }

    /**
     * 主流程释放/保留预留后通知生命周期管理器重新检查。此回调不减活动线程计数，
     * 因为进入A柜前的主流程并没有创建A柜子线程。
     */
    public void onReservationFlowFinished(String sessionId) {
        AutoDrugRunContext run = currentRun;
        if (run != null && Objects.equals(run.sessionId, sessionId)) {
            tryFinishRunIfIdle(run, "逐盒预留状态已更新");
        }
    }

    /** A柜原始送仓子线程启动前登记，防止stop后在它退出前开启新会话。 */
    public void onAHandFlowStarted(String sessionId, String traceId) {
        AutoDrugRunContext run = requireRunBySession(sessionId);
        registerRunTask(run, handTaskId(traceId), true);
    }

    /** A柜原始送仓子线程唯一的finally出口。 */
    public void onAHandFlowFinished(String sessionId, String traceId) {
        AutoDrugRunContext run = currentRun;
        if (run == null || !Objects.equals(run.sessionId, sessionId)) {
            log.warn("A柜送仓线程结束时会话已不匹配：sessionId={}, traceId={}", sessionId, traceId);
            return;
        }
        completeRunTask(run, handTaskId(traceId), "A柜送仓线程退出");
    }

    /** B柜伺服回扫码位线程启动前登记，避免旧会话的机械动作与新会话串线。 */
    public void onBServoReturnFlowStarted(String sessionId, String traceId) {
        AutoDrugRunContext run = requireRunBySession(sessionId);
        // 药盒已从B柜释放时，即使刚收到stop也必须跟踪这个在途收尾任务。
        registerRunTask(run, bServoReturnTaskId(traceId), true);
    }

    /** B柜伺服回扫码位线程的唯一finally出口。 */
    public void onBServoReturnFlowFinished(String sessionId, String traceId) {
        AutoDrugRunContext run = currentRun;
        if (run == null || !Objects.equals(run.sessionId, sessionId)) {
            log.warn("B柜伺服回扫码位线程结束时会话已不匹配：sessionId={}, traceId={}",
                    sessionId, traceId);
            return;
        }
        completeRunTask(run, bServoReturnTaskId(traceId), "B柜伺服回扫码位线程退出");
    }

    private void assertNoRunStillClosing() {
        synchronized (runLifecycleMonitor) {
            if (stopOperationsInProgress == 0 && currentRun == null) {
                return;
            }
            if (stopOperationsInProgress > 0) {
                throw new ServiceException("自动上药正在执行停止收尾，禁止重新启动");
            }
            throw new ServiceException(String.format(
                    "上一轮自动上药仍在收尾，禁止重新启动：sessionId=%s，活动任务=%d，已取消=%s",
                    currentRun.sessionId, currentRun.activeTaskIds.size(),
                    currentRun.cancelled.get()));
        }
    }

    private AutoDrugRunContext installNewRun(String sessionId) {
        synchronized (runLifecycleMonitor) {
            if (stopOperationsInProgress > 0) {
                throw new ServiceException("自动上药正在执行停止收尾，禁止重新启动");
            }
            if (currentRun != null) {
                throw new ServiceException("上一轮自动上药仍在收尾，禁止重新启动");
            }
            AutoDrugRunContext run = new AutoDrugRunContext(sessionId);
            run.activeTaskIds.add(STARTUP_TASK);
            currentRun = run;
            currentSessionId = sessionId;
            running = true;
            log.info("自动上药本地会话已占位：sessionId={}, task={}", sessionId, STARTUP_TASK);
            return run;
        }
    }

    private AutoDrugRunContext requireCurrentRunForTask(String taskId) {
        AutoDrugRunContext run;
        synchronized (runLifecycleMonitor) {
            run = currentRun;
            if (run == null || run.cancelled.get()) {
                throw new ServiceException("自动上药会话不存在或正在停止，无法启动任务：" + taskId);
            }
        }
        registerRunTask(run, taskId, false);
        return run;
    }

    private AutoDrugRunContext requireRunBySession(String sessionId) {
        synchronized (runLifecycleMonitor) {
            AutoDrugRunContext run = currentRun;
            if (run == null || !Objects.equals(run.sessionId, sessionId)) {
                throw new IllegalStateException("自动上药子线程所属会话已失效：" + sessionId);
            }
            return run;
        }
    }

    private void registerRunTask(AutoDrugRunContext run, String taskId, boolean allowCancelled) {
        synchronized (runLifecycleMonitor) {
            if (currentRun != run) {
                throw new IllegalStateException("自动上药任务所属会话已失效：" + taskId);
            }
            if (!allowCancelled && run.cancelled.get()) {
                throw new IllegalStateException("自动上药会话正在停止，拒绝启动任务：" + taskId);
            }
            if (!run.activeTaskIds.add(taskId)) {
                throw new IllegalStateException("自动上药任务重复登记：" + taskId);
            }
            log.info("自动上药任务登记：sessionId={}, task={}, activeTasks={}",
                    run.sessionId, taskId, run.activeTaskIds.size());
        }
    }

    private void completeRunTask(AutoDrugRunContext run, String taskId, String reason) {
        boolean removed;
        int remaining;
        synchronized (runLifecycleMonitor) {
            removed = run.activeTaskIds.remove(taskId);
            remaining = run.activeTaskIds.size();
        }
        if (!removed) {
            log.error("自动上药任务重复结束或未登记：sessionId={}, task={}, reason={}",
                    run.sessionId, taskId, reason);
            return;
        }
        log.info("自动上药任务结束：sessionId={}, task={}, remaining={}, reason={}",
                run.sessionId, taskId, remaining, reason);
        tryFinishRunIfIdle(run, reason);
    }

    private boolean cancelRunLocally(AutoDrugRunContext run, String reason) {
        // 即使Redis已断开，也必须首先让Java线程看到停止状态。
        if (run == null) {
            synchronized (runLifecycleMonitor) {
                if (currentRun == null) {
                    running = false;
                }
            }
            return false;
        }
        boolean firstCancellation = run.cancelled.compareAndSet(false, true);
        synchronized (runLifecycleMonitor) {
            // 旧会话的迟到异常绝不能修改新会话的兼容running状态。
            if (currentRun == run) {
                running = false;
            }
        }
        if (firstCancellation) {
            log.warn("自动上药本地会话已取消：sessionId={}, reason={}", run.sessionId, reason);
        }
        return firstCancellation;
    }

    private boolean isRunActive(AutoDrugRunContext run) {
        return currentRun == run && !run.cancelled.get();
    }

    private void failRun(AutoDrugRunContext run,
                         String message,
                         Throwable cause,
                         ConfigData configData,
                         boolean returnTable) {
        // 不读取Redis判断是否已经停止；本地CAS是失败关闭的唯一判定。
        boolean firstFailure = cancelRunLocally(run, message);
        if (!firstFailure) {
            log.warn("自动上药会话已在停止，忽略重复失败关闭：sessionId={}, message={}",
                    run.sessionId, message);
            return;
        }
        safeSetValue(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false", "异常关闭自动上药");
        safeSetValue(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START, "false", "异常关闭上药皮带");
        notifyAutoDrugProtection(message, cause);
        if (returnTable) {
            safeServoTableReturn(configData, "自动上药异常退回");
        }
        safeStopAutoDrugCommand("自动上药异常关闭");
        safeNotifyFrontendFinished();
        tryFinishRunIfIdle(run, "自动上药异常关闭");
    }

    private boolean safeSetValue(String key, String value, String reason) {
        try {
            valueOperations.set(key, value);
            return true;
        } catch (Throwable redisError) {
            log.error("{}时写Redis失败：key={}, value={}", reason, key, value, redisError);
            return false;
        }
    }

    private void safeServoTableReturn(ConfigData configData, String reason) {
        try {
            ConfigData actualConfig = configData == null
                    ? configFunction.getAutoDrugConfigData() : configData;
            sendDrugFunction.servoTableReturnForStop(actualConfig, reason);
        } catch (Throwable returnError) {
            log.error("自动上药失败关闭时滑台退回失败：reason={}", reason, returnError);
        }
    }

    private void safeStopAutoDrugCommand(String reason) {
        try {
            sendDrugFunction.autoDrug(
                    CabinetConstants.CabinetBApplyCommand.AUTO,
                    CabinetConstants.CabinetBApplyMode.STOP,
                    CabinetConstants.CabinetBApplyStatus.ZERO);
        } catch (Throwable stopError) {
            log.error("发送自动上药停止命令失败：reason={}", reason, stopError);
        }
    }

    private void safeNotifyFrontendFinished() {
        try {
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DOSAGE_BUTTON_FINISH.getCode());
            commandData.put("data", "end");
            websocketService.sendInfo(CommandEnums.ON_CABINET_WEB.getCode(),commandData);
        } catch (Throwable websocketError) {
            log.error("停止自动上药后通知前端失败", websocketError);
        }
    }

    private void tryFinishRunIfIdle(AutoDrugRunContext run, String reason) {
        synchronized (runLifecycleMonitor) {
            if (currentRun != run || !run.cancelled.get()
                    || !run.activeTaskIds.isEmpty() || run.finalizing) {
                return;
            }
            run.finalizing = true;
        }

        try {
            if (run.sessionEstablished.get()) {
                AutoDrugSessionResult result =
                        autoDrugReservationManager.finishSessionAndReleaseAll(run.sessionId);
                log.info("停止自动上药并释放本轮全部临时预增：sessionId={}, reason={}, outcome={}, releasedCount={}",
                        run.sessionId, reason, result.getOutcome(), result.getResidualCount());
            } else {
                log.info("自动上药Redis会话尚未建立，仅清理本地启动占位：sessionId={}, reason={}",
                        run.sessionId, reason);
            }
        } catch (Throwable sessionError) {
            log.error("自动上药Redis会话收尾失败：sessionId={}, reason={}",
                    run.sessionId, reason, sessionError);
            notifyAutoDrugProtection(
                    "停止自动上药时临时预增释放失败；下次启动将再次自动清理，sessionId=" + run.sessionId,
                    sessionError);
        } finally {
            synchronized (runLifecycleMonitor) {
                run.finalizing = false;
                if (currentRun == run && run.cancelled.get() && run.activeTaskIds.isEmpty()) {
                    currentRun = null;
                    currentSessionId = null;
                    running = false;
                    log.info("自动上药本地会话已完全退出：sessionId={}, reason={}",
                            run.sessionId, reason);
                }
            }
        }
    }

    private String handTaskId(String traceId) {
        return flowTaskId(HAND_TASK_PREFIX, traceId, "A柜送仓线程");
    }

    private String bServoReturnTaskId(String traceId) {
        return flowTaskId(B_SERVO_RETURN_TASK_PREFIX, traceId, "B柜伺服回扫码位线程");
    }

    private String flowTaskId(String prefix, String traceId, String taskName) {
        if (traceId == null || traceId.trim().isEmpty()) {
            throw new IllegalArgumentException(taskName + "traceId不能为空");
        }
        return prefix + traceId.trim();
    }

    private static final class AutoDrugRunContext {
        private final String sessionId;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean sessionEstablished = new AtomicBoolean(false);
        private final Set<String> activeTaskIds = new HashSet<>();
        private boolean finalizing;

        private AutoDrugRunContext(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    //获取running状态
    public boolean getIsRunning(){
        return running;
    }

}
