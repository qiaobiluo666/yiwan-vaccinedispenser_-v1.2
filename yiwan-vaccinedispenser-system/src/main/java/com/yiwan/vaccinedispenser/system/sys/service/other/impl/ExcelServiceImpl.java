package com.yiwan.vaccinedispenser.system.sys.service.other.impl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.other.ExcelService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacSendDrugRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author 78671
 */
@Service
@Slf4j
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private VacMachineService vacMachineService;

    @Autowired
    private VacSendDrugRecordService vacSendDrugRecordService;
    @Override
    public ResponseEntity<byte[]> getInventoryExcel(String productName) throws  IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("库存管理");

        // 日期标题
        String title = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 发苗机库存管理";
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle);

        // 合并标题单元格
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 表头
        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);
        String[] tableHeaders = {"疫苗名称", "疫苗数量(支)"};
        for (int i = 0; i < tableHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 写入数据
        List<InventoryResponse> vacMachineList = vacMachineService.vacMachineInventoryListPdf(productName);
        int rowNum = 2;
        for (InventoryResponse record : vacMachineList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(22);
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(record.getProductName());
            nameCell.setCellStyle(dataStyle);

            Cell numCell = row.createCell(1);
            numCell.setCellValue(record.getTotalVaccineNum());
            numCell.setCellStyle(dataStyle);
        }

        // 设置列宽
        sheet.setColumnWidth(0, 70 * 256); // 疫苗名称列宽
        sheet.setColumnWidth(1, 15 * 256); // 数量列宽
        // 写入内存流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        byte[] bytes = out.toByteArray();

        // 设置响应头
        String fileName = URLEncoder.encode(title + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    @Override
    public ResponseEntity<byte[]> getInventoryDetailExcel(String productName) throws  IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("库存管理详情");

        // 日期标题
        String title = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 发苗机库存管理详情";
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle);

        // 合并标题单元格
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 表头
        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);
        String[] tableHeaders = {"疫苗名称", "仓位号", "数量(支)","疫苗有效期" };
        for (int i = 0; i < tableHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        //时间样式
        CellStyle dateCellStyle = workbook.createCellStyle();
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));


        // 数据样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 写入数据
        List<VacMachine> vacMachineList = vacMachineService.getVacMachineListByProductNo(productName);
        int rowNum = 2;
        for (VacMachine record : vacMachineList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(22);
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(record.getProductName());
            nameCell.setCellStyle(dataStyle);

            Cell boxNoCell = row.createCell(1);
            boxNoCell.setCellValue(record.getBoxNo());
            boxNoCell.setCellStyle(dataStyle);

            Cell numCell = row.createCell(2);
            numCell.setCellValue(record.getVaccineNum());
            numCell.setCellStyle(dataStyle);

            Cell exportCell = row.createCell(3);
            exportCell.setCellValue(record.getExpiredAt());
            exportCell.setCellStyle(dateCellStyle);

        }

        // 设置列宽
        sheet.setColumnWidth(0, 70 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 15 * 256);
        sheet.setColumnWidth(3, 60 * 256);
        // 写入内存流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        byte[] bytes = out.toByteArray();

        // 设置响应头
        String fileName = URLEncoder.encode(title + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);

    }

    @Override
    public ResponseEntity<byte[]> getSendDrugExcel(Date createTimeStart, Date createTimeEnd ,String workbenchName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("发苗详情");

        // 字体设置
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setFontName("微软雅黑");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setFontName("微软雅黑");

        Font dataFont = workbook.createFont();
        dataFont.setFontName("微软雅黑");

        // 标题样式
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        String[] tableHeaders;
        // 标题内容
        String title = new SimpleDateFormat("yyyy-MM-dd").format(createTimeStart) + " — " +
                new SimpleDateFormat("yyyy-MM-dd").format(createTimeEnd) + " 发苗记录";
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        if(workbenchName!=null && !workbenchName.isEmpty()){
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2)); // 合并标题单元格
            tableHeaders = new String[]{"疫苗名称", "发苗数量(盒)", "接种台"};
        }else {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1)); // 合并标题单元格
           tableHeaders = new String[]{"疫苗名称", "发苗数量(盒)"};
        }


        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);

        // 数据单元格样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setWrapText(true);

        // 日期格式样式
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.cloneStyleFrom(dataStyle);
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));

        // 表头
        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);


        for (int i = 0; i < tableHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据填充
        List<SendDrugRecordRequest> recordList = vacSendDrugRecordService.sendDrugRecordTotalListByCreateTime(createTimeStart, createTimeEnd,workbenchName);


        int rowNum = 2;

        for (SendDrugRecordRequest record : recordList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(22);

            row.createCell(0).setCellValue(record.getProductName());
            row.getCell(0).setCellStyle(dataStyle);

            row.createCell(1).setCellValue(record.getTotalNum());
            row.getCell(1).setCellStyle(dataStyle);

            if(workbenchName!=null && !workbenchName.isEmpty()){
                row.createCell(2).setCellValue(record.getWorkbenchName());
                row.getCell(2).setCellStyle(dataStyle);
            }

        }



        sheet.setColumnWidth(0, 50 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        // 写入内存流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        byte[] bytes = out.toByteArray();

        // 设置下载响应
        String fileName = URLEncoder.encode(title + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);

    }

    @Override
    public ResponseEntity<byte[]> getSendDrugDetailExcel(Date createTimeStart, Date createTimeEnd, String workbenchName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("发苗记录详情");

        // 字体设置
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setFontName("微软雅黑");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setFontName("微软雅黑");

        Font dataFont = workbook.createFont();
        dataFont.setFontName("微软雅黑");

        // 标题样式
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 标题内容
        String title = new SimpleDateFormat("yyyy-MM-dd").format(createTimeStart) + " — " +
                new SimpleDateFormat("yyyy-MM-dd").format(createTimeEnd) + " 发苗记录详情";
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6)); // 合并标题单元格

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);

        // 数据单元格样式
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setWrapText(true);

        // 日期格式样式
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.cloneStyleFrom(dataStyle);
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));

        // 表头
        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);
        String[] tableHeaders = {"疫苗名称", "仓位号", "电子监管码", "疫苗批号", "疫苗有效期", "接种台", "发苗时间"};

        for (int i = 0; i < tableHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据填充
        List<VacSendDrugRecord> recordList = vacSendDrugRecordService.sendDrugRecordListByCreateTime(createTimeStart, createTimeEnd,workbenchName );
        int rowNum = 2;
        for (VacSendDrugRecord record : recordList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(22);

            row.createCell(0).setCellValue(record.getProductName());
            row.getCell(0).setCellStyle(dataStyle);

            row.createCell(1).setCellValue(record.getMachineNo());
            row.getCell(1).setCellStyle(dataStyle);

            row.createCell(2).setCellValue(record.getSupervisedCode());
            row.getCell(2).setCellStyle(dataStyle);

            row.createCell(3).setCellValue(record.getBatchNo());
            row.getCell(3).setCellStyle(dataStyle);

            Cell expCell = row.createCell(4);
            if (record.getExpiredAt() != null) {
                expCell.setCellValue(record.getExpiredAt());
                expCell.setCellStyle(dateCellStyle);
            } else {
                expCell.setCellStyle(dataStyle);
            }

            row.createCell(5).setCellValue(record.getWorkbenchName());
            row.getCell(5).setCellStyle(dataStyle);

            Cell createTimeCell = row.createCell(6);
            if (record.getCreateTime() != null) {
                createTimeCell.setCellValue(record.getCreateTime());
                createTimeCell.setCellStyle(dateCellStyle);
            } else {
                createTimeCell.setCellStyle(dataStyle);
            }
        }

        // 设置列宽
        sheet.setColumnWidth(0, 50 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 20 * 256);
        sheet.setColumnWidth(5, 20 * 256);
        sheet.setColumnWidth(6, 20 * 256);

        // 写入内存流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        byte[] bytes = out.toByteArray();

        // 设置下载响应
        String fileName = URLEncoder.encode(title + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);

    }


}
