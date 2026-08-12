package com.yiwan.vaccinedispenser.system.plc.model;

import lombok.Data;

/**
 * B柜送药请求 (上位机->PLC)
 * <p>
 * 寄存器地址: 10~11
 * 功能说明: 指定B柜送药的层号和序号
 */
@Data
public class BCabinetSendDrugRequest {

    /** 地址10 - B柜送药请求层 (1-10) */
    private Integer layer;

    /** 地址11 - B柜送药请求序号 (1-24) */
    private Integer index;

}
