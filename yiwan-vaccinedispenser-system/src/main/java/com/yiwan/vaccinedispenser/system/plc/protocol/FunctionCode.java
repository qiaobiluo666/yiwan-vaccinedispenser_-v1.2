package com.yiwan.vaccinedispenser.system.plc.protocol;

public enum FunctionCode {

    READ_COILS(0x01, "读线圈状态"),
    READ_DISCRETE_INPUTS(0x02, "读离散输入状态"),
    READ_HOLDING_REGISTERS(0x03, "读保持寄存器"),
    READ_INPUT_REGISTERS(0x04, "读输入寄存器"),
    WRITE_SINGLE_COIL(0x05, "写单个线圈"),
    WRITE_SINGLE_REGISTER(0x06, "写单个保持寄存器"),
    WRITE_MULTIPLE_COILS(0x0F, "写多个线圈"),
    WRITE_MULTIPLE_REGISTERS(0x10, "写多个保持寄存器");

    private final int code;
    private final String description;

    FunctionCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FunctionCode fromCode(int code) {
        for (FunctionCode fc : values()) {
            if (fc.code == code) {
                return fc;
            }
        }
        return null;
    }

    public static boolean isExceptionCode(int code) {
        return (code & 0x80) != 0;
    }

    public static FunctionCode fromExceptionCode(int exceptionCode) {
        return fromCode(exceptionCode & 0x7F);
    }
}
