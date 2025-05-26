package com.yiwan.vaccinedispenser.system.until;

import cn.hutool.core.util.HexUtil;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.LedRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author slh
 * @date 2024/2/21 0021 9:40
 * netty的方法类
 */
@Slf4j
public class NettyUtils {

    /**
     *
     * @param dataLength
     * @return
     * 请求头和数据长度封装
     */
    public static StringBuilder toHandler(Integer dataLength) {

        //创建字符串操作类
        StringBuilder stringBuilder = new StringBuilder();
        //加入包头
        stringBuilder.append(Integer.toHexString(CabinetConstants.PREFIX_FIRST));
        //加入包头2
        stringBuilder.append(Integer.toHexString(CabinetConstants.PREFIX_SECOND));
        //加入数据长度 固定
        stringBuilder.append(intToHexString(dataLength,2));



        return stringBuilder;

    }


    //一个IO拓展板选中哪几个io 变为1
    // 将输出位置列表转换为一个40位的二进制字符串，并将其转换为5个10进制数值的subString列表
    public static StringBuilder IOExpand(List<Integer> positions) {

        // 创建一个长度为40的字符数组，用于存储二进制字符串
        char[] binaryChars = new char[40];
        // 将所有字符初始化为 '0'
        for (int i = 0; i < binaryChars.length; i++) {
            binaryChars[i] = '0';
        }

        // 将列表中的位置号对应的位置设置为 '1'
        for (int position : positions) {
            // 确保位置号在合法范围内
            if (position >= 1 && position <= 40) {
                binaryChars[position - 1] = '1';
            }
        }

        // 创建一个 StringBuilder 用于存储结果
        StringBuilder x = new StringBuilder();

        // 将二进制字符串转换为五个十六进制数
        for (int i = 0; i < 5; i++) {
            // 获取每个字节的二进制字符串
            String byteBinary = new String(binaryChars, i * 8, 8);
            // 将二进制字符串转换为十六进制并添加到结果中
            x.append(String.format("%02X", Integer.parseInt(byteBinary, 2)));
        }

        // 按照字节进行分割，并将每个字节转换为10进制数值的subString
        StringBuilder result = new StringBuilder();
        result.append(x);
        return result;

    }



//    // 将LED状态数组编码为16进制字符串
//    public static StringBuilder ledExpand(LedRequest ledRequest) {
//        String[] ledStatuses = new String[20];
//        Arrays.fill(ledStatuses, "00");
//
//        //常亮
//        if(ledRequest.getLightList()!=null){
//            List<Integer> list = ledRequest.getLightList();
//            for(int i=0;i<ledRequest.getLightList().size();i++){
//                ledStatuses[list.get(i)-1] = CabinetConstants.LedType.LIGHT.code;
//            }
//        }
//
//        //长闪
//        if(ledRequest.getLongList()!=null){
//            List<Integer> list = ledRequest.getLongList();
//            for(int i=0;i<ledRequest.getLongList().size();i++){
//                ledStatuses[list.get(i)-1] = CabinetConstants.LedType.LONG.code;
//            }
//        }
//
//        //短闪
//        if(ledRequest.getShortList()!=null){
//            List<Integer> list = ledRequest.getShortList();
//            for(int i=0;i<ledRequest.getShortList().size();i++){
//                ledStatuses[list.get(i)-1] = CabinetConstants.LedType.SHORT.code;
//            }
//        }
//
//        StringBuilder hexBuilder = new StringBuilder();
//        // 处理20个状态，每次处理两个，共处理10次
//        for (int i = 0; i < ledStatuses.length; i += 2) {
//            int led1Status = encodeSingleLed(ledStatuses[i]);
//            int led2Status = encodeSingleLed(ledStatuses[i + 1]);
//            // 将两个LED的状态合并为一个字节
//            int combinedStatus = (led1Status << 4) | led2Status;
//
//            // 将字节转换为16进制字符串，并添加到StringBuilder
//            hexBuilder.append(String.format("%02X", combinedStatus));
//
//        }
//        return hexBuilder;
//    }

//    // 将单个LED状态编码为整数
//    private static int encodeSingleLed(String ledStatus) {
//        return switch (ledStatus) {
//            case "00" -> 0;
//            case "01" -> 1;
//            case "10" -> 2;
//            case "11" -> 3;
//            default -> 0;
//        };
//    }



    /**
     *
     * @param number
     * @param len 几个字节 len为几
     * @return
     * 将 int 转换为指定长度的带有0填充的十六进制字符串（低位在前，高位在后）
     */
    public static String intToHexString(int number, int len) {
        StringBuilder hexBuilder = new StringBuilder();

        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString((number >> (i * 8)) & 0xFF);
            hexBuilder.insert(0, hex.length() < 2 ? "0" + hex : hex);
        }

