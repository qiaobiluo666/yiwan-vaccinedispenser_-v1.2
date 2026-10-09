package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VacMachineShelfInventoryTest {

    @Test
    void keepsMachineAndShelfCountsSeparateAndAddsShelfOnlyDrug() {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacDrug machineDrug = new VacDrug();
        machineDrug.setProductNo("P001");
        machineDrug.setProductName("疫苗A");
        machineDrug.setShelfTotalNum(null);
        VacDrug shelfOnlyDrug = new VacDrug();
        shelfOnlyDrug.setProductNo("P002");
        shelfOnlyDrug.setProductName("疫苗B");
        shelfOnlyDrug.setShelfTotalNum(7);
        shelfOnlyDrug.setId(2L);
        shelfOnlyDrug.setUpdateTime(LocalDateTime.of(2026, 8, 21, 12, 0));
        VacDrug duplicateShelfOnlyDrug = new VacDrug();
        duplicateShelfOnlyDrug.setProductNo("P002");
        duplicateShelfOnlyDrug.setProductName("重复疫苗B");
        duplicateShelfOnlyDrug.setShelfTotalNum(9);
        duplicateShelfOnlyDrug.setId(1L);
        duplicateShelfOnlyDrug.setUpdateTime(LocalDateTime.of(2026, 8, 20, 12, 0));
        when(vacDrugService.list(any())).thenReturn(List.of(machineDrug, duplicateShelfOnlyDrug, shelfOnlyDrug));

        InventoryResponse machineInventory = new InventoryResponse();
        machineInventory.setProductNo("P001");
        machineInventory.setProductName("疫苗A");
        machineInventory.setTotalVaccineNum(4);
        machineInventory.setProductCount(2);
        List<InventoryResponse> inventoryList = new ArrayList<>();
        inventoryList.add(machineInventory);

        VacMachineServiceImpl service = new VacMachineServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);
        ReflectionTestUtils.invokeMethod(service, "mergeShelfInventory", inventoryList, "");

        assertEquals(2, inventoryList.size());
        assertEquals(4, inventoryList.get(0).getTotalVaccineNum());
        assertEquals(0, inventoryList.get(0).getShelfTotalNum());
        InventoryResponse shelfOnlyInventory = inventoryList.get(1);
        assertEquals(0, shelfOnlyInventory.getTotalVaccineNum());
        assertEquals(7, shelfOnlyInventory.getShelfTotalNum());
        assertEquals(0, shelfOnlyInventory.getProductCount());
    }
}
