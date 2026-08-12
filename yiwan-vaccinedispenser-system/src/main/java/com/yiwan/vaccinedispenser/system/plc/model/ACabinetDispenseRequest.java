package com.yiwan.vaccinedispenser.system.plc.model;

import lombok.Data;

/**
 * A柜发药请求 (上位机->PLC)
 * <p>
 * 寄存器地址: 20~25
 * 功能说明: 指定A柜发药的来源仓位、目标工位及相关参数
 */
@Data
public class ACabinetDispenseRequest {

    /** 地址20 - A柜来源层 (1-10) */
    private Integer lineNum;

    /** 地址21 - A柜来源序号 (1-24) */
    private Integer positionNum;

    /** 地址22 - A柜目标工位 (1-6) */
    private Integer workbenchNum;

    /** 地址23 - A柜电磁铁时间 (单位:10ms) */
    private Integer ioTime;

    /** 地址24 - A柜步进伸出距离 (机械手出药模式使用, 单位:mm) */
    private Integer stepExtendDistance;

    /** 地址25 - A柜步进升高距离 (机械手出药模式使用, 单位:mm) */
    private Integer stepLiftDistance;

}
