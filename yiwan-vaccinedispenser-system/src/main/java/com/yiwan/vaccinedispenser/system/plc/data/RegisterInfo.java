package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Data;

/**
 * 寄存器解析结果
 */
@Data
public class RegisterInfo {

    /** 寄存器地址 */
    private int address;
    /** 参数名 */
    private String paramName;
    /** 中文描述 */
    private String chineseDesc;
    /** 原始整型值 */
    private int rawValue;
    /** 实际展示值 (单位换算后) */
    private String actualValue;
    /** 单位 */
    private String unit;
    /** 数据长度 (寄存器个数,条码为25) */
    private int dataLength;
    /** 是否为条码类型 */
    private boolean barcode;
    /** 条码ASCII字符串 */
    private String barcodeValue;
    /** 寄存器元数据枚举 (可直接用于 switch 跳转) */
    private PlcRegisterTable meta;

    public RegisterInfo() {
    }
}
