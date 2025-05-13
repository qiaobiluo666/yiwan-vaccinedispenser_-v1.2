package com.yiwan.vaccinedispenser.system.com;

import cn.hutool.core.util.HexUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/21 9:12
 */
@Slf4j
@Service
public class ComService {

    private final Map<String, SerialPort> connectedPorts  = new HashMap<>();

    @Autowired
    private ComPortConfig comPortConfig;

    @Autowired
    private WebsocketService websocketService;

//    @Autowired
//    public ComService(ComPortConfig comPortConfig) {
//        this.comPortConfig = comPortConfig;
//    }


//    @PostConstruct
//    public void initialize() throws IOException {
//        if(comPortConfig.isEnabled()){
//            connectSerialPort(comPortConfig.getCom1());
//            connectSerialPort(comPortConfig.getCom2());
//            connectSerialPort(comPortConfig.getCom3());
//        }
//
//    }

    private void connectSerialPort(String portDescriptor) throws IOException {


        Map<String, Object> commandData = new HashMap<>();
        if(comPortConfig.getCom1().equals(portDescriptor)){
            commandData.put("code", CommandEnums.DEVICE_STATUS_B_DISTANCE_LEFT.getCode());
        }else if(comPortConfig.getCom2().equals(portDescriptor)) {
            commandData.put("code", CommandEnums.DEVICE_STATUS_B_DISTANCE_RIGHT.getCode());
        }else {
            commandData.put("code", CommandEnums.DEVICE_STATUS_B_DISTANCE_HIGH.getCode());
        }
        SerialPort serialPort = SerialPort.getCommPort(portDescriptor);
        serialPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        if (!serialPort.openPort()) {
            commandData.put("data", "fail");
           log.info("连接串口：{}失败！",portDescriptor);

        }else {
            commandData.put("data", "sucess");
            log.info("连接串口：{}成功!",portDescriptor);
        }
        websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
        connectedPorts.put(portDescriptor, serialPort);
    }


    public CompletableFuture<String> sendMessage(String comPortName, String message,Boolean flag) {
        return CompletableFuture.supplyAsync(() -> {
            SerialPort comPort = connectedPorts.get(comPortName);
            if (comPort == null || !comPort.openPort()) {
                log.error("连接COM：{}失败！",comPortName);
                assert comPort != null;
                comPort.closePort();
                log.info("正在重新连接COM:{}",comPortName);
                try {
                    connectSerialPort(comPortName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "false";
            }

            byte[] messageBytes = HexUtil.decodeHex(message);
            comPort.writeBytes(messageBytes, messageBytes.length);

            byte[] readBuffer = new byte[1024];
            int numRead = comPort.readBytes(readBuffer, readBuffer.length);

            if(flag){
                return bytesToDistanceHeight(readBuffer, numRead);
            }else {

//                return bytesToDistanceHeight(readBuffer, numRead);
                try {

                    return bytesToDistanceASCII(readBuffer, numRead, comPortName);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }


        }).exceptionally(ex -> {
            // 这里处理连接失败或其他异常
            log.error("Communication error on " + comPortName + ": " + ex.getMessage());
            return "Error: " + ex.getMessage();
        });
    }

    /**
     *
     * @param bytes
     * @param length
     * @return
     * 直接返回给的距离
     */
    private String bytesToDistanceHeight(byte[] bytes, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        log.info(String.valueOf(hexString));
        String  reversedHexString = hexString.substring(10,12)+hexString.substring(8,10);
        log.info("高：{}",String.valueOf((int) Math.round(Integer.parseInt(reversedHexString, 16) / 10.0)));
       return String.valueOf((int) Math.round(Integer.parseInt(reversedHexString, 16) / 10.0));


    }


    /**
     *
     * @param bytes
     * @param length
     * @return
     * 直接返回给的距离
     */
    private String bytesToDistanceASCII(byte[] bytes, int length,String comPortName) throws ExecutionException, InterruptedException {

        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
//        return  hexToASCII(hexString.substring(6,20));
        return  hexToASCII(hexString.toString(),comPortName);
    }




    public String hexToASCII(String hexString,String comPortName) throws ExecutionException, InterruptedException {
        if(hexString!=null&& !hexString.isEmpty()){
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < hexString.length(); i += 2) {
                String str = hexString.substring(i, i + 2);
                output.append((char) Integer.parseInt(str, 16));
            }
            log.info("{}:{}",comPortName, output);
            //如果有错误码
            if (output.toString().contains("ERR")) {
                if (output.toString().contains("ERR--10")){
                    return "电量低";
                }else if(output.toString().contains("ERR--14")){
                    return "计算错误";
                } else if(output.toString().contains("ERR--15")){
                    return "超出量程";
                }else if(output.toString().contains("ERR--16")){
                    return "信号弱或者测量时间太长";
                }else if(output.toString().contains("ERR--18")){
                    return "环境光太强";
                }else if(output.toString().contains("ERR--26")){
                    return "超出显示范围";
                }else {
                    return "未知错误";
                }
            } else {


                String[] parts = output.toString().split("\u0006")[1].split("\\.");
                return parts[1].replaceAll("[^0-9]", "");
            }


        }else {

            //读取缓存里的错误代码
            CompletableFuture<String> response1 = sendMessage(comPortName, "80 06 07 73",false);
            CompletableFuture<Void> allDone = CompletableFuture.allOf(response1);
            try {
                // 阻塞直到所有任务完成
                allDone.get();
            } catch (InterruptedException | ExecutionException e) {
                // 处理异常
            }
            return response1.get();
        }



//        return output.toString().split("\\.")[1];

    }

}


