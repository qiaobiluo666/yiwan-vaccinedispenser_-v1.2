package com.yiwan.vaccinedispenser.system.until;

import cn.hutool.core.util.HexUtil;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/7 17:17
 */
public class CRC16Modbus {

    private static final int POLY = 0xA001;


    public static String calculateCRC(String crc) {
        int crcValue = calculate(HexUtil.decodeHex(crc));
        String result = Integer.toHexString(crcValue).toUpperCase();

        // 如果结果长度小于4，则在前面补零
        while (result.length() < 4) {
            result = "0" + result;
        }
        result = result.substring(2, 4) + result.substring(0, 2);
        return result;
    }

    public static int calculate(byte[] bytes) {
        int crc = 0xFFFF;

        for (byte b : bytes) {
            crc ^= b & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= POLY;
                } else {
                    crc >>= 1;
                }
            }
        }

        return crc;
    }
}

