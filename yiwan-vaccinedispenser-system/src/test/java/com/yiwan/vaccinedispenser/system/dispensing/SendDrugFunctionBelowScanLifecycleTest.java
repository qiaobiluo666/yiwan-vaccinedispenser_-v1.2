package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.camera.CameraSendMsg;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.ScanCodeData;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SendDrugFunctionBelowScanLifecycleTest {

    @Test
    @SuppressWarnings("unchecked")
    void rotateAngleWaitsForParallelBelowCameraThreadBeforeReturning() throws Exception {
        SendDrugFunction function = spy(new SendDrugFunction());
        CameraSendMsg camera = mock(CameraSendMsg.class);
        CabinetBService cabinetBService = mock(CabinetBService.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        AtomicInteger belowCommands = new AtomicInteger();

        ReflectionTestUtils.setField(function, "cameraSendMsg", camera);
        ReflectionTestUtils.setField(function, "cabinetBService", cabinetBService);
        ReflectionTestUtils.setField(function, "valueOperations", values);
        when(values.get(RedisKeyConstant.scanCode.SIDE)).thenReturn("NoRead");
        when(values.get(RedisKeyConstant.scanCode.BELOW)).thenReturn("NoRead");
        doAnswer(invocation -> {
            belowCommands.incrementAndGet();
            return true;
        }).when(camera).sendCommandToBelowCamera();
        doReturn(true).when(function).moveScanServo(any(DistanceServoData.class), eq(2));
        doReturn(true).when(function).cabinetBStepPosition(
                CabinetConstants.CabinetBStepMode.ROTATE,
                SettingConstants.AngleDistance.RETURN);

        long startedAt = System.currentTimeMillis();
        function.rotateAngle(
                CabinetConstants.CabinetBStepMode.ROTATE,
                new ScanCodeData(), new DistanceServoData(), new ConfigData());
        long elapsed = System.currentTimeMillis() - startedAt;
        int commandsAtReturn = belowCommands.get();

        assertThat(commandsAtReturn).isGreaterThan(0);
        assertThat(elapsed).isGreaterThanOrEqualTo(
                SettingConstants.SCAN_STEP_BELOW_WAIT_TIME - 150L);

        // rotateAngle返回后不应再有旧下方扫码线程继续发命令。
        Thread.sleep(250);
        assertThat(belowCommands.get()).isEqualTo(commandsAtReturn);
    }
}
