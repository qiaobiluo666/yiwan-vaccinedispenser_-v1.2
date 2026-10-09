package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.OutPutRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacMachineTest8Test {

    @Test
    @SuppressWarnings("unchecked")
    void oneCycleFollowsOutput12Output13AndX1Sequence() {
        VacMachineServiceImpl service = new VacMachineServiceImpl();
        CabinetAService cabinetAService = mock(CabinetAService.class);
        DispensingFunction dispensingFunction = mock(DispensingFunction.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        String x1Key = String.format(RedisKeyConstant.sensor.SENSOR_CABINET_A_NUM, 1);
        when(values.get(x1Key))
                .thenReturn(CabinetConstants.SensorStatus.RESET.code)
                .thenReturn(CabinetConstants.SensorStatus.NORMAL.code);
        ReflectionTestUtils.setField(service, "cabinetAService", cabinetAService);
        ReflectionTestUtils.setField(service, "dispensingFunction", dispensingFunction);
        ReflectionTestUtils.setField(service, "valueOperations", values);

        AtomicLong closeOutputAt = new AtomicLong();
        doAnswer(invocation -> {
            OutPutRequest request = invocation.getArgument(0);
            if (request.getMode() == SettingConstants.CABINET_A_LIFT_BLANK_CLOSE_NUM
                    && request.getCommand() == CabinetConstants.OutPutCommand.OUTPUT) {
                closeOutputAt.set(System.nanoTime());
            }
            return null;
        }).when(cabinetAService).outPut(any(OutPutRequest.class));

        long cycleStartedAt = System.nanoTime();
        service.runTest8Cycle(1);

        ArgumentCaptor<OutPutRequest> outputCaptor = ArgumentCaptor.forClass(OutPutRequest.class);
        verify(cabinetAService, times(4)).outPut(outputCaptor.capture());
        List<OutPutRequest> outputs = outputCaptor.getAllValues();
        assertOutput(outputs.get(0), SettingConstants.CABINET_A_LIFT_BLANK_OPEN_NUM,
                CabinetConstants.OutPutCommand.OUTPUT);
        assertOutput(outputs.get(1), SettingConstants.CABINET_A_LIFT_BLANK_OPEN_NUM,
                CabinetConstants.OutPutCommand.NOT_OUTPUT);
        assertOutput(outputs.get(2), SettingConstants.CABINET_A_LIFT_BLANK_CLOSE_NUM,
                CabinetConstants.OutPutCommand.OUTPUT);
        assertOutput(outputs.get(3), SettingConstants.CABINET_A_LIFT_BLANK_CLOSE_NUM,
                CabinetConstants.OutPutCommand.NOT_OUTPUT);

        verify(dispensingFunction, times(2))
                .intPut(CabinetConstants.InPutCommand.QUERY, 1);
        verify(values).set(x1Key, CabinetConstants.SensorStatus.RESET.code);
        assertThat(closeOutputAt.get() - cycleStartedAt)
                .as("12停止后应保持打开1秒，才允许13开始关闭")
                .isGreaterThanOrEqualTo(900_000_000L);
    }

    private static void assertOutput(OutPutRequest request,
                                     int outputNumber,
                                     CabinetConstants.OutPutCommand command) {
        assertThat(request.getWorkMode()).isEqualTo(CabinetConstants.Cabinet.CAB_A);
        assertThat(request.getCabinet()).isEqualTo(CabinetConstants.Cabinet.CAB_A);
        assertThat(request.getMode()).isEqualTo(outputNumber);
        assertThat(request.getCommand()).isEqualTo(command);
    }
}
