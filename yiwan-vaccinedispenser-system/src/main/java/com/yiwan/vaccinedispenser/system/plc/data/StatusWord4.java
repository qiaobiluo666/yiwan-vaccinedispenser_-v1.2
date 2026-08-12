package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 状态字4 位定义 (地址203)
 * <p>
 * PLC 通过对应位通知上位机当前 C 柜任务状态
 *
 * @author yiwan
 */
@Getter
public enum StatusWord4 {

    C_SEND_DRUG_DONE(0, "cSendDrugDone", "C柜_送药完成", 0x0001, ""),
    C_SMASH_DOOR_OPEN_DONE(1, "cSmashDoorOpenDone", "C柜_砸门开完成", 0x0002, ""),
    C_SMASH_DOOR_CLOSE_DONE(2, "cSmashDoorCloseDone", "C柜_砸门关完成", 0x0004, "");

    /** 位序号 (0~15) */
    private final int bit;
    /** 参数名 */
    private final String paramName;
    /** 中文语义 */
    private final String chineseDesc;
    /** 十六进制掩码 */
    private final int mask;
    /** 备注 */
    private final String remark;

    StatusWord4(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
