package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 仓容余量查询请求
 *
 * @author codex
 */
@Data
public class MarginRequest {

    /**
     * 产品名称（任意字匹配）
     */
    private String productName;

    /**
     * 产品名称（多选精确匹配）
     */
    private List<String> productNames;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 200, message = "每页数量不能大于 200")
    private Integer size = 10;
}
