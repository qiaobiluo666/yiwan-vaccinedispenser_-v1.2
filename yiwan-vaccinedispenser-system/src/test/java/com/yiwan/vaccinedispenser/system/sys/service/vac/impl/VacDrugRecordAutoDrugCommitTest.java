package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugRecordMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacDrugRecordAutoDrugCommitTest {

    @Test
    void inventoryIsAtomicallyIncrementedBeforeDrugRecordIsInserted() {
        Fixture fixture = fixture();
        DrugRecordRequest request = request();
        when(fixture.machineMapper.incrementInventoryIfBelowMax(request, 1, 10))
                .thenReturn(1);
        when(fixture.recordMapper.insert(any(VacDrugRecord.class))).thenReturn(1);

        int result = fixture.service.addAutoDrugRecordAndInventory(request, 1, 10);

        assertThat(result).isEqualTo(1);
        var order = inOrder(fixture.machineMapper, fixture.recordMapper);
        order.verify(fixture.machineMapper)
                .incrementInventoryIfBelowMax(request, 1, 10);
        order.verify(fixture.recordMapper).insert(any(VacDrugRecord.class));
    }

    @Test
    void fullOrMissingMachineDoesNotWriteAFalseDrugRecord() {
        Fixture fixture = fixture();
        DrugRecordRequest request = request();
        when(fixture.machineMapper.incrementInventoryIfBelowMax(request, 1, 10))
                .thenReturn(0);

        assertThatThrownBy(() ->
                fixture.service.addAutoDrugRecordAndInventory(request, 1, 10))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("库存提交失败");

        verify(fixture.recordMapper, never()).insert(any(VacDrugRecord.class));
    }

    @Test
    void recordInsertFailureRaisesAnExceptionSoSpringRollsBackInventory() {
        Fixture fixture = fixture();
        DrugRecordRequest request = request();
        when(fixture.machineMapper.incrementInventoryIfBelowMax(request, 1, 10))
                .thenReturn(1);
        when(fixture.recordMapper.insert(any(VacDrugRecord.class))).thenReturn(0);

        assertThatThrownBy(() ->
                fixture.service.addAutoDrugRecordAndInventory(request, 1, 10))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("仓位库存更新已回滚");
    }

    @Test
    void commitMethodIsTransactionalForEveryException() throws Exception {
        Method method = VacDrugRecordServiceImpl.class.getMethod(
                "addAutoDrugRecordAndInventory",
                DrugRecordRequest.class, int.class, int.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(Arrays.asList(transactional.rollbackFor()))
                .contains(Exception.class);
    }

    @Test
    void invalidCommitArgumentsAreRejectedBeforeDatabaseAccess() {
        Fixture fixture = fixture();
        DrugRecordRequest missingMachine = request();
        missingMachine.setMachineId(null);

        assertThatThrownBy(() ->
                fixture.service.addAutoDrugRecordAndInventory(null, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                fixture.service.addAutoDrugRecordAndInventory(missingMachine, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                fixture.service.addAutoDrugRecordAndInventory(request(), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(fixture.machineMapper, never())
                .incrementInventoryIfBelowMax(any(), eq(1), eq(10));
        verify(fixture.recordMapper, never()).insert(any(VacDrugRecord.class));
    }

    private Fixture fixture() {
        VacDrugRecordServiceImpl service = new VacDrugRecordServiceImpl();
        VacDrugRecordMapper recordMapper = mock(VacDrugRecordMapper.class);
        VacMachineMapper machineMapper = mock(VacMachineMapper.class);
        ReflectionTestUtils.setField(service, "vacDrugRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "vacMachineMapper", machineMapper);
        return new Fixture(service, recordMapper, machineMapper);
    }

    private DrugRecordRequest request() {
        DrugRecordRequest request = new DrugRecordRequest();
        request.setMachineId(26L);
        request.setMachineNo("A01");
        request.setVaccineId(100L);
        request.setProductNo("P-001");
        request.setProductName("测试疫苗");
        request.setBatchNo("B-001");
        request.setSupervisedCode("CODE-001");
        return request;
    }

    private record Fixture(
            VacDrugRecordServiceImpl service,
            VacDrugRecordMapper recordMapper,
            VacMachineMapper machineMapper) {
    }
}
