package com.yiwan.vaccinedispenser.system.sys.service.other;

import com.itextpdf.text.DocumentException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Date;

/**
 * @author 78671
 */
public interface PdfService {

    ResponseEntity<byte[]> getSendDrugPdf(Date createTimeStart, Date createTimeEnd,String workbenchName) throws DocumentException, IOException;

    ResponseEntity<byte[]> getSendDrugDetailPdf(Date createTimeStart, Date createTimeEnd,String workbenchName) throws DocumentException, IOException;

    ResponseEntity<byte[]> getVaccineListPdf() throws DocumentException, IOException;

    ResponseEntity<byte[]>  getInventoryPdf(String productName) throws DocumentException, IOException;


    ResponseEntity<byte[]>  getInventoryPdfDetail(String productName) throws DocumentException, IOException;



}
