package com.yiwan.vaccinedispenser.system.sys.service.other.impl;

import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ExcelServiceImplTest {

    @Test
    void importsShelfTotalAndSkipsBlankShelfCells() throws Exception {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        when(vacDrugService.vacDrugGetByproductNo(anyString())).thenAnswer(invocation -> {
            VacDrug drug = new VacDrug();
            drug.setId("P001".equals(invocation.getArgument(0)) ? 1L : 2L);
            return drug;
        });
        when(vacDrugService.updateBatchById(anyCollection())).thenReturn(true);
        ExcelServiceImpl service = new ExcelServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);

        MockMultipartFile file = createShelfWorkbook("货架总数.xlsx", false,
                new Object[][]{{"种类A", "疫苗A", "P001", 12}, {"种类B", "疫苗B", "P002", null}});

        Result<Integer> result = service.importShelfTotalExcel(file);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<VacDrug>> captor = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(vacDrugService).updateBatchById(captor.capture());
        VacDrug update = captor.getValue().iterator().next();
        assertEquals(1L, update.getId());
        assertEquals(12, update.getShelfTotalNum());
        verify(vacDrugService, never()).vacDrugGetByproductNo("P002");
    }

    @Test
    void importsShelfTotalFromInventoryExportLayout() throws Exception {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacDrug drug = new VacDrug();
        drug.setId(9L);
        when(vacDrugService.vacDrugGetByproductNo("P009")).thenReturn(drug);
        when(vacDrugService.updateBatchById(anyCollection())).thenReturn(true);
        ExcelServiceImpl service = new ExcelServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);

        MockMultipartFile file = createShelfWorkbook("库存管理.xlsx", true,
                new Object[][]{{"种类", "疫苗", "P009", 3}});

        Result<Integer> result = service.importShelfTotalExcel(file);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData());
        verify(vacDrugService).updateBatchById(anyCollection());
    }

    @Test
    void rejectsInvalidShelfTotalWithoutUpdating() throws Exception {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        ExcelServiceImpl service = new ExcelServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);
        MockMultipartFile file = createShelfWorkbook("货架总数.xlsx", false,
                new Object[][]{{"种类", "疫苗", "P001", -1}});

        Result<Integer> result = service.importShelfTotalExcel(file);

        assertTrue(result.getCode() != 0);
        assertTrue(result.getMessage().contains("大于等于0的整数"));
        verify(vacDrugService, never()).updateBatchById(anyCollection());
    }

    @Test
    void inventoryExportKeepsSeparateCountsAndWritesCombinedTotal() throws Exception {
        VacMachineService vacMachineService = mock(VacMachineService.class);
        InventoryResponse inventory = new InventoryResponse();
        inventory.setProductName("疫苗A");
        inventory.setProductNo("P001");
        inventory.setTotalVaccineNum(4);
        inventory.setShelfTotalNum(5);
        when(vacMachineService.vacMachineInventoryListPdf("")).thenReturn(List.of(inventory));
        ExcelServiceImpl service = new ExcelServiceImpl();
        ReflectionTestUtils.setField(service, "vacMachineService", vacMachineService);

        ResponseEntity<byte[]> response = service.getInventoryExcel("");

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Row header = workbook.getSheetAt(0).getRow(1);
            Row data = workbook.getSheetAt(0).getRow(2);
            assertEquals("机内库存(支)", header.getCell(4).getStringCellValue());
            assertEquals("货架总数", header.getCell(5).getStringCellValue());
            assertEquals("疫苗总数量(支)", header.getCell(6).getStringCellValue());
            assertEquals(4, (int) data.getCell(4).getNumericCellValue());
            assertEquals(5, (int) data.getCell(5).getNumericCellValue());
            assertEquals(9, (int) data.getCell(6).getNumericCellValue());
        }
    }

    @Test
    void shelfExportContainsOnlyFourColumnsSortsByNameAndConvertsNullToZero() throws Exception {
        VacDrugService vacDrugService = mock(VacDrugService.class);
        VacDrug second = new VacDrug();
        second.setVaccineMinorName("种类乙");
        second.setProductName("乙疫苗");
        second.setProductNo("P002");
        second.setShelfTotalNum(3);
        setDimensions(second);
        VacDrug first = new VacDrug();
        first.setVaccineMinorName("种类甲");
        first.setProductName("阿疫苗");
        first.setProductNo("P001");
        first.setShelfTotalNum(null);
        first.setId(2L);
        first.setUpdateTime(LocalDateTime.of(2026, 8, 21, 12, 0));
        setDimensions(first);
        VacDrug duplicate = new VacDrug();
        duplicate.setVaccineMinorName("重复种类");
        duplicate.setProductName("重复疫苗");
        duplicate.setProductNo("P001");
        duplicate.setShelfTotalNum(99);
        duplicate.setId(1L);
        duplicate.setUpdateTime(LocalDateTime.of(2026, 8, 20, 12, 0));
        setDimensions(duplicate);
        VacDrug oldEligible = new VacDrug();
        oldEligible.setProductName("旧疫苗");
        oldEligible.setProductNo("P003");
        oldEligible.setId(3L);
        oldEligible.setUpdateTime(LocalDateTime.of(2026, 8, 19, 12, 0));
        setDimensions(oldEligible);
        VacDrug latestIneligible = new VacDrug();
        latestIneligible.setProductName("新疫苗");
        latestIneligible.setProductNo("P003");
        latestIneligible.setId(4L);
        latestIneligible.setUpdateTime(LocalDateTime.of(2026, 8, 21, 13, 0));
        when(vacDrugService.list(any())).thenReturn(List.of(second, duplicate, oldEligible, first, latestIneligible));
        ExcelServiceImpl service = new ExcelServiceImpl();
        ReflectionTestUtils.setField(service, "vacDrugService", vacDrugService);

        ResponseEntity<byte[]> response = service.getShelfTotalExcel();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(4, sheet.getRow(0).getLastCellNum());
            assertEquals("疫苗种类", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("疫苗名称", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("产品编码", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("货架总数", sheet.getRow(0).getCell(3).getStringCellValue());
            assertEquals("阿疫苗", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(0, (int) sheet.getRow(1).getCell(3).getNumericCellValue());
            assertEquals("乙疫苗", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals(2, sheet.getLastRowNum());
        }
    }

    private void setDimensions(VacDrug drug) {
        drug.setVaccineLong(10);
        drug.setVaccineWide(10);
        drug.setVaccineHigh(10);
    }

    private MockMultipartFile createShelfWorkbook(String fileName, boolean inventoryLayout, Object[][] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            int headerIndex = inventoryLayout ? 1 : 0;
            if (inventoryLayout) {
                sheet.createRow(0).createCell(0).setCellValue("发苗机库存管理");
            }
            Row header = sheet.createRow(headerIndex);
            String[] headers = inventoryLayout
                    ? new String[]{"疫苗种类", "疫苗名称", "产品编码", "生产企业", "机内库存(支)", "货架总数", "疫苗总数量(支)", "今日发苗(支)", "今日上苗(支)"}
                    : new String[]{"疫苗种类", "疫苗名称", "产品编码", "货架总数"};
            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
                header.createCell(columnIndex).setCellValue(headers[columnIndex]);
            }
            for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
                Row row = sheet.createRow(headerIndex + rowIndex + 1);
                for (int columnIndex = 0; columnIndex < data[rowIndex].length; columnIndex++) {
                    Object value = data[rowIndex][columnIndex];
                    int targetColumnIndex = inventoryLayout && columnIndex == 3 ? 5 : columnIndex;
                    if (value instanceof Number) {
                        row.createCell(targetColumnIndex).setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        row.createCell(targetColumnIndex).setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
