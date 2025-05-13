package com.yiwan.vaccinedispenser.system.sys.service.other.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.yiwan.vaccinedispenser.core.until.StringUntils;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.other.PdfService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacSendDrugRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * @author 78671
 */
@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

    @Value("${pdf.fontPath}")
    private  String fontPath;

    @Autowired
    private VacSendDrugRecordService vacSendDrugRecordService;

    @Autowired
    private VacDrugService vacDrugService;


    @Autowired
    private VacMachineService vacMachineService;

    @Override
    public ResponseEntity<byte[]> getSendDrugPdf(Date createTimeStart, Date createTimeEnd ,String workbenchName) throws DocumentException, IOException {

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 2. 获取 PdfWriter 实例
        PdfWriter.getInstance(document, outputStream);
        // 3. 打开文档
        document.open();
        // 4. 加载支持中文的字体
        Font chineseFont = loadChineseFont(fontPath);
        Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
        Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);

        // 5. 添加标题（加粗、居中）
        String title = dateFormat.format(createTimeStart) + " —— " + dateFormat.format(createTimeEnd) + " 发药记录";
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        List<SendDrugRecordRequest> vacDrugRecordList = vacSendDrugRecordService.sendDrugRecordTotalListByCreateTime(createTimeStart, createTimeEnd,workbenchName);
        PdfPTable table;
        if(workbenchName!=null && !workbenchName.isEmpty()){
             table = new PdfPTable(3);
        }else {
             table = new PdfPTable(2);
        }

        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        //表头固定
        table.setHeaderRows(1);

        String[] headers;
        if(workbenchName!=null && !workbenchName.isEmpty()){
            // 8. 设置表头
            headers = new String[]{"疫苗名称", "发苗数量", "接种台"};
            table.setWidths(new float[]{2.5f, 1.5f, 1.5f});
        }else {
            headers = new String[]{"疫苗名称", "发苗数量"};
            table.setWidths(new float[]{2.5f, 1.5f});

        }

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(3);
            table.addCell(cell);
        }

        // 9. 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (SendDrugRecordRequest record : vacDrugRecordList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getTotalNum()), chineseFont));
            if(workbenchName!=null && !workbenchName.isEmpty()){
                table.addCell(createTableCell(record.getWorkbenchName(), chineseFont));
            }
        }

        // 10. 添加表格到文档
        document.add(table);
        // 11. 关闭文档
        document.close();
        // 12. 设置响应头并返回 PDF
        HttpHeaders httpheaders = new HttpHeaders();
        httpheaders.setContentType(MediaType.APPLICATION_PDF);
        httpheaders.setContentDispositionFormData("attachment", "report.pdf");
        return ResponseEntity.ok()
                .headers(httpheaders)
                .body(outputStream.toByteArray());

    }

    @Override
    public ResponseEntity<byte[]> getSendDrugDetailPdf(Date createTimeStart, Date createTimeEnd,String workbenchName) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 2. 获取 PdfWriter 实例
        PdfWriter.getInstance(document, outputStream);
        // 3. 打开文档
        document.open();
        // 4. 加载支持中文的字体
        Font chineseFont = loadChineseFont(fontPath);
        Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
        Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);

        // 5. 添加标题（加粗、居中）
        String title = dateFormat.format(createTimeStart) + " —— " + dateFormat.format(createTimeEnd) + " 发药记录";
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        List<VacSendDrugRecord> vacDrugRecordList = vacSendDrugRecordService.sendDrugRecordListByCreateTime(createTimeStart, createTimeEnd, workbenchName);
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        //表头固定
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.5f, 1.5f, 2.5f, 2f, 2f, 2f, 2f});


        // 8. 设置表头
        String[] headers = {"疫苗名称", "仓位号", "电子监管码", "疫苗批号", "疫苗有效期", "接种台","发苗时间" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 9. 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (VacSendDrugRecord record : vacDrugRecordList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(record.getMachineNo(), chineseFont));
            table.addCell(createTableCell(record.getSupervisedCode(), chineseFont));
            table.addCell(createTableCell(record.getBatchNo(), chineseFont));
            table.addCell(createTableCell(dateFormat.format(record.getExpiredAt()), chineseFont));
            table.addCell(createTableCell(record.getWorkbenchName(), chineseFont));
            table.addCell(createTableCell(record.getCreateTime().format(formatter), chineseFont));
        }

        // 10. 添加表格到文档
        document.add(table);
        // 11. 关闭文档
        document.close();
        // 12. 设置响应头并返回 PDF
        HttpHeaders httpheaders = new HttpHeaders();
        httpheaders.setContentType(MediaType.APPLICATION_PDF);
        httpheaders.setContentDispositionFormData("attachment", "report.pdf");
        return ResponseEntity.ok()
                .headers(httpheaders)
                .body(outputStream.toByteArray());
    }

    @Override
    public ResponseEntity<byte[]> getVaccineListPdf() throws DocumentException, IOException {

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 2. 获取 PdfWriter 实例
        PdfWriter.getInstance(document, outputStream);
        // 3. 打开文档
        document.open();
        // 4. 加载支持中文的字体
        Font chineseFont = loadChineseFont(fontPath);
        Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
        Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);

        // 5. 添加标题（加粗、居中）
        String title = " 发药记录";
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);


        List<VacDrug> vacDrugList = vacDrugService.getVaccinePdf();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        //表头固定
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 1.5f});

        // 8. 设置表头
        String[] headers = {"疫苗名称", "长(mm)", "宽(mm)", "高(mm)" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 9. 填充数据
        for (VacDrug record : vacDrugList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getVaccineLong()), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getVaccineWide()), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getVaccineHigh()), chineseFont));

        }

        // 10. 添加表格到文档
        document.add(table);
        // 11. 关闭文档
        document.close();
        // 12. 设置响应头并返回 PDF
        HttpHeaders httpheaders = new HttpHeaders();
        httpheaders.setContentType(MediaType.APPLICATION_PDF);
        httpheaders.setContentDispositionFormData("attachment", "vaccine.pdf");

        return ResponseEntity.ok()
                .headers(httpheaders)
                .body(outputStream.toByteArray());
    }

    @Override
    public ResponseEntity<byte[]> getInventoryPdf(String productName) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 2. 获取 PdfWriter 实例
        PdfWriter.getInstance(document, outputStream);
        // 3. 打开文档
        document.open();
        // 4. 加载支持中文的字体
        Font chineseFont = loadChineseFont(fontPath);
        Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
        Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);
        String title;
        if(productName==null){
            title = "库存导出";
        }else {
            title = productName+"库存导出";
        }
        // 5. 添加标题（加粗、居中）
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        List<InventoryResponse> vacMachineList = vacMachineService.vacMachineInventoryListPdf(productName);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        //表头固定
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.5f,2.5f});

        // 8. 设置表头
        String[] headers = {"疫苗名称",  "疫苗数量(支)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 9. 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (InventoryResponse record : vacMachineList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getTotalVaccineNum()), chineseFont));
        }

        // 10. 添加表格到文档
        document.add(table);
        // 11. 关闭文档
        document.close();
        // 12. 设置响应头并返回 PDF
        HttpHeaders httpheaders = new HttpHeaders();
        httpheaders.setContentType(MediaType.APPLICATION_PDF);
        httpheaders.setContentDispositionFormData("attachment", "inventory.pdf");
        return ResponseEntity.ok()
                .headers(httpheaders)
                .body(outputStream.toByteArray());

    }

    @Override
    public ResponseEntity<byte[]> getInventoryPdfDetail(String productName) throws DocumentException, IOException {

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 2. 获取 PdfWriter 实例
        PdfWriter.getInstance(document, outputStream);
        // 3. 打开文档
        document.open();
        // 4. 加载支持中文的字体
        Font chineseFont = loadChineseFont(fontPath);
        Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
        Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);
        String title;
        if(productName==null){
            title = "库存导出";
        }else {
            title = productName+"库存导出";
        }
        // 5. 添加标题（加粗、居中）
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        List<VacMachine> vacMachineList = vacMachineService.getVacMachineListByProductNo(productName);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        //表头固定
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.5f, 1.5f, 1.5f,2.5f});

        // 8. 设置表头
        String[] headers = {"疫苗名称", "仓位号", "数量(支)","疫苗有效期" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 9. 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (VacMachine record : vacMachineList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(record.getBoxNo(), chineseFont));
            table.addCell(createTableCell(String.valueOf(Integer.parseInt(StringUntils.extractValue(record.getProductName()))*record.getVaccineNum()), chineseFont));
            table.addCell(createTableCell(dateFormat.format(record.getExpiredAt()), chineseFont));
        }

        // 10. 添加表格到文档
        document.add(table);
        // 11. 关闭文档
        document.close();
        // 12. 设置响应头并返回 PDF
        HttpHeaders httpheaders = new HttpHeaders();
        httpheaders.setContentType(MediaType.APPLICATION_PDF);
        httpheaders.setContentDispositionFormData("attachment", "inventory.pdf");
        return ResponseEntity.ok()
                .headers(httpheaders)
                .body(outputStream.toByteArray());




    }


    private Font loadChineseFont(String path) throws IOException, DocumentException {
        ClassPathResource fontResource = new ClassPathResource(path);
        BaseFont baseFont = BaseFont.createFont(fontResource.getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(baseFont, 12);
    }

    private PdfPCell createTableCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

}
