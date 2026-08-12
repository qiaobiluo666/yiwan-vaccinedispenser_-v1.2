package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * PLC寄存器解析器
 * <p>
 * 字节序规则:
 * 1. 标准数据（状态字、报警字、长度、功能码）: 大端序（高位在前）
 *    bytesToRegisters(): 每2字节按大端序组成一个 int
 * 2. 条码/字符串数据（地址 233、282）: 小端序（低位在前）
 *    registersToAscii(): 每个寄存器先取低字节、再取高字节
 * 3. 去掉末尾 0x00 填充（不可见字符自动过滤）
 *
 * @author yiwan
 */
@Slf4j
public class PlcRegisterReader {

    /**
     * 按起始地址解析寄存器裸数据
     *
     * @param registerBytes 裸寄存器数据 (不含1字节byteCount)
     * @param startAddr     本次数据的起始寄存器地址
     * @return 解析后的寄存器信息列表
     */
    public static List<RegisterInfo> parse(byte[] registerBytes, int startAddr) {
        List<RegisterInfo> result = new ArrayList<>();
        if (registerBytes == null || registerBytes.length < 2) {
            return result;
        }
        int[] registers = bytesToRegisters(registerBytes);

        for (int i = 0; i < registers.length; ) {
            int address = startAddr + i;
            int rawValue = registers[i];

            PlcRegisterTable meta = PlcRegisterTable.findByAddress(address);
            if (meta == null) {
                i++;
                continue;
            }

            RegisterInfo info = new RegisterInfo();
            info.setAddress(address);
            info.setParamName(meta.getParamName());
            info.setChineseDesc(meta.getChineseDesc());
            info.setDataLength(meta.getDataLength());
            info.setUnit(meta.getUnit());
            info.setMeta(meta);

            if (meta.isBarcode()) {
                int barcodeLen = Math.min(meta.getDataLength(), registers.length - i);
                String barcodeStr = registersToAscii(registers, i, barcodeLen);
                info.setBarcode(true);
                info.setBarcodeValue(barcodeStr);
                info.setActualValue(barcodeStr);
                info.setRawValue(0);
                result.add(info);
                i += barcodeLen;
                continue;
            }

            info.setRawValue(rawValue);
            info.setActualValue(convertValue(rawValue, meta.getUnit()));
            result.add(info);
            i++;
        }

        return result;
    }

    /**
     * 单位换算
     */
    private static String convertValue(int raw, String unit) {
        if ("0.1mm".equals(unit)) {
            return String.format("%.1f", raw / 10.0);
        }
        return String.valueOf(raw);
    }

    /**
     * byte[] -> int[] (大端序)
     */
    private static int[] bytesToRegisters(byte[] registerBytes) {
        int count = registerBytes.length / 2;
        int[] registers = new int[count];
        for (int i = 0; i < count; i++) {
            registers[i] = ((registerBytes[i * 2] & 0xFF) << 8) | (registerBytes[i * 2 + 1] & 0xFF);
        }
        return registers;
    }

    /**
     * 条码解析: 小端序（低位在前），去掉末尾 0x00 填充
     * <p>
     * 每个寄存器(2字节)按 低字节→高字节 顺序输出 ASCII 字符
     * 例如寄存器 0x3138 → low=0x38('8') high=0x31('1') → "81"
     */
    private static String registersToAscii(int[] registers, int offset, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int val = registers[offset + i];
            char low = (char) (val & 0xFF);
            char high = (char) ((val >> 8) & 0xFF);
            if (low >= 0x20 && low <= 0x7E) sb.append(low);
            if (high >= 0x20 && high <= 0x7E) sb.append(high);
        }
        String result = sb.toString();
        while (result.endsWith("\0")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.trim();
    }
}
