package com.yiwan.vaccinedispenser.system.until;

import cn.hutool.core.util.HexUtil;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/7 18:55
 */
public class CRC8 {


    private static final int POLY = 0x07;

    public static String calculateCRC(String crc) {
        int crcValue = calculate(HexUtil.decodeHex(crc));
        String result = Integer.toHexString(crcValue & 0xFF).toUpperCase();

        // 如果结果长度小于2，则在前面补零
        while (result.length() < 2) {
            result = "0" + result;
        }

        return result;
    }

    public static int calculate(byte[] bytes) {
        int crc = 0x00;

        for (byte b : bytes) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (crc << 1) ^ POLY;
                } else {
                    crc = (crc << 1);
                }
            }
        }

        return crc;
    }
}
