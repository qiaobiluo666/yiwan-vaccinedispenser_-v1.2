package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 控制字1 位定义 (地址0)
 * <p>
 * 每位对应一个控制功能，写入对应位后PLC处理完成自动清0
 * 十六进制掩码直接用 shift 计算，清晰可读
 *
 * @author yiwan
 */
@Getter
public enum ControlWord1 {

    HEARTBEAT(0, "heartbeat", "上位机心跳", 0x0001, "1s写0 1s写1"),
    B_FEED_START(1, "bFeedStart", "B柜_上苗启动", 0x0002, "PLC收到后由PLC写0"),
    B_FEED_STOP(2, "bFeedStop", "B柜_上苗停止", 0x0004, "PLC收到后由PLC写0"),
    ALARM_CLEAR(3, "alarmClear", "报警清除", 0x0008, "PLC收到后由PLC写0");

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

    ControlWord1(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
