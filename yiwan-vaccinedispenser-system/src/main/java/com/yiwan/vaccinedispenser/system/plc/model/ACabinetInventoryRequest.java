package com.yiwan.vaccinedispenser.system.plc.model;

import lombok.Data;

/**
 * A柜盘苗请求 (上位机->PLC)
 * <p>
 * 寄存器地址: 12~14
 * 功能说明: 指定A柜盘苗的层号、序号和偏移量
 */
@Data
public class ACabinetInventoryRequest {

    /** 地址12 - A柜盘苗层号 (1-10) */
    private Integer layer;

    /** 地址13 - A柜盘苗序号 (1-24) */
    private Integer index;

    /** 地址14 - A柜盘苗偏移量 (单位:1mm, 上下仓+-3mm保护) */
    private Integer offset;

}
