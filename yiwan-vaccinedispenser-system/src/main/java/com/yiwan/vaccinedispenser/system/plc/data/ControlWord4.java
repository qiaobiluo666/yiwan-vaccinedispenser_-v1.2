package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 控制字4 位定义 (地址3)
 * <p>
 * 每位对应 C 柜的一个控制功能，写入对应位后 PLC 处理完成自动清 0
 *
 * @author yiwan
 */
@Getter
public enum ControlWord4 {

    C_SEND_DRUG_ACK(0, "cSendDrugAck", "C柜_送药完成应答", 0x0001, "PLC收到后由PLC写0"),
    C_SMASH_DOOR_OPEN(1, "cSmashDoorOpen", "C柜_砸门开", 0x0002, "PLC收到后由PLC写0"),
    C_SMASH_DOOR_CLOSE(2, "cSmashDoorClose", "C柜_砸门关", 0x0004, "PLC收到后由PLC写0");

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

    ControlWord4(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
