package com.yiwan.vaccinedispenser.system.sys.data.response.vac;

import lombok.Data;

/**
 * 仓容余量响应
 *
 * @author codex
 */
@Data
public class MarginResponse {

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编码
     */
    private String productNo;

    /**
     * 仓柜规格
     */
    private String boxSpecName;

    /**
     * 空余仓位数量
     */
    private Integer emptyBoxNum;

    /**
     * 单仓可上最大数量
     */
    private Integer boxMaxNum;

    /**
     * 最大可上药量
     */
    private Integer marginNum;
}
