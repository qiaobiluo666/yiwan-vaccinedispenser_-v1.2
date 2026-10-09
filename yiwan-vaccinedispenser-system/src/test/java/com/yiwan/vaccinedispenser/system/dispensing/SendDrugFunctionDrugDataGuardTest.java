package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendDrugFunctionDrugDataGuardTest {

    @Test
    void automaticLoadingRejectsIncompleteDimensionsAndCreatesAlarm() {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacMachineExceptionService exceptionService = mock(VacMachineExceptionService.class);
        VacDrug incompleteDrug = new VacDrug();
        incompleteDrug.setId(2631L);
        incompleteDrug.setProductNo("05734001501");
        incompleteDrug.setProductName("测试疫苗");
        incompleteDrug.setVaccineLong(137);
        incompleteDrug.setVaccineWide(null);
        incompleteDrug.setVaccineHigh(26);
        when(vacDrugService.vacDrugGetValidDimensionsByProductNo("05734001501")).thenReturn(incompleteDrug);

        SendDrugFunction function = new SendDrugFunction();
        ReflectionTestUtils.setField(function, "vacDrugService", vacDrugService);
        ReflectionTestUtils.setField(function, "vacMachineExceptionService", exceptionService);
        DrugRecordRequest request = new DrugRecordRequest();
        request.setProductNo("05734001501");
        request.setProductName("测试疫苗");

        VacDrug selected = ReflectionTestUtils.invokeMethod(function, "getValidDrugForAutoLoading", request);

        assertNull(selected);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(exceptionService).sendException(anyInt(), eq("测试疫苗"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("vaccineId=2631"));
        assertTrue(messageCaptor.getValue().contains("宽=null"));
        assertTrue(messageCaptor.getValue().contains("已停止为该药品分配仓位"));
    }
}
