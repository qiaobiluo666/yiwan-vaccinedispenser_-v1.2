package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugSessionOutcome;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugSessionResult;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugThreadManagerLifecycleTest {

    @Test
    void autoDrugStartUsesOriginalCameraRedisStatus() {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("相机Redis异常时不应提交自动上药任务");
        });
        fixture.redis.put(RedisKeyConstant.cameraStatus.ABOVE, "false");

        assertThatThrownBy(fixture.manager::sendDrug)
                .isInstanceOf(ServiceException.class)
                .hasMessage("上方扫码相机未连接，无法开启自动上药");
    }

    @Test
    void redisSessionFailureStopsLocalRunEvenWhenRedisFlagsCannotBeWritten() {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });
        Object run = fixture.installRun("session-redis-down");
        fixture.registerTask(run, "main");
        fixture.completeTask(run, "startup", "测试启动结束");

        doThrow(new IllegalStateException("assertSession redis down"))
                .when(fixture.reservationManager).assertSession("session-redis-down");
        doThrow(new IllegalStateException("redis write down"))
                .when(fixture.values).set(anyString(), anyString());

        ReflectionTestUtils.invokeMethod(
                fixture.manager, "runMainTask", run, fixture.configData, fixture.configSetting);

        assertThat(fixture.manager.getIsRunning()).isFalse();
        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNull();
        verify(fixture.exceptionService).sendException(anyInt(), eq(""),
                contains("自动上药主线程异常"));
        verify(fixture.function, atLeastOnce()).autoDrug(
                CabinetConstants.CabinetBApplyCommand.AUTO,
                CabinetConstants.CabinetBApplyMode.STOP,
                CabinetConstants.CabinetBApplyStatus.ZERO);
    }

    @Test
    void restartIsBlockedUntilMainAndDistanceTasksHaveExitedAfterStop() throws Exception {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });
        Object run = fixture.installRun("session-active-tasks");
        fixture.registerTask(run, "main");
        fixture.registerTask(run, "distance");
        fixture.completeTask(run, "startup", "测试启动结束");

        fixture.manager.stop();

        assertThat(fixture.manager.getIsRunning()).isFalse();
        assertThatThrownBy(fixture.manager::sendDrug)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("上一轮自动上药仍在收尾")
                .hasMessageContaining("活动任务=2");

        fixture.completeTask(run, "main", "测试主线程退出");
        fixture.completeTask(run, "distance", "测试测距线程退出");
        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNull();
    }

    @Test
    void handTaskKeepsOldSessionClosedUntilOriginalHandThreadFinishes() throws Exception {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });
        Object run = fixture.installRun("session-hand-running");
        fixture.registerTask(run, "main");
        fixture.registerTask(run, "distance");
        fixture.manager.onAHandFlowStarted("session-hand-running", "trace-001");
        fixture.completeTask(run, "startup", "测试启动结束");

        fixture.manager.stop();
        fixture.completeTask(run, "main", "测试主线程退出");
        fixture.completeTask(run, "distance", "测试测距线程退出");

        assertThatThrownBy(fixture.manager::sendDrug)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("活动任务=1");
        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isSameAs(run);

        fixture.manager.onAHandFlowFinished("session-hand-running", "trace-001");

        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNull();
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                fixture.manager, "assertNoRunStillClosing")).doesNotThrowAnyException();
    }

    @Test
    void bServoReturnTaskKeepsOldSessionClosedUntilOriginalThreadFinishes() throws Exception {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });
        Object run = fixture.installRun("session-b-servo-return");
        fixture.registerTask(run, "main");
        fixture.manager.onBServoReturnFlowStarted(
                "session-b-servo-return", "trace-b-return-001");
        fixture.completeTask(run, "startup", "测试启动结束");

        fixture.manager.stop();
        fixture.completeTask(run, "main", "测试主线程退出");

        assertThatThrownBy(fixture.manager::sendDrug)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("活动任务=1");
        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isSameAs(run);

        fixture.manager.onBServoReturnFlowFinished(
                "session-b-servo-return", "trace-b-return-001");

        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNull();
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                fixture.manager, "assertNoRunStillClosing")).doesNotThrowAnyException();
    }

    @Test
    void rejectedDistanceSubmissionCancelsWholeRunAndFinishesAfterMainTaskExits()
            throws Exception {
        AtomicInteger submissions = new AtomicInteger();
        AtomicReference<Runnable> acceptedMainTask = new AtomicReference<>();
        TaskExecutor executor = task -> {
            if (submissions.incrementAndGet() == 1) {
                acceptedMainTask.set(task);
                return;
            }
            throw new TaskRejectedException("测试线程池已满");
        };
        Fixture fixture = new Fixture(executor);

        fixture.manager.sendDrug();
        assertThat(fixture.manager.getIsRunning()).isTrue();
        assertThat(acceptedMainTask.get()).isNotNull();

        assertThatThrownBy(fixture.manager::goTable)
                .isInstanceOf(TaskRejectedException.class)
                .hasMessageContaining("测试线程池已满");

        assertThat(fixture.manager.getIsRunning()).isFalse();
        assertThat(fixture.redis.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START)).isEqualTo("false");
        assertThat(fixture.redis.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START)).isEqualTo("false");
        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNotNull();
        verify(fixture.function).servoTableReturnForStop(any(ConfigData.class), contains("异常退回"));
        verify(fixture.exceptionService).sendException(anyInt(), eq(""),
                contains("测距线程提交或启动失败"));
        verify(fixture.function, atLeastOnce()).autoDrug(
                CabinetConstants.CabinetBApplyCommand.AUTO,
                CabinetConstants.CabinetBApplyMode.STOP,
                CabinetConstants.CabinetBApplyStatus.ZERO);

        // 被线程池接受的主任务仍算活动任务；它看到取消后退出才能收尾会话。
        acceptedMainTask.get().run();

        assertThat(ReflectionTestUtils.getField(fixture.manager, "currentRun")).isNull();
        verify(fixture.reservationManager).finishSessionAndReleaseAll(anyString());
    }

    @Test
    void stopWithoutLocalRunStillClearsStaleTemporaryReservations() throws Exception {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });

        fixture.manager.stop();

        verify(fixture.reservationManager).clearAllTemporaryReservations(
                contains("兜底释放"));
    }

    @Test
    void oneOfTwoConcurrentStopsCannotClearTheOtherStopGuard() throws Exception {
        Fixture fixture = new Fixture(command -> {
            throw new AssertionError("本测试不应向线程池提交任务");
        });
        AtomicInteger mechanicalStops = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        doAnswer(invocation -> {
            int stopNumber = mechanicalStops.incrementAndGet();
            if (stopNumber == 1) {
                firstEntered.countDown();
                if (!releaseFirst.await(3, TimeUnit.SECONDS)) {
                    throw new AssertionError("第一个stop等待释放超时");
                }
            } else {
                secondEntered.countDown();
                if (!releaseSecond.await(3, TimeUnit.SECONDS)) {
                    throw new AssertionError("第二个stop等待释放超时");
                }
            }
            return null;
        }).when(fixture.function).servoTableReturnForStop(any(ConfigData.class), anyString());

        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread firstStop = stopThread(fixture.manager, firstFailure, "test-stop-1");
        Thread secondStop = stopThread(fixture.manager, secondFailure, "test-stop-2");

        try {
            firstStop.start();
            assertThat(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();
            secondStop.start();
            assertThat(secondEntered.await(2, TimeUnit.SECONDS)).isTrue();

            releaseFirst.countDown();
            firstStop.join(2000);
            assertThat(firstStop.isAlive()).isFalse();
            assertThat(firstFailure.get()).isNull();

            assertThatThrownBy(fixture.manager::sendDrug)
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("正在执行停止收尾");

            releaseSecond.countDown();
            secondStop.join(2000);
            assertThat(secondStop.isAlive()).isFalse();
            assertThat(secondFailure.get()).isNull();
            assertThat(ReflectionTestUtils.getField(
                    fixture.manager, "stopOperationsInProgress")).isEqualTo(0);
            assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                    fixture.manager, "assertNoRunStillClosing")).doesNotThrowAnyException();
        } finally {
            releaseFirst.countDown();
            releaseSecond.countDown();
            firstStop.join(2000);
            secondStop.join(2000);
        }
    }

    private static Thread stopThread(SendDrugThreadManager manager,
                                     AtomicReference<Throwable> failure,
                                     String name) {
        Thread thread = new Thread(() -> {
            try {
                manager.stop();
            } catch (Throwable error) {
                failure.set(error);
            }
        }, name);
        thread.setDaemon(true);
        return thread;
    }

    private static final class Fixture {
        private final Map<String, String> redis = new HashMap<>();
        private final SendDrugThreadManager manager;
        private final SendDrugFunction function = mock(SendDrugFunction.class);
        private final ConfigFunction configFunction = mock(ConfigFunction.class);
        private final WebsocketService websocketService = mock(WebsocketService.class);
        private final VacMachineExceptionService exceptionService =
                mock(VacMachineExceptionService.class);
        private final AutoDrugReservationManager reservationManager =
                mock(AutoDrugReservationManager.class);
        private final ValueOperations<String, String> values;
        private final ConfigData configData = new ConfigData();
        private final ConfigSetting configSetting = new ConfigSetting();

        @SuppressWarnings("unchecked")
        private Fixture(TaskExecutor executor) {
            manager = new SendDrugThreadManager(executor);
            values = mock(ValueOperations.class);

            redis.put(RedisKeyConstant.CABINET_B_SERVO_ERROR, "false");
            redis.put(RedisKeyConstant.controlStatus.CABINET_A, "true");
            redis.put(RedisKeyConstant.controlStatus.CABINET_B, "true");
            redis.put(RedisKeyConstant.cameraStatus.ABOVE, "true");
            redis.put(RedisKeyConstant.cameraStatus.SIDE, "true");
            redis.put(RedisKeyConstant.cameraStatus.BELOW, "true");
            when(values.get(anyString())).thenAnswer(invocation ->
                    redis.get(invocation.getArgument(0, String.class)));
            doAnswer(invocation -> {
                redis.put(invocation.getArgument(0, String.class),
                        invocation.getArgument(1, String.class));
                return null;
            }).when(values).set(anyString(), anyString());

            when(function.handHasDrug()).thenReturn(false);
            when(configFunction.getAutoDrugConfigData()).thenReturn(configData);
            when(configFunction.getSettingConfigData()).thenReturn(configSetting);
            when(reservationManager.beginSession(anyString())).thenReturn(
                    AutoDrugSessionResult.builder()
                            .outcome(AutoDrugSessionOutcome.STARTED)
                            .sessionId("test-session")
                            .build());
            when(reservationManager.finishSessionAndReleaseAll(anyString())).thenReturn(
                    AutoDrugSessionResult.builder()
                            .outcome(AutoDrugSessionOutcome.FINISHED)
                            .sessionId("test-session")
                            .build());

            ReflectionTestUtils.setField(manager, "sendDrugFunction", function);
            ReflectionTestUtils.setField(manager, "configFunction", configFunction);
            ReflectionTestUtils.setField(manager, "websocketService", websocketService);
            ReflectionTestUtils.setField(manager, "vacMachineExceptionService", exceptionService);
            ReflectionTestUtils.setField(manager, "autoDrugReservationManager", reservationManager);
            ReflectionTestUtils.setField(manager, "valueOperations", values);
        }

        private Object installRun(String sessionId) {
            return ReflectionTestUtils.invokeMethod(manager, "installNewRun", sessionId);
        }

        private void registerTask(Object run, String taskId) {
            ReflectionTestUtils.invokeMethod(manager, "registerRunTask", run, taskId, false);
        }

        private void completeTask(Object run, String taskId, String reason) {
            ReflectionTestUtils.invokeMethod(manager, "completeRunTask", run, taskId, reason);
        }
    }
}
