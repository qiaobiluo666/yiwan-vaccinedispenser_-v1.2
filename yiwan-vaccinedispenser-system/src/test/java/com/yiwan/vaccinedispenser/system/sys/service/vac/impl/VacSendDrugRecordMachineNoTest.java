package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.dao.VacSendDrugRecordMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacWorkbenchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacSendDrugRecordMachineNoTest {

    private VacSendDrugRecordMapper recordMapper;
    private VacDrugRecordService drugRecordService;
    private VacWorkbenchService workbenchService;
    private VacDrugService drugService;
    private VacSendDrugRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        recordMapper = mock(VacSendDrugRecordMapper.class);
        drugRecordService = mock(VacDrugRecordService.class);
        workbenchService = mock(VacWorkbenchService.class);
        drugService = mock(VacDrugService.class);
        service = new VacSendDrugRecordServiceImpl();
        ReflectionTestUtils.setField(service, "vacSendDrugRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "vacDrugRecordService", drugRecordService);
        ReflectionTestUtils.setField(service, "vacWorkbenchService", workbenchService);
        ReflectionTestUtils.setField(service, "vacDrugService", drugService);
    }

    @Test
    void missingMachineNoUsesBoxNoWhenFallbackRecordIsCreated() {
        RedisDrugListData data = new RedisDrugListData();
        data.setMachineId(665L);
        data.setBoxNo("A0208");
        data.setMachineNo(null);
        data.setProductNo("00518000402_1_1");
        data.setProductName("流感疫苗");
        data.setWorkbenchNum(1);
        data.setRequestNo("request-1");
        data.setTaskId("task-1");

        when(drugRecordService.getListByMachineIdAndProductNoAndExpiredAt(any(), any(), any()))
                .thenReturn(null);
        when(workbenchService.getByWorkbenchNum(1)).thenReturn(null);
        when(drugService.vacDrugGetByproductNo(data.getProductNo())).thenReturn(null);

        service.sendDrugRecordAdd(data, 1, "发药正常");

        ArgumentCaptor<VacSendDrugRecord> captor = ArgumentCaptor.forClass(VacSendDrugRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMachineNo()).isEqualTo("A0208");
    }
}
