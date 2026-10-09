package com.yiwan.vaccinedispenser.system.sys.service.vac;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MarginRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.MarginResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 仓容余量
 *
 * @author codex
 */
public interface MarginService {

    /**
     * 分页查询仓容余量
     */
    Page<MarginResponse> marginList(MarginRequest request);

    /**
     * 导出仓容余量 PDF
     */
    ResponseEntity<byte[]> marginPdf(List<String> productNames) throws Exception;

    /**
     * 导出仓容余量 Excel
     */
    ResponseEntity<byte[]> marginExcel(List<String> productNames) throws Exception;
}
