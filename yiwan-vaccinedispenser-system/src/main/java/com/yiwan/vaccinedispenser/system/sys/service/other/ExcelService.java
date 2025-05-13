package com.yiwan.vaccinedispenser.system.sys.service.other;

import com.itextpdf.text.DocumentException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Date;

/**
 * @author 78671
 */
public interface ExcelService {


    //库存管理导出Excel
    ResponseEntity<byte[]> getInventoryExcel(String productName) throws DocumentException, IOException;



    //库存管理详情导出Excel
    ResponseEntity<byte[]> getInventoryDetailExcel(String productName) throws DocumentException, IOException;



    //发药导出
    ResponseEntity<byte[]> getSendDrugExcel(Date createTimeStart, Date createTimeEnd ,String workbenchName) throws  IOException;


    //发药详情导出
    ResponseEntity<byte[]> getSendDrugDetailExcel(Date createTimeStart, Date createTimeEnd ,String workbenchName) throws  IOException;



}
