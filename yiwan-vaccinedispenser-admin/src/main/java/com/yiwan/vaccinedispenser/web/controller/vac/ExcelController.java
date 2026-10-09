package com.yiwan.vaccinedispenser.web.controller.vac;


import com.itextpdf.text.DocumentException;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.service.other.ExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 78671
 */
@RestController
@RequestMapping("/excel")
@Slf4j
public class ExcelController {

    @Autowired
    private ExcelService excelService;


    /**
     *
     * @return  导出库存Excel文件
     * @throws IOException
     * @throws DocumentException
     */
    @GetMapping("/inventory")
    public ResponseEntity<byte[]> exportInventoryExcel( @RequestParam String productName) throws IOException, DocumentException {
       return excelService.getInventoryExcel(productName);
    }

    /**
     * 导出货架总数Excel
     */
    @GetMapping("/shelfTotal")
    public ResponseEntity<byte[]> exportShelfTotalExcel() throws IOException {
        return excelService.getShelfTotalExcel();
    }

    /**
     * 导入货架总数Excel
     */
    @PostMapping("/shelfTotal/import")
    public Result<Integer> importShelfTotalExcel(@RequestParam("file") MultipartFile file) {
        return excelService.importShelfTotalExcel(file);
    }

    /**
     * @return  导出库存详情Excel文件
     * @throws IOException
     * @throws DocumentException
     */

    @GetMapping("/inventory/detail")
    public ResponseEntity<byte[]> exportInventoryDetailExcel( @RequestParam String productName) throws IOException, DocumentException {
        return excelService.getInventoryDetailExcel(productName);
    }



    /*
     */
    @GetMapping("/sendDrug")
    public ResponseEntity<byte[]> downloadSendDrugExcel(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
            @RequestParam String workbenchName
    ) throws Exception {

        log.info("收到参数：{}，{},{}",createTimeStart,createTimeEnd,workbenchName);
        // 8. 返回 PDF 文件流
        return excelService.getSendDrugExcel(createTimeStart,createTimeEnd,workbenchName);

    }


    /**
     *上药记录导出
     */
    @GetMapping("/sendDrug/detail")
    public ResponseEntity<byte[]> downloadSendDetailDrugExcel(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
            @RequestParam String workbenchName
    ) throws Exception {

        log.info("收到参数：{}，{},{}",createTimeStart,createTimeEnd,workbenchName);
        // 8. 返回 PDF 文件流
        return excelService.getSendDrugDetailExcel(createTimeStart,createTimeEnd,workbenchName);

    }

    @GetMapping("/drugRecord")
    public ResponseEntity<byte[]> downloadDrugRecordExcel(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd
    ) throws Exception {
        log.info("收到参数：{}，{}",createTimeStart,createTimeEnd);
        return excelService.getDrugRecordExcel(createTimeStart,createTimeEnd);
    }

    @GetMapping("/drugRecord/detail")
    public ResponseEntity<byte[]> downloadDrugRecordDetailExcel(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd
    ) throws Exception {
        log.info("收到参数：{}，{}",createTimeStart,createTimeEnd);
        return excelService.getDrugRecordDetailExcel(createTimeStart,createTimeEnd);
    }

    /**
     * 药盒统计导出
     */
    @GetMapping("/drugBox/statistics")
    public ResponseEntity<byte[]> downloadDrugBoxStatisticsExcel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
            @RequestParam(required = false) String workbenchName,
            @RequestParam(required = false) String productNo
    ) throws Exception {
        log.info("药盒统计导出，参数：{}，{}，{}，{}",createTimeStart,createTimeEnd,workbenchName,productNo);
        return excelService.getDrugBoxStatisticsExcel(createTimeStart,createTimeEnd,workbenchName,productNo);
    }



 }
