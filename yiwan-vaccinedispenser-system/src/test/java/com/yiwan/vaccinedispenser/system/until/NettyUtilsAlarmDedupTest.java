package com.yiwan.vaccinedispenser.system.until;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NettyUtilsAlarmDedupTest {

    @Test
    void sameAlarmIgnoresFrameNumberAndCrc() {
        String[] first = frame("0B", "03", "1B", "F4");
        String[] second = frame("0C", "03", "B9", "3E");

        assertEquals(
                NettyUtils.StringListToStringWithoutFrame(first),
                NettyUtils.StringListToStringWithoutFrame(second));
        assertEquals("A5 5A 0C 00 01 04 00 03 0D 00 55",
                NettyUtils.StringListToStringWithoutFrame(first));
    }

    @Test
    void differentAlarmPayloadIsNotSuppressed() {
        String[] first = frame("0B", "03", "1B", "F4");
        String[] differentAxis = frame("0C", "04", "A9", "2E");

        assertNotEquals(
                NettyUtils.StringListToStringWithoutFrame(first),
                NettyUtils.StringListToStringWithoutFrame(differentAxis));
    }

    private static String[] frame(String frameLow,
                                  String servoAddress,
                                  String crcLow,
                                  String crcHigh) {
        return new String[]{
                "A5", "5A", "0C", "00", frameLow, "03", "01", "04",
                "00", servoAddress, "0D", "00", crcLow, crcHigh, "55"
        };
    }
}
