package com.yiwan.vaccinedispenser.system.plc.protocol;

import lombok.Data;

@Data
public class ModbusFrame {

    private MbapHeader header;
    private int functionCode;
    private byte[] data;
    private boolean exception;

    public ModbusFrame() {
        this.header = new MbapHeader();
    }

    public ModbusFrame(MbapHeader header, int functionCode, byte[] data) {
        this.header = header;
        this.functionCode = functionCode;
        this.data = data;
        this.exception = FunctionCode.isExceptionCode(functionCode);
    }

    public int getActualFunctionCode() {
        if (exception) {
            return functionCode & 0x7F;
        }
        return functionCode;
    }

    public ModbusExceptionCode getExceptionCode() {
        if (!exception || data == null || data.length < 1) {
            return null;
        }
        return ModbusExceptionCode.fromCode(data[0] & 0xFF);
    }
}
