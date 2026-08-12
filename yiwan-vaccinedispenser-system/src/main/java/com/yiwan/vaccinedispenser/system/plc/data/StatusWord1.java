package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 状态字1 位定义 (地址200)
 * <p>
 * PLC 通过对应位通知上位机当前系统状态
 *
 * @author yiwan
 */
@Getter
public enum StatusWord1 {

    PLC_HEARTBEAT(0, "plcHeartbeat", "PLC心跳", 0x0001, "1s写0 1s写1"),
    SYS_INIT_DONE(1, "sysInitDone", "系统初始化完成", 0x0002, "伺服使能完成写1"),
    FEED_RUNNING(2, "feedRunning", "上苗运行中", 0x0004, "1=运行中, 0=停止"),
    SYS_ALARM(3, "sysAlarm", "系统报警", 0x0008, "");

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

    StatusWord1(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
