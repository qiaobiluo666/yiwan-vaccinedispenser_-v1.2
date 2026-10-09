package com.yiwan.vaccinedispenser.system.netty.function;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CabinetAMsgX1SensorTest {

    @Test
    @SuppressWarnings("unchecked")
    void x1TriggeredResponseIsStoredForPolling() {
        CabinetAMsg cabinetAMsg = new CabinetAMsg();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        ReflectionTestUtils.setField(cabinetAMsg, "valueOperations", values);
        String[] response = new String[12];
        response[9] = "01";
        response[11] = "01";

        cabinetAMsg.receiveSensor(response);

        verify(values).set(
                String.format(RedisKeyConstant.sensor.SENSOR_CABINET_A_NUM, 1),
                CabinetConstants.SensorStatus.NORMAL.code);
    }
}
