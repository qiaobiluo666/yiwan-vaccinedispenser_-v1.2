package com.yiwan.vaccinedispenser.system.plc.data;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlcStatusDispatcherBJudgeAlarmTest {

    private static final String STATE_KEY = "plc:alarm:B_JUDGE_ALARM";

    @Test
    void noDrugBoxAlarmIsLatchedForDistanceRequestAndAcknowledged() {
        AlarmFixture fixture = fixture();
        when(fixture.valueOperations.get(STATE_KEY)).thenReturn("0");

        fixture.dispatcher.dispatch(Collections.singletonList(register(1)));

        assertAlarmHandled(fixture, "1");
    }

    @Test
    void distanceSensorAlarmIsLatchedForDistanceRequestAndAcknowledged() {
        AlarmFixture fixture = fixture();
        when(fixture.valueOperations.get(STATE_KEY)).thenReturn("0");

        fixture.dispatcher.dispatch(Collections.singletonList(register(2)));

        assertAlarmHandled(fixture, "2");
    }

    private static void assertAlarmHandled(AlarmFixture fixture, String state) {
        verify(fixture.valueOperations).set(STATE_KEY, state);
        verify(fixture.valueOperations).set(RedisKeyConstant.bJudge.ALARM, state);
        verifyNoInteractions(fixture.exceptionService, fixture.websocketService);
        verify(fixture.plcSendService).sendBCabinetJudgeAlarmAck();
    }

    private static RegisterInfo register(int value) {
        RegisterInfo info = new RegisterInfo();
        info.setAddress(229);
        info.setRawValue(value);
        info.setActualValue(String.valueOf(value));
        info.setMeta(PlcRegisterTable.B_JUDGE_ALARM);
        return info;
    }

    private static AlarmFixture fixture() {
        PlcStatusDispatcher dispatcher = new PlcStatusDispatcher();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        ReflectionTestUtils.setField(dispatcher, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(dispatcher, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(dispatcher, "websocketService", websocketService);
        ReflectionTestUtils.setField(dispatcher, "plcSendService", plcSendService);
        return new AlarmFixture(dispatcher, valueOperations, exceptionService, websocketService, plcSendService);
    }

    private static final class AlarmFixture {
        private final PlcStatusDispatcher dispatcher;
        private final ValueOperations<String, String> valueOperations;
        private final VacMachineExceptionService exceptionService;
        private final WebsocketService websocketService;
        private final PlcSendService plcSendService;

        private AlarmFixture(PlcStatusDispatcher dispatcher,
                             ValueOperations<String, String> valueOperations,
                             VacMachineExceptionService exceptionService,
                             WebsocketService websocketService,
                             PlcSendService plcSendService) {
            this.dispatcher = dispatcher;
            this.valueOperations = valueOperations;
            this.exceptionService = exceptionService;
            this.websocketService = websocketService;
            this.plcSendService = plcSendService;
        }
    }
}
