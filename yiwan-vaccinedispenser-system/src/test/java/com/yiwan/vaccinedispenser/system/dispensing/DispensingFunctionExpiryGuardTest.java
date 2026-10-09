package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispensingFunctionExpiryGuardTest {

    @Test
    void allMissingExpiryStopsDispensingAndReportsEverySlot() throws Exception {
        Fixture fixture = fixture();
        VacMachine first = machine(429L, "A0917", 1, null, "202511099");
        VacMachine second = machine(430L, "A0918", 1, null, "202511099");

        assertThatThrownBy(() -> fixture.function.getDrugVacMachineMsg(List.of(first, second)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("A0917、A0918")
                .hasMessageContaining("本次发药已停止");

        verify(fixture.exceptionService).dropException(
                eq(SettingConstants.MachineException.SENDWARING.code), any(RedisDrugListData.class),
                org.mockito.ArgumentMatchers.contains("记录ID=429"));
        verify(fixture.exceptionService).dropException(
                eq(SettingConstants.MachineException.SENDWARING.code), any(RedisDrugListData.class),
                org.mockito.ArgumentMatchers.contains("记录ID=430"));
        assertCabinetAlarm(fixture.websocketService, "本次发药已停止");
    }

    @Test
    void missingExpirySlotIsSkippedWhenNormalSlotExists() throws Exception {
        Fixture fixture = fixture();
        VacMachine invalid = machine(429L, "A0917", 1, null, "202511099");
        VacMachine normal = machine(500L, "A1001", 1, new Date(), "NORMAL");

        VacMachine selected = fixture.function.getDrugVacMachineMsg(List.of(invalid, normal));

        assertThat(selected).isSameAs(normal);
        verify(fixture.exceptionService).dropException(
                eq(SettingConstants.MachineException.SENDWARING.code), any(RedisDrugListData.class),
                org.mockito.ArgumentMatchers.contains("有效期为空"));
        assertCabinetAlarm(fixture.websocketService, "继续使用正常仓位发药");
    }

    @Test
    void emptyCandidateListKeepsNoStockFlowAndDoesNotRaiseDataAlarm() {
        Fixture fixture = fixture();

        assertThat(fixture.function.filterDrugListWithValidExpiry(Collections.emptyList())).isEmpty();

        verify(fixture.exceptionService, never()).dropException(any(), any(), anyString());
    }

    @Test
    void missingBatchWithValidExpiryCanDispenseNormally() throws Exception {
        Fixture fixture = fixture();
        VacMachine normal = machine(501L, "A1002", 1, new Date(), null);

        VacMachine selected = fixture.function.getDrugVacMachineMsg(List.of(normal));

        assertThat(selected).isSameAs(normal);
        verify(fixture.exceptionService, never()).dropException(any(), any(), anyString());
        verify(fixture.websocketService, never()).sendInfo(any(Integer.class), any(Map.class));
    }

    @Test
    void multiDoseSlotWithMissingExpiryIsAlsoBlocked() throws Exception {
        Fixture fixture = fixture();
        VacMachine multiDose = machine(502L, "A1003", 2, null, "MULTI");

        assertThatThrownBy(() -> fixture.function.getDrugVacMachineMsg(List.of(multiDose)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("本次发药已停止");

        verify(fixture.exceptionService).dropException(
                eq(SettingConstants.MachineException.SENDWARING.code), any(RedisDrugListData.class),
                org.mockito.ArgumentMatchers.contains("状态=2"));
    }

    @Test
    void sameExpiryUsesEarliestLoadedBatchInsteadOfSmallestInventoryBatch() {
        Fixture fixture = fixture();
        Date expiry = new Date(1845043200000L);
        VacMachine earliestBatch = machine(880L, "A1007", 1, expiry, "202506048A");
        earliestBatch.setVaccineNum(41);
        earliestBatch.setVaccineUseNum(41);
        VacMachine newerBatch = machine(853L, "A0907", 1, expiry, "202506049A");
        newerBatch.setVaccineNum(13);
        newerBatch.setVaccineUseNum(13);
        when(fixture.drugRecordService.getBatchNoEarly(
                anyList(), eq(List.of("TEST-PRODUCT")), eq(expiry)))
                .thenReturn("202506048A");

        VacMachine selected = fixture.function.getDrugVacMachineMsg(
                List.of(newerBatch, earliestBatch));

        assertThat(selected).isSameAs(earliestBatch);
        verify(fixture.machineService, never()).getBatchListNum(anyList());
        verify(fixture.drugRecordService).getBatchNoEarly(
                anyList(), eq(List.of("TEST-PRODUCT")), eq(expiry));
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture() {
        DispensingFunction function = new DispensingFunction();
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        WebsocketService websocketService = mock(WebsocketService.class);
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        VacMachineService machineService = mock(VacMachineService.class);
        VacDrugRecordService drugRecordService = mock(VacDrugRecordService.class);
        ReflectionTestUtils.setField(function, "vacMachineExceptionService", exceptionService);
        ReflectionTestUtils.setField(function, "websocketService", websocketService);
        ReflectionTestUtils.setField(function, "listOps", listOperations);
        ReflectionTestUtils.setField(function, "vacMachineService", machineService);
        ReflectionTestUtils.setField(function, "vacDrugRecordService", drugRecordService);
        when(listOperations.size(anyString())).thenReturn(0L);
        return new Fixture(function, exceptionService, websocketService,
                machineService, drugRecordService);
    }

    private VacMachine machine(Long id, String boxNo, int status, Date expiredAt, String batchNo) {
        VacMachine machine = new VacMachine();
        machine.setId(id);
        machine.setBoxNo(boxNo);
        machine.setLineNum(1);
        machine.setPositionNum(1);
        machine.setStatus(status);
        machine.setProductName("测试疫苗");
        machine.setProductNo("TEST-PRODUCT");
        machine.setBatchNo(batchNo);
        machine.setExpiredAt(expiredAt);
        machine.setVaccineNum(15);
        machine.setVaccineUseNum(15);
        return machine;
    }

    @SuppressWarnings("unchecked")
    private void assertCabinetAlarm(WebsocketService websocketService, String expectedMessage) throws Exception {
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(websocketService).sendInfo(
                eq(CommandEnums.ON_CABINET_WEB.getCode()), messageCaptor.capture());
        assertThat(messageCaptor.getValue().get("code"))
                .isEqualTo(CommandEnums.MACHINE_STATUS_COMMAND.getCode());
        assertThat(messageCaptor.getValue().get("data").toString()).contains(expectedMessage);
    }

    private record Fixture(
            DispensingFunction function,
            VacMachineExceptionService exceptionService,
            WebsocketService websocketService,
            VacMachineService machineService,
            VacDrugRecordService drugRecordService) {
    }
}
