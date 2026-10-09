package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.plc.data.AlarmBitTable;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DispensingControllerAutoDrugAlarmTest {

    @Test
    @SuppressWarnings("unchecked")
    void plcStartOnlyClearsTemporaryDataAndSendsStartCommand() throws Exception {
        DispensingController controller = new DispensingController();
        SendDrugThreadManager threadManager = mock(SendDrugThreadManager.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);

        ReflectionTestUtils.setField(controller, "sendDrugThreadManager", threadManager);
        ReflectionTestUtils.setField(controller, "plcSendService", plcSendService);
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);

        controller.autoDrugStart();

        var ordered = inOrder(redisTemplate, plcSendService);
        ordered.verify(redisTemplate).delete(RedisKeyConstant.PLC_SEND_DRUG_MSG);
        ordered.verify(redisTemplate).delete(RedisKeyConstant.PLC_RESERVED_MACHINES);
        ordered.verify(redisTemplate).delete(
                "plc:alarm:" + AlarmBitTable.B_ALARM1_START_SIGNAL.name());
        ordered.verify(plcSendService).sendBCabinetFeedStart();
        verifyNoInteractions(threadManager);
    }
}
