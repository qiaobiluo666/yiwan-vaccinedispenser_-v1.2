package com.yiwan.vaccinedispenser.system.netty.function;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/11 11:12
 */
@Slf4j
@Component
public class CabinetAMsg {
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private CabinetSysMsg cabinetSysMsg;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;
    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;
    /**
     *
     * @param bytesStr
     * 设置redis 知道该帧号命令下位机已经收到
     */
    public  void receiveMsgFrame(String[] bytesStr, Integer workMode){
        //通过帧号确定 这条命令控制板已经收到
        BigInteger frame = NettyUtils.parseHexStringArray(bytesStr, 4,2);

        String typeKey = String.format(RedisKeyConstant.FRAME_NUMBER_COMMAND, frame);
        valueOperations.set(typeKey, "true", 30, TimeUnit.SECONDS);

        //各个控制板收到00以后才能发送下一条命令
        switch (workMode) {
            case 10 -> valueOperations.set(RedisKeyConstant.CABINET_A_RECEIVE_MSG, "true", 30, TimeUnit.SECONDS);
            case 11 -> valueOperations.set(RedisKeyConstant.CABINET_B_RECEIVE_MSG, "true", 30, TimeUnit.SECONDS);
            case 12 -> valueOperations.set(RedisKeyConstant.CABINET_C_RECEIVE_MSG, "true", 30, TimeUnit.SECONDS);
            default -> {
            }
        }
    }

    /**
     *
     * @param bytesStr
     * A柜接收信息
     */

    public  void receiveMsgCabinetA(String[] bytesStr) throws IOException {
        switch (bytesStr[7]) {
            //IO控制指令
            case "01"->{

                log.info("收到A柜{}:{}",CabinetConstants.CabinetAType.DROP.desc,NettyUtils.StringListToString(bytesStr));
                List<Integer> ioList = NettyUtils.allIo(bytesStr);
                String flag ;
                //输出
                if("01".equals(bytesStr[9])){
                    flag= "true";
                }else {
                    flag= "false";
                }

                //如果是执行成功 修改IO输出的状态
                if("01".equals(bytesStr[16])){
                    for (int i=0;i<20;i++){
                        //一层 0101 0102
                        String x = String.format(String.format("%02d", Integer.parseInt(bytesStr[8],16)),String.format("%02d", i+1));
                        //传感器触发
                        if(ioList.get(i) ==1){
                            valueOperations.set(String.format(RedisKeyConstant.CABINET_A_IO_OUTPUT_STATUS, x),flag);
                        }
                    }
                }
            }

            case "02"->{
                log.info("收到A柜{}:{}",CabinetConstants.CabinetAType.LED.desc,NettyUtils.StringListToString(bytesStr));
            }

            //皮带伺服
            case "03"->{

                receiveServo(bytesStr);
            }

            case "04"->{

                receiveStep(bytesStr);

            }

            case "05"->{
                log.info("收到A柜{}:{}",CabinetConstants.CabinetAType.OUTPUT.desc,NettyUtils.StringListToString(bytesStr));
            }


            //输入检测
            case "06"->{
                log.info("收到A柜{}:{}",CabinetConstants.CabinetAType.INPUT.desc,NettyUtils.StringListToString(bytesStr));
                receiveSensor(bytesStr);
            }

            //距离传感器
            case "07"->{
                receiveDistance(bytesStr);
            }



            //设置系统参数
            case "80" -> {
                log.info("收到A柜{}:{}",CabinetConstants.CabinetSettingType.SET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
            }



            //获取系统参数
            case "81" -> {
                log.info("收到A柜{}:{}",CabinetConstants.CabinetSettingType.GET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
                cabinetSysMsg.receiveMsgCabinetSys(CabinetConstants.Cabinet.CAB_A,bytesStr);
            }

//            //主动上报
//            case "FF"->{
//                log.info("收到A柜{}:{}",CabinetConstants.CabinetAType.REPORT.desc,NettyUtils.StringListToString(bytesStr));
//
//            }


        }
    }

    /**
     *
     * @param bytesStr
     * A柜伺服接收信息处理
     */
    public void receiveServo(String[] bytesStr) throws IOException {
        //第几个伺服
        int address = Integer.parseInt(bytesStr[9], 16);
        switch (address){
            case 1 -> log.info("收:A柜{}:第1层皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 2 -> log.info("收:A柜{}:第2层皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 3 -> log.info("收:A柜{}:第3层皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 4 -> log.info("收:A柜{}:第4层皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 5 -> log.info("收:A柜{}:第5层皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 6 -> log.info("收:A柜{}:传送小皮带指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 7 -> log.info("收:A柜{}:机械手X轴指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 8 -> log.info("收:A柜{}:机械手Z轴指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            case 9 -> log.info("收:A柜{}:抬升指令指令：{}",CabinetConstants.CabinetAType.SERVO.desc,NettyUtils.StringListToString(bytesStr));

            default -> log.info("收:A柜{}:第{}伺服指令：{}",CabinetConstants.CabinetAType.SERVO.desc,address,NettyUtils.StringListToString(bytesStr));
        }

        //获取当前距离
        if("01".equals(bytesStr[10])){
            BigInteger distance = NettyUtils.parseHexStringArray(bytesStr, 11,4);
            valueOperations.set(RedisKeyConstant.servoGetDistance.CABINET_A, String.valueOf(distance));
        }


        if (("03".equals(bytesStr[11])&&"03".equals(bytesStr[7])&&"0A".equals(bytesStr[6]))|| ("02".equals(bytesStr[11])&&"0D".equals(bytesStr[12]))){
            String msg;
            if(address>=1&&address<=5){
                msg = "第"+address+"皮带报警,请联系售后人员！"+ Arrays.toString(bytesStr);
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.BELT.code,null,msg);
            }else if(address==6||address==9){
                msg = "第"+address+"伺服报警,无法自动发药，请联系售后人员！"+ Arrays.toString(bytesStr);
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SENDDRUG.code,null,msg);
            }else if(address==7||address==8){
                log.error("自动上药伺服报警，结束自动上药");
                sendDrugThreadManager.stop();
                msg = "第"+address+"伺服报警,无法自动上药，请联系售后人员！"+ Arrays.toString(bytesStr);
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,null,msg);
            }
        }




        switch (bytesStr[8]){
            //位置模式
            case "01"->{

                switch (bytesStr[9]){
                    //扫码X伺服
                    case "07" ->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.handServo.X,"true");
                            }
                        }

                    }
                    //扫码Z伺服
                    case "08" ->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.handServo.Z,"true");
                            }
                        }

                    }

                    default -> {
                        switch (bytesStr[11]){
                            case "01"->{
                                log.info("A柜第{}伺服运动完毕",address);
                                valueOperations.set(String.format(RedisKeyConstant.BELT_SERVO_STATUS,address),"true");
                            }
                        }

                    }
                }

            }






