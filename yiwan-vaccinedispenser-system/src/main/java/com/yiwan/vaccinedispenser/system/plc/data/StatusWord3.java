package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 状态字3 位定义 (地址202)
 * <p>
 * PLC 通过对应位通知上位机当前 A 柜任务状态
 *
 * @author yiwan
 */
@Getter
public enum StatusWord3 {

    A_TASK_RECEIVED(0, "aTaskReceived", "A柜_收到任务", 0x0001, ""),
    A_INVENTORY_DONE(1, "aInventoryDone", "A柜_盘苗完成", 0x0002, ""),
    A_FILL_DONE(3, "aFillDone", "A柜_进药完成", 0x0008, ""),
    A_RETURN_DONE(4, "aReturnDone", "A柜_退苗完成", 0x0010, "状态为1时上位机清除地址60"),
    A_RETURN_ALLOWED(6, "aReturnAllowed", "A柜允许退苗", 0x0040, ""),
    A_RETURN_RUNNING(7, "aReturnRunning", "A柜退苗运行中", 0x0080, ""),
    A_ABNORMAL_CLEAN_ALLOWED(11, "aAbnormalCleanAllowed", "A柜_允许异常清苗", 0x0800, ""),
    A_ABNORMAL_CLEAN_RUNNING(12, "aAbnormalCleanRunning", "A柜_异常清苗运行中", 0x1000, ""),
    A_ABNORMAL_CLEAN_DONE(13, "aAbnormalCleanDone", "A柜_异常清苗完成", 0x2000, ""),
    A_ABNORMAL_CLEAN_OUTPUT_ERROR(14, "aAbnormalCleanOutputError", "A柜_异常清苗出苗异常", 0x4000, "");

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

    StatusWord3(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
