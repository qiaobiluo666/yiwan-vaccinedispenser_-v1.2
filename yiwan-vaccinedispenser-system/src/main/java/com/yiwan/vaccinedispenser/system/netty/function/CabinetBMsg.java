package com.yiwan.vaccinedispenser.system.netty.function;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/11 11:12
 */
@Slf4j
@Component
public class CabinetBMsg {
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    @Autowired
    private CabinetSysMsg cabinetSysMsg;
    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;


    /**
     *
     * @param bytesStr
     * B柜接收信息
     */

    public  void receiveMsgCabinetB(String[] bytesStr) throws IOException {
        switch (bytesStr[7]) {
            //自动上药
            case "01"->{
                log.info("收到B柜{}:{}",CabinetConstants.CabinetBType.APPLY.desc,NettyUtils.StringListToString(bytesStr));
                receiveApply(bytesStr);
            }
            
            //距离传感器
            case "02"->{
                receiveDistance(bytesStr);
            }


            //伺服电机
            case "03"->{
                receiveServo(bytesStr);
            }


            //步进电机
            case "04"->{
                receiveStep(bytesStr);
            }

            //输出检测
            case "05"->{
                log.info("收到B柜{}:{}",CabinetConstants.CabinetBType.OUTPUT.desc,NettyUtils.StringListToString(bytesStr));
            }


            //输入检测
            case "06"->{
                log.info("收到B柜{}:{}",CabinetConstants.CabinetBType.INPUT.desc,NettyUtils.StringListToString(bytesStr));
                receiveSensor(bytesStr);
            }




            //主动上报
            case "FF"->{
                log.info("收到B柜{}:{}",CabinetConstants.CabinetBType.REPORT.desc,NettyUtils.StringListToString(bytesStr));
                receiveReport(bytesStr);
            }


            //设置系统参数
            case "80" -> {
                log.info("收到B柜{}:{}",CabinetConstants.CabinetSettingType.SET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
            }



            //获取系统参数
            case "81" -> {
                log.info("收到B柜{}:{}",CabinetConstants.CabinetSettingType.GET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
                cabinetSysMsg.receiveMsgCabinetSys(CabinetConstants.Cabinet.CAB_B,bytesStr);
            }
        }
    }



    /**
     *
     * @param bytesStr
     * B柜发药接收信息处理
     */
    public void receiveApply(String[] bytesStr) throws IOException {
        switch (bytesStr[8]){
            //自动上药
            case "01"->{
                switch (bytesStr[9]){
                    case "01"->{
                        //滑台传感器运动结束 开始扫码上药
                        if("01".equals(bytesStr[11])){
                            valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH,"true");
                        }else if("02".equals(bytesStr[11])&&"0A".equals(bytesStr[12])){
                            log.info("没检测到药品，结束自动上药");
                            sendDrugThreadManager.stop();

                        }
                    }

//                    case "02"->{
//                        //滑台传感器运动结束 开始扫码上药
//                        if("0A".equals(bytesStr[11])){
//                            sendDrugThreadManager.stop();
//                        }
//                    }
//
                    
                    
                }
            }

            //平台滑台
            case "02"->{

            }

        }

    }


    /**
     *
     * @param bytesStr
     * B柜获取距离传感器数据
     */

    public void receiveDistance(String[] bytesStr){
        switch (bytesStr[bytesStr.length-6]){
            //读取正常
            case "01" ->{
                BigInteger distance = NettyUtils.parseHexStringArray(bytesStr, 11,4);
                switch (bytesStr[9]){
                    //左边距离传感器
                    case "01"->{
                        log.info("收到B柜{}:左边距离传感器:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.LEFT, String.valueOf(distance));
                        log.info(String.valueOf(distance));
                        valueOperations.set(RedisKeyConstant.distanceStart.LEFT,"true");

                    }
                    //右边距离传感器
                    case "02"->{
                        log.info("收到B柜{}:右边距离传感器:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.RIGHT, String.valueOf(distance));
                        log.info(String.valueOf(distance));
                        valueOperations.set(RedisKeyConstant.distanceStart.RIGHT,"true");
                    }

                    //上方距离传感器
                    case "03"->{
                        log.info("收到B柜{}:上方距离传感器:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.HIGH, String.valueOf(distance));
                        VacUntil.sleep(50);
                        log.info(String.valueOf(distance));
                        valueOperations.set(RedisKeyConstant.distanceStart.HIGH,"true");
                    }
                }


            }

            //读取异常
            case "02"->{
                switch (bytesStr[9]){
                    //左边距离传感器
                    case "01"->{
                        log.error("收到B柜{}:左边距离传感器异常:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.LEFT, "ERROR");
                        valueOperations.set(RedisKeyConstant.distanceStart.LEFT,"true");

                    }

                    //右边距离传感器
                    case "02"->{
                        log.error("收到B柜{}:右边距离传感器异常:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.RIGHT, "ERROR");
                        valueOperations.set(RedisKeyConstant.distanceStart.RIGHT,"true");
                    }


                    //上方距离传感器
                    case "03"->{
                        log.error("收到B柜{}:右边距离传感器异常:{}",CabinetConstants.CabinetBType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.HIGH, "7");
                        valueOperations.set(RedisKeyConstant.distanceStart.HIGH,"true");
                    }

                }
            }
        }
    }







    /**
     *
     * @param bytesStr
     * B柜伺服信息处理
     */
    public void receiveServo(String[] bytesStr) throws IOException {





        //第几个伺服
        int address = Integer.parseInt(bytesStr[9], 16);
        switch (address){
            case 1 -> log.info("收:B柜{}:机械手X轴：{}",CabinetConstants.CabinetBType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 2 -> log.info("收:B柜{}:机械手Y轴：{}",CabinetConstants.CabinetBType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 3 -> log.info("收:B柜{}:机械手Z轴：{}",CabinetConstants.CabinetBType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 4 -> log.info("收:B柜{}:滑台伺服：{}",CabinetConstants.CabinetBType.SERVO.desc,NettyUtils.StringListToString(bytesStr));
            
            default -> log.info("收:B柜{}:第{}伺服指令：{}",CabinetConstants.CabinetBType.SERVO.desc,address,NettyUtils.StringListToString(bytesStr));
        }

        //获取当前距离
        if("01".equals(bytesStr[10])){
            BigInteger distance = NettyUtils.parseHexStringArray(bytesStr, 11,4);
            valueOperations.set(RedisKeyConstant.servoGetDistance.CABINET_B, String.valueOf(distance));
        }

        //B柜伺服报警 自动上药停止
        if (("03".equals(bytesStr[11])&&"03".equals(bytesStr[7])&&"0A".equals(bytesStr[6]))|| ("02".equals(bytesStr[11])&&"0D".equals(bytesStr[12]))){
            valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_ERROR,"true");
            String msg = "自动上药伺服报警，结束自动上药";
            log.error(msg);
            sendDrugThreadManager.stop();
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code, msg);
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
            commandData.put("data", msg);
            websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);

        }

        switch (bytesStr[9]){
            //扫码X伺服
            case "01" ->{
                switch (bytesStr[11]){
                    case "01"->{
                        valueOperations.set(RedisKeyConstant.scanServo.X,"true");
                    }
                }

            }
            //扫码Y伺服
            case "02" ->{
                switch (bytesStr[11]){
                    case "01"->{
                        valueOperations.set(RedisKeyConstant.scanServo.Y,"true");
                    }
                }

            }
            //扫码Z伺服
            case "03" ->{
                switch (bytesStr[11]){
                    case "01"->{
                        valueOperations.set(RedisKeyConstant.scanServo.Z,"true");
                    }
                }

            }
        }


    }




    /**
     *
     * @param bytesStr
     * B柜伺服信息处理
     */
    public void receiveStep(String[] bytesStr){
        //第几个步进
        int address = Integer.parseInt(bytesStr[9], 16);
        switch (address){
            case 1-> log.info("收:B柜{}:旋转步进:{}",CabinetConstants.CabinetAType.STEP.desc,NettyUtils.StringListToString(bytesStr));
            default -> log.info("收:B柜{}:第{}步进:{}",CabinetConstants.CabinetAType.STEP.desc,address,NettyUtils.StringListToString(bytesStr));
        }
        switch (bytesStr[8]){
            //位置模式
            case "01"->{
                switch (bytesStr[9]){
                    //旋转步进
                    case "01" ->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"true");
                            }
                            case "02"-> valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"error");

                        }

                    }
                }

            }
            //原点模式
            case "03"->{
                switch (bytesStr[9]){
                    //旋转步进
                    case "01" ->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"true");
                            }
                        }

                    }

                    case "02"->{}

                }
            }


        }



    }





    /**
     *
     * @param bytesStr
     * B柜输入检测接收信号处理
     */
    public void receiveSensor(String[] bytesStr){

//        switch (bytesStr[11]) {
//            //触发
//            case "01"-> {
//                log.info("B柜传感器---触发");
//                valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"true");
//            }
//            //不触发
//            case "02"->  {
//                log.info("B柜传感器---未触发");
//                valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
//            }
//        }

        switch (bytesStr[9]) {
            //查询所有传感器状态
            case "00"->{
                List<Integer> sensorList = NettyUtils.allInPut(bytesStr);
                for (Integer integer : sensorList) {
                    if (integer == 1) {
                        valueOperations.set(RedisKeyConstant.sensor.SENSOR_CABINET_B, sensorList.toString());
                    } else {
                        valueOperations.set(RedisKeyConstant.sensor.SENSOR_CABINET_B, sensorList.toString());
                    }
                }
            }


            //滑台传感器  18
            case "12"->{
                switch (bytesStr[11]) {
                    //触发
                    case "01"-> {
                        log.info("B柜:滑台传感器---触发");
                        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"true");
                        String count = valueOperations.get(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT);
                        if(count!=null){
                            valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT,String.valueOf(Integer.parseInt(count)+1));
                        }else {
                            valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT,"1");
                        }



                    }

                    //不触发
                    case "02"->  {
                        log.info("B柜:滑台传感器---未触发");
                        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
                    }
                }
            }

        }



    }

    /**
     *
     * @param bytesStr
     * B柜主动上报信号处理
     */
    public void receiveReport(String[] bytesStr) throws IOException {


    switch (bytesStr[11]) {
        //0x01 - 药满
        case "01"->  {}
        //0x02 - 未有药
        case "02"->  {}

        //0x03 - 未有药超时自动结束
        case "03"->{
            log.info("没检测到药品，结束自动上药");
            sendDrugThreadManager.stop();
        }

        case "10"->valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"true");
    }




    }


}
