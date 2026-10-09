package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MarginRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.MarginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 仓容余量
 *
 * @author codex
 */
@RestController
@Slf4j
public class MarginController {

    private final MarginService marginService;

    public MarginController(MarginService marginService) {
        this.marginService = marginService;
    }

    /**
     * 仓容余量分页查询
     */
    @PostMapping("/margin/list")
    public Result marginList(@RequestBody @Validated MarginRequest request) {
        log.info("入参-MarginRequest:{}", request);
        return Result.success(marginService.marginList(request));
    }

    /**
     * 导出仓容余量 PDF
     */
    @GetMapping("/pdf/margin")
    public ResponseEntity<byte[]> marginPdf(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) List<String> productNames) throws Exception {
        return marginService.marginPdf(resolveProductNames(productName, productNames));
    }

    /**
     * 导出仓容余量 Excel
     */
    @GetMapping("/excel/margin")
    public ResponseEntity<byte[]> marginExcel(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) List<String> productNames) throws Exception {
        return marginService.marginExcel(resolveProductNames(productName, productNames));
    }

    private List<String> resolveProductNames(String productName, List<String> productNames) {
        if (productNames != null && !productNames.isEmpty()) {
            return productNames;
        }
        return StringUtils.hasText(productName)
                ? Collections.singletonList(productName)
                : Collections.emptyList();
    }
}
