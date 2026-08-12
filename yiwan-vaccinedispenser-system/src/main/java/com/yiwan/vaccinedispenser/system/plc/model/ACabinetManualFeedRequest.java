package com.yiwan.vaccinedispenser.system.plc.model;

import lombok.Data;

/**
 * A柜手动上苗请求 (上位机->PLC)
 * <p>
 * 寄存器地址: 15~17
 * 功能说明: 指定A柜手动上苗的层号、序号和药盒宽度
 */
@Data
public class ACabinetManualFeedRequest {

    /** 地址15 - A柜手动上苗层号 (1-10) */
    private Integer layer;

    /** 地址16 - A柜手动上苗序号 (1-24) */
    private Integer index;

    /** 地址17 - A柜手动上苗宽度 (单位:1mm) */
    private Integer width;

}