//            //速度模式
//            case "02"->{
//                switch (bytesStr[10]) {
//                    //将药品送到边缘
//                    case "10" -> {
//                        switch (bytesStr[11]) {
//                            //动作完成
//                            case "01" -> {
//                                //药停在皮带边缘状态 为 true
//                                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG, address), "true");
//                            }
//
//                            //动作出错
//                            case "02" -> {
//                                switch (bytesStr[12]){
//                                    case "0A", "0D" ->{
//                                        valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG, address), "error");
//                                    }
//
//                                }
//
//                            }
//
//
//                        }
//                    }
//
//                    //将在边缘的药掉下
//                    case "20" -> {
//                        switch (bytesStr[11]) {
//                            //动作完成
//                            case "01" -> {
//                                //A柜皮带上是否有药 为 false
//                                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG, address), "false");
//                                //药停在皮带边缘状态 为 false
//                                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG, address), "false");
//                            }
//                            //动作出错
//                            case "02" -> {
//
//                            }
//                            //传感器超时
//                            case "03" -> {
//
//                            }
//                            //伺服报警
//                            case "04" -> {
//
//                            }
//                        }
//                    }
//
//                }
//            }
        }









    }


    /**
     *
     * @param bytesStr
     * A柜步进信息处理
     */
    public void receiveStep(String[] bytesStr){
        //第几个步进
        int address = Integer.parseInt(bytesStr[9], 16);

        switch (address){
            case 1-> log.info("收:A柜{}:夹爪步进:{}",CabinetConstants.CabinetAType.STEP.desc,NettyUtils.StringListToString(bytesStr));
            case 2-> log.info("收:A柜{}:挡片步进:{}",CabinetConstants.CabinetAType.STEP.desc,NettyUtils.StringListToString(bytesStr));
            default -> log.info("收:A柜{}:第{}步进:{}",CabinetConstants.CabinetAType.STEP.desc,address,NettyUtils.StringListToString(bytesStr));
        }


        switch (bytesStr[8]){
            //位置模式
            case "01"->{
                switch (bytesStr[9]){
                    //旋转步进
                    case "01" ->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS,"true");
                            }
                        }

                    }

                    case "02"->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.CABINET_A_BLOCK_STEP_STATUS,"true");
                            }
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
                                valueOperations.set(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS,"true");
                            }
                        }

                    }

                    case "02"->{
                        switch (bytesStr[11]){
                            case "01"->{
                                valueOperations.set(RedisKeyConstant.CABINET_A_BLOCK_STEP_STATUS,"true");
                            }
                        }

                    }

                }
            }


        }



    }





    /**
     *
     * @param bytesStr
     * A柜输入检测接收信号处理
     */
    public void receiveSensor(String[] bytesStr){
        int address = Integer.parseInt(bytesStr[9], 16);
        switch (bytesStr[9]) {
            //查询所有传感器状态
            case "00"->{
                List<Integer> sensorList = NettyUtils.allInPut(bytesStr);
//                //查询10个药仓位的传感器状态
//                for (int i=6;i<16;i++){
//                    //传感器触发
//                    if(sensorList.get(i) ==1){
//                        valueOperations.set(String.format(RedisKeyConstant.sensor.DROP_SENSOR, i-5),CabinetConstants.SensorStatus.NORMAL.code);
//                    }else {
//                        valueOperations.set(String.format(RedisKeyConstant.sensor.DROP_SENSOR, i-5),CabinetConstants.SensorStatus.RESET.code);
//                    }
//                }

                if(sensorList.get(21) ==1){
                    log.info("光栅传感器触发");
                    valueOperations.set(String.format(RedisKeyConstant.sensor.BELT_SENSOR),CabinetConstants.SensorStatus.NORMAL.code);
                }else {
                    log.info("光栅传感器未触发");
                    valueOperations.set(String.format(RedisKeyConstant.sensor.BELT_SENSOR),CabinetConstants.SensorStatus.RESET.code);
                }

            }

            //机械手传感器
            case "11"->{
                switch (bytesStr[11]) {
                    //触发
                    case "01"-> {
                        log.info("A柜:机械手传感器---触发");
                        valueOperations.set(String.format(RedisKeyConstant.sensor.HAND_SENSOR),CabinetConstants.SensorStatus.NORMAL.code);
                    }
                    //不触发
                    case "02"-> {
                        log.info("A柜:机械手传感器---未触发");
                        valueOperations.set(String.format(RedisKeyConstant.sensor.HAND_SENSOR),CabinetConstants.SensorStatus.RESET.code);
                    }
                }
            }


//            //掉药传感器
//            case "07","08","09","0A","0B","0C","0D","0E","0F","10"->{
//                switch (bytesStr[11]) {
//                    //触发
//                    case "01"-> valueOperations.set(String.format(RedisKeyConstant.sensor.DROP_SENSOR, address-6),CabinetConstants.SensorStatus.NORMAL.code);
//                    //不触发
//                    case "02"-> valueOperations.set(String.format(RedisKeyConstant.sensor.DROP_SENSOR, address-6),CabinetConstants.SensorStatus.RESET.code);
//                }
//            }



            //光栅传感器
            case "16"->{
                switch (bytesStr[11]) {
                    //触发
                    case "01"-> {
                        log.info("A柜:光栅传感器传感器---触发");
                        valueOperations.set(String.format(RedisKeyConstant.sensor.BELT_SENSOR),CabinetConstants.SensorStatus.NORMAL.code);
                    }

                    //不触发
                    case "02"-> {
                        log.info("A柜:光栅传感器传感器---未触发");
                        valueOperations.set(String.format(RedisKeyConstant.sensor.BELT_SENSOR),CabinetConstants.SensorStatus.RESET.code);
                    }

                }
            }


        }




    }



    /**
     *
     * @param bytesStr
     * A柜获取距离传感器数据
     */

    public void receiveDistance(String[] bytesStr){
        switch (bytesStr[bytesStr.length-6]){
            //读取正常
            case "01" ->{
                BigInteger distance = NettyUtils.parseHexStringArray(bytesStr, 11,4);
                BigInteger divisor = new BigInteger("1000");
                distance = distance.divide(divisor);;
                switch (bytesStr[9]){
                    //盘存距离传感器
                    case "01"->{
                        log.info("收到A柜{}:盘存距离传感器:{}",CabinetConstants.CabinetAType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.COUNT, String.valueOf(distance));
                        log.info(String.valueOf(distance));
                        valueOperations.set(RedisKeyConstant.distanceStart.COUNT,"true");

                    }

                }


            }

            //读取异常
            case "02"->{
                switch (bytesStr[9]){
                    //左边距离传感器
                    case "01"->{
                        log.error("收到A柜{}:盘存距离传感器:{}",CabinetConstants.CabinetAType.DISTANCE.desc,NettyUtils.StringListToString(bytesStr));
                        valueOperations.set(RedisKeyConstant.distance.COUNT, "ERROR");
                        valueOperations.set(RedisKeyConstant.distanceStart.COUNT,"true");

                    }

                }
            }
        }
    }


}
