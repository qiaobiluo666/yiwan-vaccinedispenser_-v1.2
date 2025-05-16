package com.yiwan.vaccinedispenser.web.controller.vac;


import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.yiwan.vaccinedispenser.system.sys.service.other.ExcelService;
import com.yiwan.vaccinedispenser.system.sys.service.other.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * @author 78671
 */
@RestController
@RequestMapping("/pdf")
@Slf4j
public class PdfController {

    @Autowired
    private PdfService pdfService;


    @Autowired
    private ExcelService excelService;


    /**
     *上药记录导出
     */
    @GetMapping("/sendDrug")
    public ResponseEntity<byte[]> downloadSendDrugPdf(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
            @RequestParam String workbenchName
           ) throws Exception {

        log.info("收到参数：{}，{}",createTimeStart,createTimeEnd);
        // 8. 返回 PDF 文件流
        return pdfService.getSendDrugPdf(createTimeStart,createTimeEnd,workbenchName);
    }

    /**
     *上药记录导出
     */
    @GetMapping("/sendDrug/detail")
    public ResponseEntity<byte[]> downloadSendDrugDetailPdf(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeStart,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTimeEnd,
            @RequestParam String workbenchName
    ) throws Exception {

        log.info("收到参数：{}，{}",createTimeStart,createTimeEnd);
        // 8. 返回 PDF 文件流
        return pdfService.getSendDrugDetailPdf(createTimeStart,createTimeEnd,workbenchName);
    }






    /**
     *上药记录导出
     */
    @GetMapping("/vaccineList")
    public ResponseEntity<byte[]> downloadVaccineListPdf() throws Exception {
        // 8. 返回 PDF 文件流
        return pdfService.getVaccineListPdf();
    }

    /**
     *上药记录导出
     */
    @GetMapping("/inventory")
    public ResponseEntity<byte[]> downloadInventoryPdf(String productName) throws Exception {
        // 8. 返回 PDF 文件流
        return pdfService.getInventoryPdf(productName);
    }


    /**
     *库存详情
     */
    @GetMapping("/inventory/detail")
    public ResponseEntity<byte[]> downloadInventoryPdfDetail(String productName) throws Exception {
        // 8. 返回 PDF 文件流
        return pdfService.getInventoryPdfDetail(productName);
    }





}