        // 反转生成的十六进制字符串的字节顺序
        StringBuilder reversedHex = new StringBuilder();
        for (int i = 0; i < hexBuilder.length(); i += 2) {
            String byteStr = hexBuilder.substring(i, i + 2);
            reversedHex.insert(0, byteStr);
        }
        return reversedHex.toString().toUpperCase();
    }


    /**
     *
     * @param number
     * @param len 几个字节 len为几
     * @return
     * 将 BigInteger 转换为指定长度的带有0填充的十六进制字符串（低位在前，高位在后）
     */
    public static String intToHexString(BigInteger number, int len) {
        // 将 BigInteger 转换为十六进制字符串
        String hexString = number.toString(16);
        // 计算需要添加的 0 的个数
        int paddingZeros = len * 2 - hexString.length();
        // 在左侧填充 0，直到达到指定长度
        for (int i = 0; i < paddingZeros; i++) {
            hexString = "0" + hexString;
        }
        // 将字符串分割为每两个字符的字符串数组
        String[] hexArray = hexString.split("(?<=\\G.{2})");
        // 将字符串数组反转并重新组合为字符串
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = hexArray.length - 1; i >= 0; i--) {
            resultBuilder.append(hexArray[i]);
        }
        return resultBuilder.toString().toUpperCase();
    }










    /**
     *
     * @param arr
     * @param start 第几个字节开始
     * @param length 几个字节组成
     * @return
     * 16进制转化为10进制
     */
    public static BigInteger parseHexStringArray(String[] arr, int start, int length) {
        // 构建一个新的16进制字符串
        StringBuilder hexBuilder = new StringBuilder();
        for (int i = start+length-1; i >= start; i--) {
            hexBuilder.append(arr[i]);
        }
        String reversedHexString = hexBuilder.toString();

        // 将反转后的16进制字符串转换为整数
        return new BigInteger(reversedHexString, 16);
    }

    /**
     *
     * @param arr
     * @param start 第几个字节开始
     * @param length 几个字节组成
     * @return
     * 16进制转化为10进制
     */
    public static int parseHexStringArrayInt(String[] arr, int start, int length) {
        // 构建一个新的16进制字符串
        StringBuilder hexBuilder = new StringBuilder();
        for (int i = start+length-1; i >= start; i--) {
            hexBuilder.append(arr[i]);
        }
        String reversedHexString = hexBuilder.toString();

        // 将反转后的16进制字符串转换为整数
        return Integer.parseInt(reversedHexString,16);
    }



    /**
     *
     * @param array
     * @return
     * 字符串拼接
     */

    public static String StringListToString(String[] array) {
        StringBuilder sb = new StringBuilder();

        // 遍历数组并拼接字符串
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }




    /**
     * 字符串进行格式化
     *
     * @param cmdByte 原先字符串
     * @return
     */
    public static String getFormatHexStr(byte[] cmdByte) {
        String encodeHexStr = HexUtil.encodeHexStr(cmdByte);
        return HexUtil.format(encodeHexStr);
    }


    /**
     * 把byte数组转成ByteBuf
     *
     * @param bytes
     * @return
     */
    public static ByteBuf encodeByteBuf(byte[] bytes) {
        ByteBuf buf = Unpooled.buffer(bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }


    /**
     * ByteBuf 转成byte数组
     *
     * @param sendStr
     * @return
     */
    public static byte[] decodeByteBuf(ByteBuf sendStr) {
        byte[] content = new byte[sendStr.readableBytes()];
        sendStr.getBytes(0, content);

        return content;
    }

    /**
     *
     * @param bytesStr
     * @return
     * 拿出所有传感器是否输入输出信号
     */
    public static List<Integer> allInPut(String[] bytesStr){

        StringBuilder sb = new StringBuilder();

        for (int i = 11; i <= 14; i++) {
            sb.append(bytesStr[i]);
        }

        List<Integer> binaryList = new ArrayList<>(32);
        for (int i = 0; i < sb.length(); i++) {
            int decimal = Integer.parseInt(String.valueOf(sb.charAt(i)), 16);
            for (int j = 3; j >= 0; j--) {
                binaryList.add((decimal >> j) & 1);
            }
        }


        return binaryList;
    }

    /**
     *
     * @param bytesStr
     * @return
     * 拿出所有传感器是否输入输出信号
     */
    public static List<Integer> allIo(String[] bytesStr){
        StringBuilder sb = new StringBuilder();
        for (int i = 11; i <= 15; i++) {
            sb.append(bytesStr[i]);
        }
        List<Integer> binaryList = new ArrayList<>(40);
        for (int i = 0; i < sb.length(); i++) {
            int decimal = Integer.parseInt(String.valueOf(sb.charAt(i)), 16);
            for (int j = 3; j >= 0; j--) {
                binaryList.add((decimal >> j) & 1);
            }
        }





        return binaryList;
    }
}
