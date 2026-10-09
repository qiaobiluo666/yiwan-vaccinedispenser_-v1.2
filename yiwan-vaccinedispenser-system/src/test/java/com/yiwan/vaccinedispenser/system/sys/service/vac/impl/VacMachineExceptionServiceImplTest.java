package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacMachineExceptionServiceImplTest {

    @Test
    void nullProductNameMatchesLegacyNullOrBlankAlarmInsteadOfInsertingAgain() {
        VacMachineExceptionServiceImpl service = new VacMachineExceptionServiceImpl();
        VacMachineExceptionMapper mapper = mock(VacMachineExceptionMapper.class);
        ReflectionTestUtils.setField(service, "vacMachineExceptionMapper", mapper);
        when(mapper.selectList(any())).thenReturn(List.of(new VacMachineException()));

        service.sendException(
                SettingConstants.MachineException.SEND.code,
                null,
                "B柜步进电机运动异常 停止上药");

        verify(mapper).selectList(any());
        verify(mapper, never()).insert(any());
    }

    @Test
    void newGenericAlarmStoresBlankProductNameConsistently() {
        VacMachineExceptionServiceImpl service = new VacMachineExceptionServiceImpl();
        VacMachineExceptionMapper mapper = mock(VacMachineExceptionMapper.class);
        ReflectionTestUtils.setField(service, "vacMachineExceptionMapper", mapper);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());

        service.sendException(
                SettingConstants.MachineException.SEND.code,
                null,
                "B柜步进电机运动异常 停止上药");

        ArgumentCaptor<VacMachineException> captor = ArgumentCaptor.forClass(VacMachineException.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getDrugName()).isEmpty();
    }

    @Test
    void slotAlarmCopiesProductNameIntoExceptionManagement() {
        VacMachineExceptionServiceImpl service = new VacMachineExceptionServiceImpl();
        VacMachineExceptionMapper mapper = mock(VacMachineExceptionMapper.class);
        ReflectionTestUtils.setField(service, "vacMachineExceptionMapper", mapper);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());

        RedisDrugListData data = new RedisDrugListData();
        data.setBoxNo("A0917");
        data.setLineNum(9);
        data.setProductName("麻腮风疫苗");
        String description = "库存数据异常：有效期为空";

        service.dropException(SettingConstants.MachineException.SENDWARING.code, data, description);

        ArgumentCaptor<VacMachineException> captor = ArgumentCaptor.forClass(VacMachineException.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getBoxNo()).isEqualTo("A0917");
        assertThat(captor.getValue().getLineNum()).isEqualTo(9);
        assertThat(captor.getValue().getDrugName()).isEqualTo("麻腮风疫苗");
        assertThat(captor.getValue().getDescription()).isEqualTo(description);
    }
}
