package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugFunctionSensorCommitBoundaryTest {

    @Test
    void handSensorResetReturnsImmediatelyWithoutHoming() throws Exception {
        SendDrugFunction function = spy(new SendDrugFunction());
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ReflectionTestUtils.setField(function, "valueOperations", valueOperations);

        when(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_IS_RETURN))
                .thenReturn("true");
        when(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))
                .thenReturn("true");
        when(valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR))
                .thenReturn(CabinetConstants.SensorStatus.RESET.code);

        doNothing().when(function).cabinetAStepPosition(any(), anyInt());
        doNothing().when(function).waitCabinetAStepEnd(anyInt());
        doNothing().when(function).moveHandServo(anyInt(), anyInt());
        doNothing().when(function).intPut(any(), any(), anyInt());

        DrugRecordRequest request = new DrugRecordRequest();
        request.setMachineId(26L);
        request.setMachineNo("A01");
        request.setAutoX(1000);
        request.setAutoZ(2000);

        assertThat(function.dropDrugHandleAuto(100, 200, request)).isTrue();

        verify(valueOperations).get(RedisKeyConstant.sensor.HAND_SENSOR);
        verify(function, never()).cabinetAStepInit(any());
        verify(function, never()).moveHandServoInit(any());
    }
}
