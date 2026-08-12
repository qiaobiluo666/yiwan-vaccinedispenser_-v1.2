package com.yiwan.vaccinedispenser.system.plc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MbapHeader {

    public static final int MBAP_LENGTH = 7;

    private int transactionId;
    private int protocolId;
    private int length;
    private int unitId;

    public MbapHeader() {
        this.protocolId = 0x0000;
        this.unitId = 0x01;
    }

    public int getPduLength() {
        return length - 1;
    }
}
