package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NettyClientBCabinetDisconnectAlarmTest {

    private static final String B_CABINET_DISCONNECT_MESSAGE =
            "自动上药异常：B柜控制板未连接或连接断开，无法继续自动上药";

    @Test
    @SuppressWarnings("unchecked")
    void repeatedDisconnectCallbacksCreateOnlyOneAutoDrugAlarm() {
        NettyClient client = new NettyClient(
                "172.16.85.231", 8088, "B",
                mock(NettyReceiveCabinetService.class));
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);

        ReflectionTestUtils.setField(client, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(client, "valueOperations", values);

        client.onDisconnected();
        client.onDisconnected();

        verify(exceptionService, times(1)).sendException(
                SettingConstants.MachineException.SEND.code,
                "",
                B_CABINET_DISCONNECT_MESSAGE);
        verify(values, times(2)).set(RedisKeyConstant.controlStatus.CABINET_B, "false");
    }

    @Test
    void bCabinetReconnectClearsOnlyItsAutoDrugDisconnectAlarm() {
        NettyClient client = new NettyClient(
                "172.16.85.231", 8088, "B",
                mock(NettyReceiveCabinetService.class));
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        ReflectionTestUtils.setField(client, "vacMachineExceptionService", exceptionService);

        client.clearBCabinetAutoDrugDisconnectAlarm();

        verify(exceptionService, times(1)).delExceptionByCodeAndDesc(
                SettingConstants.MachineException.SEND.code,
                B_CABINET_DISCONNECT_MESSAGE);
    }

    @Test
    void otherCabinetReconnectDoesNotClearBCabinetAutoDrugDisconnectAlarm() {
        NettyClient client = new NettyClient(
                "172.16.85.230", 8088, "A",
                mock(NettyReceiveCabinetService.class));
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        ReflectionTestUtils.setField(client, "vacMachineExceptionService", exceptionService);

        client.clearBCabinetAutoDrugDisconnectAlarm();

        verify(exceptionService, never()).delExceptionByCodeAndDesc(
                SettingConstants.MachineException.SEND.code,
                B_CABINET_DISCONNECT_MESSAGE);
    }

    @Test
    void alarmClearFailureDoesNotEscapeReconnectCallback() {
        NettyClient client = new NettyClient(
                "172.16.85.231", 8088, "B",
                mock(NettyReceiveCabinetService.class));
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        ReflectionTestUtils.setField(client, "vacMachineExceptionService", exceptionService);
        doThrow(new RuntimeException("database unavailable"))
                .when(exceptionService)
                .delExceptionByCodeAndDesc(
                        SettingConstants.MachineException.SEND.code,
                        B_CABINET_DISCONNECT_MESSAGE);

        assertDoesNotThrow(client::clearBCabinetAutoDrugDisconnectAlarm);
    }
}
