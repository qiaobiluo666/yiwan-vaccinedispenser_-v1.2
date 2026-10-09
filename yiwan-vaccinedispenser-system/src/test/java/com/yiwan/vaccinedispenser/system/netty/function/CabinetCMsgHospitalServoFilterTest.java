package com.yiwan.vaccinedispenser.system.netty.function;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CabinetCMsgHospitalServoFilterTest {

    @Test
    @SuppressWarnings("unchecked")
    void lianShiHospitalSuppressesFourthServoAlarm() throws Exception {
        Fixture fixture = fixture();
        ConfigSetting setting = new ConfigSetting();
        setting.setHospitalName(SettingConstants.LIAN_SHI_HOSPITAL_NAME);

        fixture.target.receiveServo(alarmFrame("04"), setting);

        verify(fixture.values, never()).set(RedisKeyConstant.CABINET_C_SERVO_ERROR, "true");
        verify(fixture.exceptionService, never()).sendException(anyInt(), anyString());
        verify(fixture.websocketService, never()).sendInfo(anyInt(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void lianShiHospitalStillReportsOtherServoAlarm() throws Exception {
        Fixture fixture = fixture();
        ConfigSetting setting = new ConfigSetting();
        setting.setHospitalName(SettingConstants.LIAN_SHI_HOSPITAL_NAME);

        fixture.target.receiveServo(alarmFrame("03"), setting);

        verify(fixture.values).set(RedisKeyConstant.CABINET_C_SERVO_ERROR, "true");
        verify(fixture.exceptionService).sendException(
                SettingConstants.MachineException.SENDWARING.code,
                "C柜第3伺服报警，发药异常，请联系售后人员！A5 5A 0C 00 0C 03 00 03 55");
        verify(fixture.websocketService).sendInfo(
                org.mockito.ArgumentMatchers.eq(CommandEnums.MACHINE_STATUS_COMMAND.getCode()), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherHospitalStillReportsFourthServoAlarm() throws Exception {
        Fixture fixture = fixture();
        ConfigSetting setting = new ConfigSetting();
        setting.setHospitalName("其他医院");

        fixture.target.receiveServo(alarmFrame("04"), setting);

        verify(fixture.values).set(RedisKeyConstant.CABINET_C_SERVO_ERROR, "true");
        verify(fixture.exceptionService).sendException(
                SettingConstants.MachineException.SENDWARING.code,
                "C柜第4伺服报警，发药异常，请联系售后人员！A5 5A 0C 00 0C 03 00 04 55");
        verify(fixture.websocketService).sendInfo(
                org.mockito.ArgumentMatchers.eq(CommandEnums.MACHINE_STATUS_COMMAND.getCode()), anyMap());
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture() {
        CabinetCMsg target = new CabinetCMsg();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ReflectionTestUtils.setField(target, "valueOperations", values);
        ReflectionTestUtils.setField(target, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(target, "websocketService", websocketService);
        return new Fixture(target, values, exceptionService, websocketService);
    }

    private String[] alarmFrame(String servoAddress) {
        return new String[]{
                "A5", "5A", "0C", "00", "01", "03", "0C",
                "03", "00", servoAddress, "00", "03", "55"
        };
    }

    private static class Fixture {
        private final CabinetCMsg target;
        private final ValueOperations<String, String> values;
        private final VacMachineExceptionService exceptionService;
        private final WebsocketService websocketService;

        private Fixture(CabinetCMsg target,
                        ValueOperations<String, String> values,
                        VacMachineExceptionService exceptionService,
                        WebsocketService websocketService) {
            this.target = target;
            this.values = values;
            this.exceptionService = exceptionService;
            this.websocketService = websocketService;
        }
    }
}
