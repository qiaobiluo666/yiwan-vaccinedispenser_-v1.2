package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 状态字2 位定义 (地址201)
 * <p>
 * PLC 通过对应位通知上位机执行相应操作，上位机处理完成后通过控制字2回写结果
 *
 * @author yiwan
 */
@Getter
public enum StatusWord2 {

    JUDGE_REQUEST(0, "judgeRequest", "长宽厚请求判断", 0x0001, "PLC置1请求判断长宽高"),
    BARCODE_REQUEST(1, "barcodeRequest", "条码判断请求", 0x0002, "PLC置1请求解析条码");

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

    StatusWord2(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
