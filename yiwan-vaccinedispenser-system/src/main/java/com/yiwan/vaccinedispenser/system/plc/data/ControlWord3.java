package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 控制字3 位定义 (地址2)
 * <p>
 * 每位对应 A 柜的一个控制功能，写入对应位后 PLC 处理完成自动清 0
 *
 * @author yiwan
 */
@Getter
public enum ControlWord3 {

    A_DISPENSE_CMD(0, "aDispenseCmd", "A柜_出药命令下发", 0x0001, "A柜_收到任务为0时上位机才能写1；为1时上位机写0"),
    A_INVENTORY_START(1, "aInventoryStart", "A柜_盘苗启动", 0x0002, "A柜_盘苗完成为0时才能写1；为1时上位机写0"),
    A_MANUAL_FEED(2, "aManualFeed", "A柜_手动上苗", 0x0004, "PLC收到后由PLC写0"),
    A_FILL_ACK(3, "aFillAck", "A柜_进药完成应答", 0x0008, "PLC收到后由PLC写0"),
    A_BELT_ALARM_ACK(4, "aBeltAlarmAck", "A柜到小皮带异常确认", 0x0010, "PLC收到后由PLC写0");

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

    ControlWord3(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
