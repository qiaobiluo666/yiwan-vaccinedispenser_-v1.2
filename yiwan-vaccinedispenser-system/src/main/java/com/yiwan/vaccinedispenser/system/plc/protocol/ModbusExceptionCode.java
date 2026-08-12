package com.yiwan.vaccinedispenser.system.plc.protocol;

public enum ModbusExceptionCode {

    ILLEGAL_FUNCTION(0x01, "非法功能码"),
    ILLEGAL_DATA_ADDRESS(0x02, "非法数据地址"),
    ILLEGAL_DATA_VALUE(0x03, "非法数据值"),
    SLAVE_DEVICE_FAILURE(0x04, "从站设备故障"),
    ACKNOWLEDGE(0x05, "确认"),
    SLAVE_DEVICE_BUSY(0x06, "从站设备忙"),
    NEGATIVE_ACKNOWLEDGE(0x07, "否定确认"),
    MEMORY_PARITY_ERROR(0x08, "内存奇偶校验错误"),
    GATEWAY_PATH_UNAVAILABLE(0x0A, "网关路径不可用"),
    GATEWAY_TARGET_NO_RESPONSE(0x0B, "网关目标无响应");

    private final int code;
    private final String description;

    ModbusExceptionCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ModbusExceptionCode fromCode(int code) {
        for (ModbusExceptionCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return null;
    }
}
