package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VacDrugServiceImplDistanceGuardTest {

    private static final String CODE = "81903350557552495309";
    private static final String MESSAGE = "正在自动上药，不能获取疫苗尺寸，请先停止自动上药";

    @Test
    void plcModeDoesNotStartDistanceMeasurementWhileAutoDrugIsRunning() throws Exception {
        PlcSendService plcSendService = mock(PlcSendService.class);
        GuardFixture fixture = guardFixture(plcSendService);

        Result result = fixture.service.drugDistance(CODE);

        assertBlocked(result);
        verify(fixture.valueOperations).get(RedisKeyConstant.autoDrug.AUTO_DRUG_START);
        verifyNoInteractions(fixture.configFunction, fixture.zcyFunction, plcSendService);
        verify(fixture.sendDrugFunction, never()).drugDistance(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void legacyControllerModeDoesNotStartDistanceMeasurementWhileAutoDrugIsRunning() throws Exception {
        GuardFixture fixture = guardFixture(null);

        Result result = fixture.service.drugDistance(CODE);

        assertBlocked(result);
        verify(fixture.valueOperations).get(RedisKeyConstant.autoDrug.AUTO_DRUG_START);
        verifyNoInteractions(fixture.configFunction, fixture.zcyFunction);
        verify(fixture.sendDrugFunction, never()).drugDistance(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void plcManualDistanceReturnsQualifiedWhenAllDimensionsArePresent() throws Exception {
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);
        when(valueOperations.get(RedisKeyConstant.bJudge.LENGTH)).thenReturn("65");
        when(valueOperations.get(RedisKeyConstant.bJudge.WIDTH)).thenReturn("28");
        when(valueOperations.get(RedisKeyConstant.bJudge.HEIGHT)).thenReturn("19");
        VacDrug vacDrug = new VacDrug();

        VacDrug result = service.drugDistancePlc(vacDrug, 20L);

        assertSame(vacDrug, result);
        assertEquals(65, result.getVaccineLong());
        assertEquals(28, result.getVaccineWide());
        assertEquals(19, result.getVaccineHigh());
        InOrder order = inOrder(plcSendService);
        order.verify(plcSendService).sendBCabinetJudgeTest();
        order.verify(plcSendService).sendBCabinetJudgeQualified();
        verify(plcSendService, never()).sendBCabinetJudgeUnqualified();
    }

    @Test
    void plcManualDistanceReturnsUnqualifiedWhenDimensionsRemainZero() throws Exception {
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);
        when(valueOperations.get(RedisKeyConstant.bJudge.LENGTH)).thenReturn("0");
        when(valueOperations.get(RedisKeyConstant.bJudge.WIDTH)).thenReturn("0");
        when(valueOperations.get(RedisKeyConstant.bJudge.HEIGHT)).thenReturn("0");

        VacDrug result = service.drugDistancePlc(new VacDrug(), 20L);

        assertNull(result);
        InOrder order = inOrder(plcSendService);
        order.verify(plcSendService).sendBCabinetJudgeTest();
        order.verify(plcSendService).sendBCabinetJudgeUnqualified();
        verify(plcSendService, never()).sendBCabinetJudgeQualified();
    }

    @Test
    void plcDistanceAlarmValuesUseMessagesShownByVaccineSizeDialog() {
        assertEquals("B柜检测长宽厚报警：没有检测到药盒",
                VacDrugServiceImpl.plcDistanceAlarmMessage("1"));
        assertEquals("B柜检测长宽厚报警：距离传感器异常",
                VacDrugServiceImpl.plcDistanceAlarmMessage("2"));
        assertNull(VacDrugServiceImpl.plcDistanceAlarmMessage("0"));
    }

    @Test
    void plcManualDistanceReturnsImmediatelyWhenNoDrugBoxAlarmArrives() throws Exception {
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        PlcSendService plcSendService = mock(PlcSendService.class);
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);
        when(valueOperations.get(RedisKeyConstant.bJudge.ALARM)).thenReturn("1");

        VacDrug result = service.drugDistancePlc(new VacDrug(), 35_000L);

        assertNull(result);
        InOrder order = inOrder(plcSendService);
        order.verify(plcSendService).sendBCabinetJudgeTest();
        order.verify(plcSendService).sendBCabinetJudgeUnqualified();
        verify(plcSendService, never()).sendBCabinetJudgeQualified();
    }

    private static void assertBlocked(Result result) {
        assertNotEquals(0, result.getCode());
        assertEquals(MESSAGE, result.getMessage());
    }

    private static GuardFixture guardFixture(PlcSendService plcSendService) {
        VacDrugServiceImpl service = new VacDrugServiceImpl();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ConfigFunction configFunction = mock(ConfigFunction.class);
        ZcyFunction zcyFunction = mock(ZcyFunction.class);
        SendDrugFunction sendDrugFunction = mock(SendDrugFunction.class);

        when(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START)).thenReturn("true");
        ReflectionTestUtils.setField(service, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(service, "configFunction", configFunction);
        ReflectionTestUtils.setField(service, "zcyFunction", zcyFunction);
        ReflectionTestUtils.setField(service, "sendDrugFunction", sendDrugFunction);
        ReflectionTestUtils.setField(service, "plcSendService", plcSendService);

        return new GuardFixture(service, valueOperations, configFunction, zcyFunction, sendDrugFunction);
    }

    private static final class GuardFixture {
        private final VacDrugServiceImpl service;
        private final ValueOperations<String, String> valueOperations;
        private final ConfigFunction configFunction;
        private final ZcyFunction zcyFunction;
        private final SendDrugFunction sendDrugFunction;

        private GuardFixture(VacDrugServiceImpl service,
                             ValueOperations<String, String> valueOperations,
                             ConfigFunction configFunction,
                             ZcyFunction zcyFunction,
                             SendDrugFunction sendDrugFunction) {
            this.service = service;
            this.valueOperations = valueOperations;
            this.configFunction = configFunction;
            this.zcyFunction = zcyFunction;
            this.sendDrugFunction = sendDrugFunction;
        }
    }
}
