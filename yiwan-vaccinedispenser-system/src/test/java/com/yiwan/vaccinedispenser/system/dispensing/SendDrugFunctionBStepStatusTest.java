package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetBStepRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendDrugFunctionBStepStatusTest {

    @Test
    @SuppressWarnings("unchecked")
    void positionModeClearsCompletionStateBeforeSendingCommand() throws Exception {
        SendDrugFunction function = new SendDrugFunction();
        CabinetBService cabinetBService = mock(CabinetBService.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(values.get(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS)).thenReturn("true");
        ReflectionTestUtils.setField(function, "cabinetBService", cabinetBService);
        ReflectionTestUtils.setField(function, "valueOperations", values);

        boolean completed = function.cabinetBStepPosition(
                CabinetConstants.CabinetBStepMode.ROTATE, 0);

        assertThat(completed).isTrue();
        InOrder ordered = inOrder(values, cabinetBService);
        ordered.verify(values).set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS, "false");
        ordered.verify(cabinetBService).step(any(CabinetBStepRequest.class));
    }
}
