package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugFunctionReturnGuardTest {

    @Test
    void distanceSizeMismatchReturnIsIncludedInConsecutiveCounter() throws Exception {
        Fixture fixture = fixture("0");
        prepareDistanceMismatch(fixture);

        fixture.function.goTable(new ConfigData());

        verify(fixture.function).tableReturn();
        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "1");
        verify(fixture.threadManager, never()).stopAfterScanReturn();
    }

    @Test
    void tenthDistanceSizeMismatchReturnStopsAutoDrug() throws Exception {
        Fixture fixture = fixture("9");
        prepareDistanceMismatch(fixture);

        fixture.function.goTable(new ConfigData());

        verify(fixture.function).tableReturn();
        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "10");
        verify(fixture.threadManager).stopAfterScanReturn();
        verify(fixture.exceptionService).sendException(
                eq(SettingConstants.MachineException.SEND.code), eq(""), contains("连续10次回退"));
    }

    @Test
    void ninthConsecutiveReturnDoesNotStopAutoDrug() throws Exception {
        Fixture fixture = fixture("8");

        ReflectionTestUtils.invokeMethod(
                fixture.function, "returnAfterScan", new ConfigData(), "没扫码到码,退回");

        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "9");
        verify(fixture.threadManager, never()).stopAfterScanReturn();
        verify(fixture.exceptionService, never()).sendException(
                eq(SettingConstants.MachineException.SEND.code), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void tenthConsecutiveReturnStopsAndPushesAlarm() throws Exception {
        Fixture fixture = fixture("9");

        ReflectionTestUtils.invokeMethod(
                fixture.function, "returnAfterScan", new ConfigData(), "没扫码到码,退回");

        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "10");
        verify(fixture.threadManager).stopAfterScanReturn();
        verify(fixture.exceptionService).sendException(
                eq(SettingConstants.MachineException.SEND.code), eq(""), contains("连续10次"));

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.websocketService).sendInfo(
                eq(CommandEnums.ON_CABINET_WEB.getCode()), messageCaptor.capture());
        assertThat(messageCaptor.getValue().get("code"))
                .isEqualTo(CommandEnums.MACHINE_STATUS_COMMAND.getCode());
        assertThat(messageCaptor.getValue().get("data").toString()).contains("连续10次");
    }

    @Test
    void normalDrugResetsConsecutiveReturnCount() throws Exception {
        Fixture fixture = fixture("1");
        when(fixture.valueOperations.get(RedisKeyConstant.CABINET_B_ERROR_COUNT)).thenReturn("4");

        ReflectionTestUtils.invokeMethod(fixture.function, "resetConsecutiveScanReturnCount");

        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "0");
    }

    @Test
    void legacyNonNumericCounterIsRepairedAndCountsFromOne() throws Exception {
        Fixture fixture = fixture("false");

        ReflectionTestUtils.invokeMethod(
                fixture.function, "returnAfterScan", new ConfigData(), "没扫码到码,退回");

        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "1");
        verify(fixture.threadManager, never()).stopAfterScanReturn();
    }

    @Test
    void unavailableAboveCameraRestoresDistanceFlagAndWaitsForReconnect() throws Exception {
        Fixture fixture = fixture("0");
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START))
                .thenReturn("false");
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH))
                .thenReturn("true");
        when(fixture.valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN))
                .thenReturn("true");
        doReturn(false).when(fixture.function).aboveCamera();

        fixture.function.goTable(new ConfigData());

        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_TEST_RUN, "false");
        verify(fixture.valueOperations).set(RedisKeyConstant.CABINET_B_TEST_RUN, "true");
        verify(fixture.function, never()).distanceServoAll(any(ConfigData.class));
    }

    @Test
    void recoveredAboveCameraContinuesOnNextLoopWithoutProgramRestart() throws Exception {
        Fixture fixture = fixture("0");
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START))
                .thenReturn("false");
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH))
                .thenReturn("true");
        when(fixture.valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN))
                .thenReturn("true");
        doReturn(false, true).when(fixture.function).aboveCamera();
        DistanceServoData mismatch = new DistanceServoData();
        mismatch.setIsReturn(true);
        doReturn(mismatch).when(fixture.function).distanceServoAll(any(ConfigData.class));
        doNothing().when(fixture.function).tableReturn();

        fixture.function.goTable(new ConfigData());
        fixture.function.goTable(new ConfigData());

        verify(fixture.function, times(2)).aboveCamera();
        verify(fixture.function).distanceServoAll(any(ConfigData.class));
    }

    private void prepareDistanceMismatch(Fixture fixture) throws Exception {
        DistanceServoData mismatch = new DistanceServoData();
        mismatch.setIsReturn(true);
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START))
                .thenReturn("false");
        when(fixture.valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH))
                .thenReturn("true");
        when(fixture.valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN))
                .thenReturn("true");
        doReturn(true).when(fixture.function).aboveCamera();
        doReturn(mismatch).when(fixture.function).distanceServoAll(any(ConfigData.class));
        doNothing().when(fixture.function).tableReturn();
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(String storedCount) throws Exception {
        SendDrugFunction function = spy(new SendDrugFunction());
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SendDrugThreadManager threadManager = mock(SendDrugThreadManager.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);

        ReflectionTestUtils.setField(function, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(function, "sendDrugThreadManager", threadManager);
        ReflectionTestUtils.setField(function, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(function, "websocketService", websocketService);
        doNothing().when(function).servoTableReturn(any(ConfigData.class), anyString());
        when(valueOperations.get(RedisKeyConstant.CABINET_B_ERROR_COUNT))
                .thenReturn(storedCount);

        return new Fixture(function, valueOperations, threadManager, exceptionService, websocketService);
    }

    private record Fixture(
            SendDrugFunction function,
            ValueOperations<String, String> valueOperations,
            SendDrugThreadManager threadManager,
            VacMachineExceptionService exceptionService,
            WebsocketService websocketService) {
    }
}
