package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugSessionOutcome;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugSessionResult;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugThreadManagerReservationGuardTest {

    @Test
    @SuppressWarnings("unchecked")
    void staleReservationIsAutomaticallyReleasedAndDoesNotBlockStart() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        SendDrugThreadManager manager = new SendDrugThreadManager(executor);
        SendDrugFunction function = mock(SendDrugFunction.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        AutoDrugReservationManager reservationManager = mock(AutoDrugReservationManager.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        Map<String, String> redis = new HashMap<>();

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
        when(configFunction.getAutoDrugConfigData()).thenReturn(new ConfigData());
        when(configFunction.getSettingConfigData()).thenReturn(new ConfigSetting());
        when(reservationManager.beginSession(anyString())).thenReturn(
                AutoDrugSessionResult.builder()
                        .outcome(AutoDrugSessionOutcome.CLEARED_STALE_RESERVATIONS)
                        .residualCount(1)
                        .message("已自动释放上一轮临时预增")
                        .build());

        ReflectionTestUtils.setField(manager, "sendDrugFunction", function);
        ReflectionTestUtils.setField(manager, "configFunction", configFunction);
        ReflectionTestUtils.setField(manager, "autoDrugReservationManager", reservationManager);
        ReflectionTestUtils.setField(manager, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(manager, "valueOperations", values);

        manager.sendDrug();

        assertThat(manager.getIsRunning()).isTrue();
        assertThat(redis.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START)).isEqualTo("true");
        verify(reservationManager).beginSession(anyString());
        verify(executor).execute(any(Runnable.class));
        verify(exceptionService, never()).sendException(
                eq(SettingConstants.MachineException.SEND.code), eq(""),
                contains("残留预留"));
    }
}
