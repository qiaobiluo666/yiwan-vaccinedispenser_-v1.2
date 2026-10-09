package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MarginRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.MarginResponse;
import com.yiwan.vaccinedispenser.system.sys.service.vac.MarginService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacBoxSpecService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.until.BoxSpecIds;
import com.yiwan.vaccinedispenser.system.until.PdfFontLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 仓容余量实现
 * <p>
 * 空余仓位数量：vac_machine 中 box_spec_id 匹配、product_no 为空、正常状态（status=1）的仓位计数；
 * 单仓可上最大数量：与自动上药 getDrugNum 同一套算法（板长/(药长+5)，误差修正后与 maxNum 取小）；
 * 最大可上药量 marginNum = 空余仓位数量 × 单仓可上最大数量。
 *
 * @author codex
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class MarginServiceImpl implements MarginService {

    private final String fontPath;
    private final VacDrugService vacDrugService;
    private final VacBoxSpecService vacBoxSpecService;
    private final VacMachineService vacMachineService;
    private final ConfigFunction configFunction;

    public MarginServiceImpl(
            @Value("${pdf.fontPath}") String fontPath,
            VacDrugService vacDrugService,
            VacBoxSpecService vacBoxSpecService,
            VacMachineService vacMachineService,
            ConfigFunction configFunction) {
        this.fontPath = fontPath;
        this.vacDrugService = vacDrugService;
        this.vacBoxSpecService = vacBoxSpecService;
        this.vacMachineService = vacMachineService;
        this.configFunction = configFunction;
    }

    @Override
    public Page<MarginResponse> marginList(MarginRequest request) {
        int page = request.getPage() == null ? 1 : Math.max(1, request.getPage());
        int size = request.getSize() == null ? 10 : Math.min(200, Math.max(1, request.getSize()));
        List<MarginResponse> all = listMarginData(request.getProductName(), request.getProductNames());

        int total = all.size();
        int from = (int) Math.min((long) (page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<MarginResponse> records = new ArrayList<>(all.subList(from, to));

        Page<MarginResponse> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    /**
     * 遍历有长宽高数据的疫苗，结合自动上药规格匹配，计算每条仓容余量
     */
    private List<MarginResponse> listMarginData(String productName, List<String> productNames) {
        LambdaQueryWrapper<VacDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(VacDrug::getVaccineLong)
                .isNotNull(VacDrug::getVaccineWide)
                .isNotNull(VacDrug::getVaccineHigh)
                .eq(VacDrug::getDeleted, 0);
        List<String> selectedProductNames = normalizeProductNames(productNames);
        if (!selectedProductNames.isEmpty()) {
            wrapper.in(VacDrug::getProductName, selectedProductNames);
        } else if (StringUtils.hasText(productName)) {
            wrapper.like(VacDrug::getProductName, productName);
        }
        wrapper.orderByAsc(VacDrug::getProductName);

        List<VacDrug> drugList = vacDrugService.list(wrapper);
        if (drugList.isEmpty()) {
            return Collections.emptyList();
        }

        List<VacBoxSpec> allBoxSpecs = vacBoxSpecService.list(new LambdaQueryWrapper<VacBoxSpec>()
                .eq(VacBoxSpec::getDeleted, 0)
                .orderByAsc(VacBoxSpec::getLength));
        Map<Long, VacBoxSpec> boxSpecById = allBoxSpecs.stream()
                .collect(Collectors.toMap(VacBoxSpec::getId, Function.identity(), (left, right) -> left));

        List<VacMachine> emptyBoxes = vacMachineService.list(new LambdaQueryWrapper<VacMachine>()
                .select(VacMachine::getBoxSpecId)
                .eq(VacMachine::getDeleted, 0)
                .eq(VacMachine::getStatus, 1)
                .and(query -> query.isNull(VacMachine::getProductNo)
                        .or()
                        .eq(VacMachine::getProductNo, "")));
        Map<Long, Integer> emptyBoxCountBySpecId = new HashMap<>();
        for (VacMachine emptyBox : emptyBoxes) {
            if (emptyBox.getBoxSpecId() != null) {
                emptyBoxCountBySpecId.merge(emptyBox.getBoxSpecId(), 1, Integer::sum);
            }
        }

        ConfigData configData = configFunction.getAutoDrugConfigData();
        Integer lineLong = configData == null ? null : configData.getLineLong();

        List<MarginResponse> result = new ArrayList<>();
        for (VacDrug drug : drugList) {
            List<VacBoxSpec> boxSpecList = resolveBoxSpecs(drug, allBoxSpecs, boxSpecById);
            if (boxSpecList == null || boxSpecList.isEmpty()) {
                continue;
            }

            int emptyBoxNum = boxSpecList.stream()
                    .map(VacBoxSpec::getId)
                    .mapToInt(id -> emptyBoxCountBySpecId.getOrDefault(id, 0))
                    .sum();

            int boxMaxNum = calcBoxMaxNum(lineLong, drug);
            if (boxMaxNum <= 0) {
                continue;
            }

            MarginResponse marginResponse = new MarginResponse();
            marginResponse.setProductName(drug.getProductName());
            marginResponse.setProductNo(drug.getProductNo());
            marginResponse.setBoxSpecName(boxSpecList.stream()
                    .map(VacBoxSpec::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("、")));
            marginResponse.setEmptyBoxNum(emptyBoxNum);
            marginResponse.setBoxMaxNum(boxMaxNum);
            marginResponse.setMarginNum(Math.multiplyExact(emptyBoxNum, boxMaxNum));
            result.add(marginResponse);
        }
        result.sort(Comparator.comparing(
                MarginResponse::getBoxSpecName,
                Comparator.nullsLast(Collator.getInstance(Locale.CHINA))));
        return result;
    }

    static List<String> normalizeProductNames(List<String> productNames) {
        if (productNames == null || productNames.isEmpty()) {
            return Collections.emptyList();
        }
        return productNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    static List<VacBoxSpec> resolveBoxSpecs(
            VacDrug drug,
            List<VacBoxSpec> allBoxSpecs,
            Map<Long, VacBoxSpec> boxSpecById) {
        List<Long> specifiedIds = BoxSpecIds.parse(drug.getBoxSpecIds());
        if (!specifiedIds.isEmpty()) {
            return specifiedIds.stream()
                    .map(boxSpecById::get)
                    .filter(spec -> spec != null)
                    .toList();
        }

        Integer vaccineWide = drug.getVaccineWide();
        if (vaccineWide == null) {
            return Collections.emptyList();
        }
        return allBoxSpecs.stream()
                .filter(spec -> spec.getLength() != null && spec.getRanges() != null)
                .filter(spec -> spec.getLength() >= vaccineWide)
                .filter(spec -> spec.getLength() - spec.getRanges() < vaccineWide)
                .toList();
    }

    /**
     * 与自动上药 getDrugNum 同口径：板长/(药长+5)，误差修正，再与单仓最大限制取小
     */
    static int calcBoxMaxNum(Integer lineLong, VacDrug drug) {
        if (drug == null || lineLong == null || lineLong <= 0
                || drug.getVaccineLong() == null || drug.getVaccineLong() <= 0) {
            return 0;
        }
        int num = lineLong / (drug.getVaccineLong() + 5);
        if (5 * num > drug.getVaccineLong() * 1.5) {
            num = num + 1;
        }
        if (drug.getMaxNum() != null) {
            num = Math.min(num, drug.getMaxNum());
        }
        return Math.max(0, num);
    }

    @Override
    public ResponseEntity<byte[]> marginPdf(List<String> productNames) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        try {
            document.open();

            Font chineseFont = loadChineseFont(fontPath);
            Font titleFont = new Font(chineseFont.getBaseFont(), 16, Font.BOLD);
            Font headerFont = new Font(chineseFont.getBaseFont(), 12, Font.BOLD);

        List<String> selectedProductNames = normalizeProductNames(productNames);
        String title = selectedProductNames.size() == 1
                ? selectedProductNames.get(0) + " 仓容余量" : "仓容余量";
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        List<MarginResponse> dataList = listMarginData(null, selectedProductNames);
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setHeaderRows(1);
        table.setWidths(new float[]{2.2f, 2.0f, 2.0f, 1.6f, 1.8f, 1.8f});

        String[] headers = {"产品名称", "产品编码", "仓柜规格", "空余仓位数量", "单仓可上最大数量", "最大可上药量"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (MarginResponse record : dataList) {
            table.addCell(createTableCell(record.getProductName(), chineseFont));
            table.addCell(createTableCell(record.getProductNo(), chineseFont));
            table.addCell(createTableCell(record.getBoxSpecName(), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getEmptyBoxNum()), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getBoxMaxNum()), chineseFont));
            table.addCell(createTableCell(String.valueOf(record.getMarginNum()), chineseFont));
        }

            document.add(table);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_PDF);
        httpHeaders.setContentDispositionFormData("attachment", "margin.pdf");
        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(outputStream.toByteArray());
    }

    @Override
    public ResponseEntity<byte[]> marginExcel(List<String> productNames) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("仓容余量");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(35);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("发苗机仓容余量");
        Cell dateCell = titleRow.createCell(5);
        dateCell.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleCell.setCellStyle(titleStyle);

        CellStyle dateStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font dateFont = workbook.createFont();
        dateFont.setBold(false);
        dateFont.setFontHeightInPoints((short) 12);
        dateFont.setColor(IndexedColors.WHITE.getIndex());
        dateStyle.setFont(dateFont);
        dateStyle.setAlignment(HorizontalAlignment.RIGHT);
        dateStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dateStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        dateCell.setCellStyle(dateStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.LEFT);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);
        String[] tableHeaders = {"产品名称", "产品编码", "仓柜规格", "空余仓位数量", "单仓可上最大数量", "最大可上药量"};
        for (int i = 0; i < tableHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.LEFT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);

        List<MarginResponse> dataList = listMarginData(null, productNames);
        int rowNum = 2;
        for (MarginResponse record : dataList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            setCell(row, 0, record.getProductName(), dataStyle);
            setCell(row, 1, record.getProductNo(), dataStyle);
            setCell(row, 2, record.getBoxSpecName(), dataStyle);
            setNumericCell(row, 3, record.getEmptyBoxNum(), numberStyle);
            setNumericCell(row, 4, record.getBoxMaxNum(), numberStyle);
            setNumericCell(row, 5, record.getMarginNum(), numberStyle);
        }

        int[] columnWidths = {25, 22, 20, 16, 18, 18};
        for (int i = 0; i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i] * 256);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);

        String fileName = URLEncoder.encode("发苗机仓容余量_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .headers(headers)
                .body(out.toByteArray());
        } finally {
            workbook.close();
        }
    }

    private void setCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int index, Integer value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : 0);
        cell.setCellStyle(style);
    }

    private Font loadChineseFont(String path) throws IOException, DocumentException {
        return PdfFontLoader.load(path);
    }

    private PdfPCell createTableCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(content == null ? "" : content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }
}
