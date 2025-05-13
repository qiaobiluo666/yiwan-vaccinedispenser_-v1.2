package com.yiwan.vaccinedispenser.system.dispensing;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.camera.CameraSendMsg;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import com.yiwan.vaccinedispenser.system.sys.data.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/15 9:32
 */
@Slf4j
@Component
public class SendDrugFunction {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private CabinetBService cabinetBService;

    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;




    @Autowired
    private CameraSendMsg cameraSendMsg;


    @Autowired
    private VacBoxSpecService vacBoxSpecService;


    @Autowired
    private VacMachineService vacMachineService;


    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private SendDrugFunction sendDrugFunction;


    @Autowired
    private VacDrugMapper vacDrugMapper;

    @Autowired
    private VacDrugService vacDrugService;


    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private VacMachineDrugService vacMachineDrugService;


    @Autowired
    private WebsocketService websocketService;




    /**
     * 药掉到滑台 并测量距离
     */

    public void goTable(ConfigData configData) throws ExecutionException, InterruptedException, IOException {

        //启动运输
        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START))){
            valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
                //启动自动上药流程
            autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);

        }

        //根据传感器来判断
//        if("true".equals(valueOperations.get(RedisKeyConstant.sensor.TABLE_SENSOR))&&"true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN))) {

        log.info("测距线程 redis状态：药品是否到滑台：{},是否可以开始测距:{}",valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH),valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN));
        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH))&&"true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_RUN))) {
            log.info("开始测距");
            //重置滑台信号
            valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT,"1");

            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_RUN,"false");

            // 设置开始测距 判断滑台是否还有药盒 为false
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"false");

            //扫码不能先动 等待长宽高检测完毕
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_SCAN,"false");

            aboveCamera();
            //打开3个距离传感器 计算距离
            DistanceServoData distanceServoData =distanceServoAll(configData);

            //判断药盒形态
            if(distanceServoData.getIsReturn()) {
                tableReturn();
                return;
            }

            String result = valueOperations.get(RedisKeyConstant.scanCode.ABOVE);
            if("NoRead".equals(result)){
                aboveCamera();
                VacUntil.sleep(500);
                result = valueOperations.get(RedisKeyConstant.scanCode.ABOVE);
            }

            distanceServoData.setAboveCode(result);
            valueOperations.set(RedisKeyConstant.CABINET_B_DRUG_MSG,JSON.toJSONString(distanceServoData));

            //可以开始扫码
            log.info("更改状态：可以开始扫码抓药");
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_SCAN,"true");
            valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");
            //等待Z轴   最多6s  将药吸上去 再次用距离传感器测距 如果相差不大 则判定滑台上没有其他药 如果有 掉药 重新发药
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
                if ("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_DRUGS_START))) {
                    break;
                }
                VacUntil.sleep(200);
            }
            log.info("检测滑台上是否有药品");

            //测试左右俩边的距离
            distanceSensor(configData);

        }else {

            //滑台传感器没有触发 每过1秒发送查询指令
            intPut(CabinetConstants.Cabinet.CAB_B,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_B_TABLE_NUM);
            VacUntil.sleep(1000);
            String countStr = valueOperations.get(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT);

            if(countStr==null){
//                valueOperations.set(RedisKeyConstant.CABINET_B_COUNT,"1");


                valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT,"1");
            }else {
                int count = Integer.parseInt(countStr);
                if(count>=30) {
                    log.error("触发30次 开始自动上药");
                    autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);
                    valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR_COUNT,"1");
                }


            }

    }

    }



    /**
     * 自动上药总流程
     */
    public void sendDrug( ConfigData configData,ConfigSetting configSetting) throws Exception {
        //开始扫码
        if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_SCAN))){
            log.info("===================================开始扫码================================================");
            //先将状态设置为false
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_SCAN,"false");
            //获取扫码信息
            String distanceServoDataStr = valueOperations.get(RedisKeyConstant.CABINET_B_DRUG_MSG);
            DistanceServoData distanceServoData = JSON.parseObject(distanceServoDataStr,DistanceServoData.class);
            //扫描上方药盒条形码 有俩个
            ScanCodeData scanCodeData = new ScanCodeData();
            assert distanceServoData != null;
            scanCodeData.setAboveCode(distanceServoData.getAboveCode());
            if(scanCodeData.getAboveCode().split(";").length>1){
                tableReturn();
                return;
            }

            //判断机械手是否运动完成
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END))){
                    break;
                }
                VacUntil.sleep(200);
            }

            log.info("运动扫码伺服 机械手抓取药盒中心点");
            //运动扫码伺服 机械手抓取药盒中心点
            boolean isNormal = moveScanServo(distanceServoData,1);
            if(!isNormal){
                String msg = "伺服报警 退出抓取药盒中心点！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                return;
            }

            //运动完成以后 吸盘吸住药盒
            xiPangStart();

            //将Z轴回到侧边扫码的距离
            distanceServoData.setServoZ(configData.getSideScanZ());

            isNormal= moveScanServo(distanceServoData,2);
            if(!isNormal){
                xiPangEnd();
                String msg = "伺服报警 退出将药盒抓起！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                return;
            }
            //抓取药盒完毕 开始测距 判断滑台是否还有药盒
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"true");
            //等待判断下方是否有药的结果
            timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END))){
                    log.info("检测下方是否有药完毕");
                    VacUntil.sleep(200);
                    break;
                }
            }

            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");

            //如果确定滑台有药 回滚
            if( Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT), "true")){
                servoTableReturn(configData,"滑台有药 退回");
                return;
            }

            //启动自动上药流程
            autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);

            //步进走零
            cabinetBStepPosition(CabinetConstants.CabinetBStepMode.ROTATE,0);

            if("NoRead".equals(scanCodeData.getAboveCode())){
                //步进电机 旋转扫码
                scanCodeData = rotateAngle(CabinetConstants.CabinetBStepMode.ROTATE,scanCodeData,distanceServoData,configData);
                if(scanCodeData.getIsServoError()){
                    xiPangEnd();
                    return;
                }
            }else {
                scanCodeData.setCode(scanCodeData.getAboveCode());
            }

            log.info("扫码信息：{}",JSON.toJSONString(scanCodeData));
            // 没扫码到码 或者 滑台上还有药
            if(scanCodeData.getCode()==null||  scanCodeData.getCode().trim().isEmpty()) {
//            if(("NoRead".equals(scanCodeData.getAboveCode())&&"NoRead".equals(scanCodeData.getSideCode())&&"NoRead".equals(scanCodeData.getBelowCode()))|| Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT), "true")){
                servoTableReturn(configData,"没扫码到码,退回");
                return;

            }

            //TODO 根据政采云接口 拿到药盒的长宽高
            log.info("========================开始运动到掉药区域==============================");
            DrugRecordRequest drugRecordData = null;
            if("true".equals(configSetting.getZcyAuto())){
//            if("true".equals(isSendOpen)){
                //跟政采云扫码 获得 药品信息
                drugRecordData = zcyFunction.getVaccineMsgByCode(scanCodeData.getCode());
                log.info("拿到政采云疫苗信息：{}",JSON.toJSONString(drugRecordData));
            }else {
                if(scanCodeData.getCode().length()>7){
                    drugRecordData = vacDrugService.sendDrugTest(scanCodeData.getCode());
                    drugRecordData.setExpiredAt(new Date());
                    drugRecordData.setBatchNo("测试编号");
                    drugRecordData.setPrice(String.valueOf(321));
                    drugRecordData.setTag("测试标签");
                    drugRecordData.setSupervisedCode(scanCodeData.getCode());
                    log.info("测试疫苗信息：{}",JSON.toJSONString(drugRecordData));
                }else {
                    drugRecordData.setIsReturn(true);
                }

            }

            if(drugRecordData.getIsReturn()){
                //电子监管码请求失败
                //TODO 没有仓位可以装这个药
                log.error("自动上药异常：电子监管码请求失败：{}",drugRecordData.getMsg());
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,scanCodeData.getCode(),drugRecordData.getMsg());
                servoTableReturn(configData,"电子监管码请求失败,退回");
                return;
            }


            //查看尺寸对不对
            VaccineData drugFlag = drugIsRight(distanceServoData,drugRecordData.getProductNo());
            if(!drugFlag.getIsRight()){
                log.error("{}尺寸不合规，回滚重发。测量到的长宽高：{} {} {} 数据库的长宽高：{} {} {}"
                        ,drugFlag.getProductName(),distanceServoData.getVaccineLong(),distanceServoData.getVaccineWide(),distanceServoData.getVaccineHigh(),
                        drugFlag.getDrugLong(),drugFlag.getDrugWide(),drugFlag.getDrugHigh());
//                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),msg);
                servoTableReturn(configData,"药品尺寸不合规，退回");
                return;
            }

            //查看有没有仓位
            DrugRecordRequest drugRecordRequest = findBox(distanceServoData,drugRecordData);
            //如果为null 没有这个药的规格或者仓位满了
            if(drugRecordRequest==null){
                //TODO 没有仓位可以装这个药
                String msg = "自动上药异常：没有仓位可以装:"+drugRecordData.getProductName();
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),msg);
                servoTableReturn(configData,"没有仓位能装退回");
                return;
            }

            //等机械手回原以后才能开始掉药
            boolean flag = false;
            timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.DRUG_DROP_HAND_WAIT_TIME){
                VacUntil.sleep(200);
                if("true".equals(valueOperations.get(RedisKeyConstant.HANDLE_IS_DROP))){
                    flag = true;
                    break;
                }
            }

            if(!flag){

                log.error("机械手20秒还没回原！机器异常，停止上药！");
                log.error("自动上药异常：传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code, "传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                servoTableReturn(configData,"自动上药异常：传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                sendDrugThreadManager.stop();
                return;
            }


            //查看机械手上是否有药
            boolean  handHasDrugFlag= handHasDrug();
            //传感器有药！机器暂停
            if (handHasDrugFlag) {
                //传感器一直触发 检查机械手是否有药、或者传感器损坏
                log.error("自动上药异常：传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code, "传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                servoTableReturn(configData,"自动上药异常：传感器损坏或机械手上还有药，请人工拿掉药后再开始自动上药");
                sendDrugThreadManager.stop();
                return;
            }


//           提前走夹药一段距离
            int longs = distanceServoData.getVaccineWide()*100;
            if((configData.getHandLen()-longs-configData.getEarly())>0){
                int earlyDis = configData.getHandLen()-longs-configData.getEarly();
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,earlyDis);

            }

            //移动到掉药区域
            isNormal = goDrop(distanceServoData,configData,scanCodeData);
            if(!isNormal){
                String msg = "伺服报警 退出移动到掉药区域！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                servoTableReturn(configData,"伺服报警,退回");
                return;
            }

            //吸盘放下
            xiPangEnd();
            //等待1S后 伺服回到初始位置
            VacUntil.sleep(200);
            //先回扫码位置 不然要撞到
            Thread threadInit = new Thread(() -> {
                try {
                    cabinetBServoInit();
                } catch (IOException e) {
                    // 处理异常，例如记录日志
                    e.printStackTrace();
                }
            });
            threadInit.start();

            //查看机械手上是否有药 有药继续
            boolean  dropDrugFlag= dropHasDrug(configData,longs);
            int errorCount;
            //机械手没药！重新开始上药
            if (!dropDrugFlag) {

                //传感器一直不触发 检查机械手是否有药、或者传感器损坏
                log.error("自动上药异常：药物异常报警,药没掉入机械手");
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),"药没掉入机械手");
                servoTableReturn(configData,"药物异常报警,药没掉入机械手");

                String errorCountStr = valueOperations.get(RedisKeyConstant.CABINET_B_ERROR_COUNT);
                if(errorCountStr==null){
                    errorCount = 0;
                }else {
                    errorCount = Integer.parseInt(errorCountStr);
                }
                if(errorCount<=3){
                    valueOperations.set(RedisKeyConstant.CABINET_B_COUNT,String.valueOf(errorCount+1));
                }else {
                    log.error("药没掉入机械手3次！自动上药停止");
                    sendDrugThreadManager.stop();
                }
                return;
            }

            //掉药到机械手 重新开一个线程
            Thread thread = new Thread(() -> {
                valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"false");
                valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"false");
                log.info("================================开始机械手程序===============================");
                valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");

                //夹紧 空出200
                int clampDis = configData.getHandLen()-longs-configData.getGap();
                //走的行程
                int dropDis = configData.getHandLen()-longs+200;
                if(!dropDrugHandle(clampDis,dropDis,drugRecordRequest)){
                    log.error("自动上药异常：药物异常报警,药没掉入药仓");
                    return;
                }

                //机械手上有药，仓位药品数量+1，新增上药记录
                addDrugRecord(drugRecordRequest,1);

                //A柜 步进电机 回原
                cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);

                //机械手回原
                moveHandServoInit(configData);

                //可以进入掉药区域
                valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
                log.info("=================================上药结束==========================");
            });
            thread.start();

            long timeoutInit = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeoutInit) < SettingConstants.CABINET_INIT_WAIT_TIME) {

                if(Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_INIT), "true")){
                    break;
                }
                VacUntil.sleep(200);
            }
            //重新开始走发药皮带
            autoInit();
        }
        else {
            log.info("还未开始上药");
        }
    }

    //机械手挡片 夹药回原   掉药
    public boolean dropDrugHandle(int clampDis , int dropDis ,DrugRecordRequest drugRecordRequest){

        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,clampDis);
        //等待夹爪步进电机运动完成
        waitCabinetAStepEnd(1);

        //运动伺服
        moveHandServo(drugRecordRequest.getAutoX(),drugRecordRequest.getAutoZ());

        //挡片步进电机旋转90
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.BLOCK,900);
        waitCabinetAStepEnd(2);

        //A柜机械手 步进电机走 0
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
        waitCabinetAStepEnd(1);


        String sensorIsPuts= null;
        boolean drugHand = false;
        boolean inputFlag = false;
        long timeouts = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeouts) < SettingConstants.WAIT_DROP_TIME){
            //查询 机械手底部传感器信号
            intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
            VacUntil.sleep(200);
            //判断机械手底部传感器信号是否被触发
            sensorIsPuts = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
            assert sensorIsPuts != null;
            //如果传感器触发 一直等待 不触发结束
            if(sensorIsPuts.equals(CabinetConstants.SensorStatus.RESET.code)){
                drugHand =true;
                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
                break;
            }

            if((System.currentTimeMillis() - timeouts)>3000&&(System.currentTimeMillis() - timeouts)<15000){

                //夹紧
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,dropDis);
                waitCabinetAStepEnd(1);

                //震动电机
                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
                inputFlag = true;

                //空出 1cm缝隙
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
                waitCabinetAStepEnd(1);
                VacUntil.sleep(500);

            }

            if((System.currentTimeMillis() - timeouts)>15000 && inputFlag ){
                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
                inputFlag = false;
            }


        }

        if(!drugHand){
            log.error("自动上药异常：药物异常报警,药没掉入药仓");
            return false;
        }

        return  true;

    }



    //A柜步进电机位置模式
    public void cabinetAStepPosition(CabinetConstants.CabinetAStepMode mode , int distance){
        CabinetAStepRequest stepRequests = new CabinetAStepRequest();
        stepRequests.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        stepRequests.setCommand(CabinetConstants.CabinetAStepCommand.POSITION);
        stepRequests.setMode(mode);
        stepRequests.setDistance(distance);
        stepRequests.setStatus(CabinetConstants.CabinetAStepStatus.ZERO);
        cabinetAService.step(stepRequests);
    }


    //A柜步进电机位置模式
    public void cabinetBStepPosition(CabinetConstants.CabinetBStepMode mode , int distance){
        CabinetBStepRequest stepRequests = new CabinetBStepRequest();
        stepRequests.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        stepRequests.setCommand(CabinetConstants.CabinetBStepCommand.POSITION);
        stepRequests.setMode(mode);
        stepRequests.setDistance(distance);
        stepRequests.setStatus(CabinetConstants.CabinetBStepStatus.ZERO);
        cabinetBService.step(stepRequests);

        //等待B柜步进电机运动结束
        waitCabinetBStepEnd();
    }



    public void xiPangStart(){

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_END_NUM,CabinetConstants.OutPutCommand.OUTPUT);
        VacUntil.sleep(200);

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
        VacUntil.sleep(200);

    }



    public void xiPangEnd(){

//        //打开电磁铁让药盒掉落
//        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
//        VacUntil.sleep(200);


        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
        VacUntil.sleep(200);

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_END_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
        VacUntil.sleep(200);


    }



    //移动扫码伺服
    public void moveHandServo(int x,int z){

        //设置3个伺服都为未运动完成状态
        valueOperations.set(RedisKeyConstant.handServo.X,"false");
        valueOperations.set(RedisKeyConstant.handServo.Z,"false");

        CabinetAServoRequest request = new CabinetAServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAServoCommand.POSITION);
        request.setMode(CabinetConstants.CabinetAServoMode.APPLY_SERVO_X.num);
        request.setStatus(CabinetConstants.CabinetAServoStatus.ZERO);
        request.setDistance(x);
        cabinetAService.servo(request);
        VacUntil.sleep(50);
        request.setDistance(z);
        request.setMode(CabinetConstants.CabinetAServoMode.APPLY_SERVO_Z.num);
        cabinetAService.servo(request);
        VacUntil.sleep(50);


        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
           
            if("true".equals(valueOperations.get(RedisKeyConstant.handServo.X))&&"true".equals(valueOperations.get(RedisKeyConstant.handServo.Z))){
                break;
            }
            VacUntil.sleep(200);
        }
    }

    //机械手回零
    public void moveHandServoInit(ConfigData configData){

        valueOperations.set(RedisKeyConstant.handServo.X,"false");
        valueOperations.set(RedisKeyConstant.handServo.Z,"false");

        CabinetAServoRequest request = new CabinetAServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAServoCommand.POSITION);
        request.setMode(CabinetConstants.CabinetAServoMode.APPLY_SERVO_Z.num);
        request.setStatus(CabinetConstants.CabinetAServoStatus.ZERO);
        request.setDistance(configData.getHandInitZ());
        cabinetAService.servo(request);
        
//        long timeout = System.currentTimeMillis();
//        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
//            if("true".equals(valueOperations.get(RedisKeyConstant.handServo.Z))){
//                break;
//            }
//            VacUntil.sleep(200);
//        }

        VacUntil.sleep(50);
        request.setDistance(configData.getHandInitX());
        request.setMode(CabinetConstants.CabinetAServoMode.APPLY_SERVO_X.num);
        cabinetAService.servo(request);

        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

            if("true".equals(valueOperations.get(RedisKeyConstant.handServo.X))&&"true".equals(valueOperations.get(RedisKeyConstant.handServo.Z))){
                break;
            }
            VacUntil.sleep(200);
        }

    }

    //A点步进电机回原
    public void cabinetAStepInit(CabinetConstants.CabinetAStepMode mode){
        CabinetAStepRequest stepRequest = new CabinetAStepRequest();
        stepRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        stepRequest.setCommand(CabinetConstants.CabinetAStepCommand.ZERO);
        stepRequest.setMode(mode);
        stepRequest.setStatus(CabinetConstants.CabinetAStepStatus.ZERO);
        cabinetAService.step(stepRequest);
    }



    //开始发药指令
    public void autoDrug(CabinetConstants.CabinetBApplyCommand command, CabinetConstants.CabinetBApplyMode mode, CabinetConstants.CabinetBApplyStatus status){
        CabinetBApplyRequest cabinetBApplyRequest = new CabinetBApplyRequest();
        cabinetBApplyRequest.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        cabinetBApplyRequest.setCommand(command);
        cabinetBApplyRequest.setStatus(status);
        cabinetBApplyRequest.setMode(mode);
        cabinetBService.apply(cabinetBApplyRequest);
        if(mode.equals(CabinetConstants.CabinetBApplyMode.START)){
            valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH,"false");
        }

    }

    //重新开始上药初始化
    public void autoInit() throws IOException {
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_RUN,"true");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"false");
//        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"false");
        cabinetBStepInit();
        cabinetBServoInit();
    }




    //3个距离传感器计算距离
    public DistanceServoData distanceServoAll(ConfigData configData) throws ExecutionException, InterruptedException, IOException {

        log.info("开始测距离");
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        DistanceServoData data =new DistanceServoData();
        long time = System.currentTimeMillis();
        Integer resultLeft = getDistanceLeft();
        Integer resultRight = getDistanceRight();

        if(resultLeft==null ){
            log.error("左侧传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }
        if(resultRight==null ){
            log.error("右侧侧传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }

        //左边距离多少
        int left = configData.getLeftConstants()-resultLeft/1000;
        //右边距离多少
        int right =configData.getRightConstants()-resultRight/1000;

        log.info("左边距离：{}",left);
        log.info("右边距离：{}",right);

        if(left>210||right>210){
            data.setIsReturn(true);
            log.error("药盒尺寸超出机器量程");
            return  data;
        }

        //走负数 伺服要报警
        if(left<=20 || right<=20){
            data.setIsReturn(true);
            log.error("药盒尺寸有问题");
            return  data;
        }
        //移动中心点 测量激光
        DistanceServoData distanceServoData = new DistanceServoData();
        int moveX;
        int moveY ;

        if("true".equals(configSetting.getBFindX())){
            //计算中心点的XY走的距离
            data = VacUntil.findRectangleCenterX(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX()-configData.getSensorDistanceX();

        }else {
            data = VacUntil.findRectangleCenterY(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX()+configData.getSensorDistanceX();
        }

        moveY = data.getServoY()+configData.getSensorDistanceY();

//        if("true".equals(isX)){
//            //计算中心点的XY走的距离
//            data = VacUntil.findRectangleCenterX(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
//            moveX= data.getServoX()-configData.getSensorDistanceX();
//        }else {
//            data = VacUntil.findRectangleCenterY(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
//            moveX= data.getServoX()+configData.getSensorDistanceX();
//        }
//        moveY = data.getServoY()+configData.getSensorDistanceY();

        log.info(JSON.toJSONString(data));
        distanceServoData.setServoX(moveX);
        distanceServoData.setServoY(moveY);
        distanceServoData.setServoZ(configData.getSensorDistanceZ());

        //查看机械手是否回原
        while ((System.currentTimeMillis() - time) < 5){
            if(Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_INIT), "true")){
                break;
            }
            VacUntil.sleep(200);
        }

        //Z轴先走
        boolean  isNormal= moveScanServo(distanceServoData,2);
        if(!isNormal){
            log.error("======================================伺服报警 退出测距功能！===============================================");

            data.setIsReturn(true);
            return data;
        }
        Integer resultHigh =getDistanceHigh();
        if(resultHigh==null){
            log.error("上方传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }
        //上方距离多少
        int high = configData.getHeightConstants()-resultHigh/1000;
        log.info("上边距离：{}",high);

        //走的高
        data.setServoZ(configData.getTableZ()-high*100);
        //根据长宽高判断药盒的形态是否正确 如果不正确则反转 重新发药

        if(high>left || high>right){
            data.setIsReturn(Math.abs(high - left) > 3 && Math.abs(high - right) > 3);
        }else {
            data.setIsReturn(false);
        }

        if(high<=5){
            data.setIsReturn(true);
        }

        //长和宽都大于100
        if(left>=100&&right>=100){
            data.setIsReturn(true);
        }


        //判断长的边是否在左侧
        data.setIsLeft(left>=right);
        //将测量出来的长宽高
        data.setVaccineHigh(high);
        if(left>right){
            data.setVaccineWide(right);
            data.setVaccineLong(left);
        }else {
            data.setVaccineWide(left);
            data.setVaccineLong(right);
        }

        //测试是否疫苗在库里
        VaccineData drugFlag = drugIsRight(data,null);
        if(!drugFlag.getIsRight()){
            log.error("疫苗尺寸不合规，回滚重发。测量到的长宽高：{} {} {}",data.getVaccineLong(),data.getVaccineWide(),data.getVaccineHigh());
            data.setIsReturn(true);
        }
       int distance = 0;

        //判断完左边长还是左边短 旋转步进
        if(left>=right){

            distance = configData.getLeftAngle();
//            distance = SettingConstants.AngleDistance.LEFT_RETURN;

        }else {
            distance = configData.getRightAngle();
//            distance = SettingConstants.AngleDistance.RIGHT_RETURN;
        }

        cabinetBStepPosition(CabinetConstants.CabinetBStepMode.ROTATE,distance);
        log.info("药盒长宽高数据:{}",data);

        //将疫苗的长宽高和信息都存入redis
        return data;

    }

    public DistanceServoData  DistanceSerVoGetXY(ConfigData configData)throws ExecutionException, InterruptedException, IOException {
        log.info("开始测距离");
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        DistanceServoData data =new DistanceServoData();
        long time = System.currentTimeMillis();
        Integer resultLeft = getDistanceLeft();
        Integer resultRight = getDistanceRight();

        if(resultLeft==null ){
            log.error("左侧传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }
        if(resultRight==null ){
            log.error("右侧侧传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }
        //左边距离多少
        int left = configData.getLeftConstants()-resultLeft/1000;
        //右边距离多少
        int right =configData.getRightConstants()-resultRight/1000;
        log.info("左边距离：{}",left);
        log.info("右边距离：{}",right);

        if(left>210||right>210){
            data.setIsReturn(true);
            log.error("药盒尺寸超出机器量程");
            return  data;
        }

        //走负数 伺服要报警
        if(left<=20 || right<=20){
            data.setIsReturn(true);
            log.error("药盒尺寸有问题");
            return  data;
        }
        //移动中心点 测量激光
        DistanceServoData distanceServoData = new DistanceServoData();
        int moveX;
        int moveY ;

        if("true".equals(configSetting.getBFindX())){
            //计算中心点的XY走的距离
            data = VacUntil.findRectangleCenterX(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX()-configData.getSensorDistanceX();

        }else {
            data = VacUntil.findRectangleCenterY(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX()+configData.getSensorDistanceX();
        }
        moveY = data.getServoY()+configData.getSensorDistanceY();
        log.info(JSON.toJSONString(data));
        distanceServoData.setServoX(moveX);
        distanceServoData.setServoY(moveY);
        distanceServoData.setServoZ(configData.getSensorDistanceZ());
        //查看机械手是否回原
        while ((System.currentTimeMillis() - time) < 5){
            if(Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_INIT), "true")){
                break;
            }
            VacUntil.sleep(200);
        }

        Integer resultHigh =getDistanceHigh();
        if(resultHigh==null){
            log.error("上方传感器测试异常处理");
            data.setIsReturn(true);
            return data;
        }
        //上方距离多少
        int high =resultHigh/1000;
        log.info("上边距离：{}",high);

        //走的高
        data.setServoZ(null);
        data.setVaccineWide(left);
        data.setVaccineLong(right);
        //将疫苗的长宽高和信息都存入redis
        return data;
    }




    //2个距离传感器计算距离
    public void distanceSensor(ConfigData configData){
            int count=0;
            Integer resultLeft = getDistanceLeft();
            Integer resultRight = getDistanceRight();

            //左右距离传感器会出现 多次给数据情况
            while (count<5){
                if(resultLeft==null || resultRight==null  ){
                    //测量左
                     resultLeft = getDistanceLeft();
                     resultRight = getDistanceRight();
                }else {
                    break;
                }
                count++;
            }

            if(resultLeft==null || resultRight==null){
                log.error("距离传感器测试异常,回滚重发！！！");
                valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"true");
                //测试完毕
                valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"true");
                return;
            }

            //左边距离多少
            int left = configData.getLeftConstants()-resultLeft/1000;
            //右边距离多少
            int right =configData.getRightConstants()-resultRight/1000;

            //如果误差大于10 说明台面上有药 需要机械手直接走到废料区
            if(left>10 || right>10){
                valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"true");
                log.error("台面上还有药，回滚重发！！！");
            }
            //测试完毕
            valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"true");

    }


    public Integer getDistanceLeft(){
        distanceLeft();
        int count= 1;
        while (count<5){
            String leftDis = valueOperations.get(RedisKeyConstant.distance.LEFT);
            if("ERROR".equals(leftDis)||"-1".equals(leftDis)){
                distanceLeft();
            }else {
                assert leftDis != null;
                return Integer.parseInt(leftDis);
            }
            count++;
        }
        return null;
    }



    public Integer getDistanceRight(){
        distanceRight();

        int count=1;
        while (count<5){
            String rightDis = valueOperations.get(RedisKeyConstant.distance.RIGHT);
            if("ERROR".equals(rightDis)||"-1".equals(rightDis)){
                distanceRight();
            }else {
                assert rightDis != null;
                return Integer.parseInt(rightDis);
            }
            count++;
        }
        return null;
    }

    public Integer getDistanceHigh(){
        distanceHigh();

        int count=1;
        while (count<5){
            String highDis = valueOperations.get(RedisKeyConstant.distance.HIGH);
            if("ERROR".equals(highDis)||"-1".equals(highDis)){
                distanceHigh();
            }else {
                assert highDis != null;
                return Integer.parseInt(highDis);
            }
            count++;
        }
        return null;
    }

    public Integer getDistanceCount(){
        distanceCount();

        int count=1;
        while (count<5){
            String countDis = valueOperations.get(RedisKeyConstant.distance.COUNT);
            if("ERROR".equals(countDis)||"-1".equals(countDis)){
                distanceCount();
            }else {
                assert countDis != null;
                return Integer.parseInt(countDis);
            }
            count++;
        }
        return null;
    }




    public void  distanceLeft(){
        valueOperations.set(RedisKeyConstant.distanceStart.LEFT,"false");
        valueOperations.set(RedisKeyConstant.distance.LEFT,"-1");
        CabinetBGetDistanceRequest request = new CabinetBGetDistanceRequest();
        request.setMode(1);
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setCommand(CabinetConstants.CabinetBGetDistanceCommand.GET);
        cabinetBService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.LEFT)) ) {
                break;
            }
            VacUntil.sleep(50);
        }

    }


    public void  distanceRight(){

        valueOperations.set(RedisKeyConstant.distanceStart.RIGHT,"false");
        valueOperations.set(RedisKeyConstant.distance.RIGHT,"-1");
        CabinetBGetDistanceRequest request = new CabinetBGetDistanceRequest();
        request.setMode(2);
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setCommand(CabinetConstants.CabinetBGetDistanceCommand.GET);
        cabinetBService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.RIGHT)) ) {
                break;
            }
            VacUntil.sleep(50);
        }
    }

    public void distanceHigh(){
        valueOperations.set(RedisKeyConstant.distanceStart.HIGH,"false");
        valueOperations.set(RedisKeyConstant.distance.HIGH,"-1");
        CabinetBGetDistanceRequest request = new CabinetBGetDistanceRequest();
        request.setMode(3);
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setCommand(CabinetConstants.CabinetBGetDistanceCommand.GET);
        cabinetBService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.HIGH)) ) {
                break;
            }
            VacUntil.sleep(50);
        }
    }

    public void distanceCount(){
        valueOperations.set(RedisKeyConstant.distanceStart.COUNT,"false");
        valueOperations.set(RedisKeyConstant.distance.COUNT,"-1");
        CabinetAGetDistanceRequest request = new CabinetAGetDistanceRequest();
        request.setMode(1);
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAGetDistanceCommand.GET);
        cabinetAService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.COUNT)) ) {
                break;
            }
            VacUntil.sleep(50);
        }
    }





    //B柜Z轴回原点
    public  void  moveServoZZero(){

        valueOperations.set(RedisKeyConstant.scanServo.Z, "false");

        CabinetBServoRequest request = new CabinetBServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setCommand(CabinetConstants.CabinetBServoCommand.POSITION);
        request.setStatus(CabinetConstants.CabinetBServoStatus.ZERO);
        request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Z.num);
        request.setDistance(0);
        cabinetBService.servo(request);

        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
           
            if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.Z)) ) {
                break;
            }
            VacUntil.sleep(200);
        }

    }



    //移动扫码伺服 1 Z轴最后走
    public boolean moveScanServo(DistanceServoData data,int type) throws IOException {
        boolean servoNormalFlag = true;
        //1 Z最后走 2 Z轴先走
        valueOperations.set(RedisKeyConstant.scanServo.X, "false");
        valueOperations.set(RedisKeyConstant.scanServo.Y, "false");
        valueOperations.set(RedisKeyConstant.scanServo.Z, "false");
        Integer distanceX = data.getServoX();
        Integer distanceY = data.getServoY();
        Integer distanceZ = data.getServoZ();
        CabinetBServoRequest request = new CabinetBServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setCommand(CabinetConstants.CabinetBServoCommand.POSITION);
        request.setStatus(CabinetConstants.CabinetBServoStatus.ZERO);

        //Z轴最后走
        if (type == 1) {
            request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_X.num);
            request.setDistance(distanceX);
            cabinetBService.servo(request);
            VacUntil.sleep(50);
            request.setDistance(distanceY);
            request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Y.num);
            cabinetBService.servo(request);
            VacUntil.sleep(50);
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
               
                if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.X)) && "true".equals(valueOperations.get(RedisKeyConstant.scanServo.Y))) {
                    break;
                }
                VacUntil.sleep(100);

                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR))){
                    log.error("B柜伺服已经报警！终止自动上药程序！");
                    servoNormalFlag = false;
                    sendDrugThreadManager.stop();
                    break;
                }

                VacUntil.sleep(100);

            }

            //如果伺服没有报警 则继续运动
            if(servoNormalFlag){
                request.setDistance(distanceZ);
                request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Z.num);
                cabinetBService.servo(request);

                timeout = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

                    if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.Z))) {
                        break;
                    }
                    VacUntil.sleep(200);
                }
            }

        } else {
            //Z轴先走
            request.setDistance(distanceZ);
            request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Z.num);
            cabinetBService.servo(request);
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
                if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.Z))) {
                    break;
                }
                VacUntil.sleep(100);

                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_ERROR))){
                    log.error("B柜伺服已经报警！终止自动上药程序！");
                    sendDrugThreadManager.stop();
                    servoNormalFlag = false;
                    break;
                }

                VacUntil.sleep(100);
            }

            if(servoNormalFlag){
                request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_X.num);
                request.setDistance(distanceX);
                cabinetBService.servo(request);
                VacUntil.sleep(50);
                request.setDistance(distanceY);
                request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Y.num);
                cabinetBService.servo(request);
                VacUntil.sleep(50);

                timeout = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

                    if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.X)) && "true".equals(valueOperations.get(RedisKeyConstant.scanServo.Y))) {
                        break;
                    }
                    VacUntil.sleep(200);
                }
            }
        }

        //如果伺服报警 则显示3轴运动失败  true 运动正常 false 运动异常
        return servoNormalFlag;

    }



    /**
     *
     * @return
     * 上方扫码器
     */
    public  void aboveCamera(){
        //运动到上方扫码伺服
        valueOperations.set(RedisKeyConstant.scanCode.ABOVE,"NoRead");
        cameraSendMsg.sendCommandToAboveCamera();
    }


    /**
     *
     * @return
     * 下方扫码器
     */
    public  ScanCodeData getBelowCamera(ScanCodeData scanCodeData){
        String  result = valueOperations.get(RedisKeyConstant.scanCode.BELOW);
        scanCodeData.setBelowCode(result);
        return  scanCodeData;
    }


    /**
     *
     * @return
     * 侧方扫码器
     */
    public  ScanCodeData getSideCamera(ScanCodeData scanCodeData){
        String  result = valueOperations.get(RedisKeyConstant.scanCode.SIDE);
        scanCodeData.setSideCode(result);
        return  scanCodeData;
    }



    //旋转步进 角度模式(扫码)
    public ScanCodeData  rotateAngle(CabinetConstants.CabinetBStepMode mode,ScanCodeData scanCodeData,DistanceServoData distanceServoData,ConfigData configData) throws IOException {

        //开始侧边区域扫码
        distanceServoData.setServoX(configData.getSideScanX());
        distanceServoData.setServoY(configData.getSideScanY());
        distanceServoData.setServoZ(configData.getSideScanZ());
        boolean  isNormal = moveScanServo(distanceServoData,2);
        if(!isNormal){
            log.error("======================================伺服报警 退出去侧方扫码功能！===============================================");
            scanCodeData.setIsServoError(true);
            return scanCodeData;
        }else {
            scanCodeData.setIsServoError(false);
        }

        CabinetBStepRequest request = new CabinetBStepRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setMode(mode);
        request.setCommand(CabinetConstants.CabinetBStepCommand.POSITION);
        request.setStatus(CabinetConstants.CabinetBStepStatus.ZERO);


        //先扫码该位置是否有条形码
        //侧边扫码 先将其设置未NoRead
        valueOperations.set(RedisKeyConstant.scanCode.LAST_SIDE, "NoRead");
        valueOperations.set(RedisKeyConstant.scanCode.SIDE, "NoRead");

        valueOperations.set(RedisKeyConstant.scanCode.LAST_BELOW, "NoRead");
        valueOperations.set(RedisKeyConstant.scanCode.BELOW, "NoRead");

        //旋转前先扫一下码
        cameraSendMsg.sendCommandToSideCamera();
        scanCodeData = getSideCamera(scanCodeData);

        //下方扫码线程
        Thread belowScan = new Thread(() -> {
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_BELOW_WAIT_TIME) {
                cameraSendMsg.sendCommandToBelowCamera();
                VacUntil.sleep(50);
            }
        });


        if("NoRead".equals(scanCodeData.getSideCode())){

            //下方扫码、侧方扫码开始
            belowScan.start();
            boolean flag = false;
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_SIDE_WAIT_TIME) {
                if (!flag){
                    request.setDistance(SettingConstants.AngleDistance.ALL);
                    cabinetBService.step(request);
                    flag = true;
                }
                cameraSendMsg.sendCommandToSideCamera();
                VacUntil.sleep(50);
            }


            scanCodeData = getSideCamera(scanCodeData);
            scanCodeData =  getBelowCamera(scanCodeData);

            //旋转180 进入送药
            cabinetBStepPosition(CabinetConstants.CabinetBStepMode.ROTATE,SettingConstants.AngleDistance.RETURN);


        }

        if(("NoRead".equals(scanCodeData.getSideCode())&&(!Objects.equals(scanCodeData.getBelowCode(), "NoRead")))){
            scanCodeData.setCode(scanCodeData.getBelowCode());
        }else if(!Objects.equals(scanCodeData.getSideCode(), "NoRead")){
            scanCodeData.setCode(scanCodeData.getSideCode());
        }

        return scanCodeData;
    }

    //控制吸盘的输出
    public void suckerOutput(CabinetConstants.Cabinet  cabinet , int mode, CabinetConstants.OutPutCommand command){
        OutPutRequest outPutRequest = new OutPutRequest();
        outPutRequest.setCabinet(cabinet);
        outPutRequest.setWorkMode(cabinet);
        outPutRequest.setCommand(command);
        outPutRequest.setMode(mode);
        cabinetAService.outPut(outPutRequest);

    }


    //查询传感器 是否触发
    public void outPut(CabinetConstants.Cabinet cabinet,CabinetConstants.OutPutCommand command,int mode){

        OutPutRequest request = new OutPutRequest();
        request.setWorkMode(cabinet);
        request.setCabinet(cabinet);
        request.setCommand(command);
        request.setMode(mode);
        cabinetAService.outPut(request);

    }



    //查询传感器 是否触发
    public void intPut(CabinetConstants.Cabinet cabinet,CabinetConstants.InPutCommand command,int mode){
        InPutRequest request = new InPutRequest();
        request.setWorkMode(cabinet);
        request.setCabinet(cabinet);
        request.setCommand(command);
        request.setMode(mode);
        cabinetAService.intPut(request);

    }


    //B柜旋转步进电机回原
    public void cabinetBStepInit(){
        CabinetBStepRequest request = new CabinetBStepRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setMode(CabinetConstants.CabinetBStepMode.ROTATE);
        request.setCommand(CabinetConstants.CabinetBStepCommand.ZERO);
        request.setStatus(CabinetConstants.CabinetBStepStatus.ZERO);
        cabinetBService.step(request);
        waitCabinetBStepEnd();
    }






    //B柜伺服电机走到扫码位置
    public void cabinetBServoInit() throws IOException {
        valueOperations.set(RedisKeyConstant.CABINET_B_INIT,"false");
        ConfigData configData = configFunction.getAutoDrugConfigData();
        DistanceServoData distanceServoData = new DistanceServoData();
        distanceServoData.setServoZ(configData.getAboveScanZ());
        distanceServoData.setServoY(configData.getAboveScanY());
        distanceServoData.setServoX(configData.getAboveScanX());
        moveScanServo(distanceServoData,2);
        valueOperations.set(RedisKeyConstant.CABINET_B_INIT,"true");

    }


    /**
     * 等待步进电机运动完成
     */
    
    public void waitCabinetBStepEnd(){
        valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"false");
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_WAIT_TIME) {
            if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS))){
                break;
            }
            VacUntil.sleep(200);
        }
        log.info("===========================步进电机运动结束==========================");
    }



    /**
     * 等待步进电机运动完成
     */

    public void waitCabinetAStepEnd(int type){
        //夹爪步进电机
        if(type==1){
            valueOperations.set(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS,"false");
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_WAIT_TIME) {

                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS))){
                    break;
                }
                VacUntil.sleep(200);
            }
            log.info("===========================夹爪步进电机运动结束==========================");

        }else {
            valueOperations.set(RedisKeyConstant.CABINET_A_BLOCK_STEP_STATUS,"false");
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_WAIT_TIME) {

                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_A_BLOCK_STEP_STATUS))){
                    break;
                }
                VacUntil.sleep(200);
            }
            log.info("===========================挡片步进电机运动结束==========================");

        }
    }



    /**
     * 滑台反转
     */

    public void  tableReturn() throws IOException {

        log.info("回滚，重新上苗");

        //暂停自动上药流程
        autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);
        VacUntil.sleep(500);

        //滑台反转
        autoDrug(CabinetConstants.CabinetBApplyCommand.TABLE, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.REVERSAL);

        //等待2秒
        VacUntil.sleep(SettingConstants.RETURN_DRUG_TABLE_WAIT_TIME);

        //反转停止
        autoDrug(CabinetConstants.CabinetBApplyCommand.TABLE, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);

        autoInit();

        autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);

    }


    /**
     * 移动到掉药区 再滑台反转
     *
     */

    public void  servoTableReturn(ConfigData configData,String msg) throws IOException {
        log.warn(msg);
        DistanceServoData distanceServoData = new DistanceServoData();
        distanceServoData.setServoX(configData.getWasteX());
        distanceServoData.setServoY(configData.getWasteY());
        distanceServoData.setServoZ(0);
        moveScanServo(distanceServoData,2);
        distanceServoData.setServoZ(configData.getWasteZ());
        moveScanServo(distanceServoData,2);
        xiPangEnd();
        tableReturn();
    }

    /**
     * 掉药
     */
    public boolean  goDrop(DistanceServoData distanceServoData,ConfigData configData,ScanCodeData scanCodeData) throws IOException {
        DistanceServoData data = new DistanceServoData();
        if("false".equals(configData.getDropXAdd())){
            //掉药到机械手
            data.setServoX(configData.getDropX()-(distanceServoData.getVaccineWide()*100)/2);
        }else {
            data.setServoX(configData.getDropX()+(distanceServoData.getVaccineWide()*100)/2);
        }

        data.setServoY(configData.getDropY());
        data.setServoZ(configData.getDropZ()-distanceServoData.getVaccineHigh()*100);
        log.info("掉药区域位置：{}",data);
        return moveScanServo(data,1);
    }


    /**
     * 将扫码扫到的信息
     */
    public DrugRecordRequest findBox(DistanceServoData distanceServoData ,DrugRecordRequest request){

        //计算一个仓位最多能存储多少只药品  误差要加 5mm
        int num = getDrugNum(distanceServoData.getVaccineLong());
        log.info("药品：{}，最大支持几只：{}",request.getProductNo(),num);
        //确定是什么型号的仓柜
        List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(distanceServoData.getVaccineWide());

        List<Long> boxSpecIds = new ArrayList<>();
        for(VacBoxSpec vacBoxSpec:vacBoxSpecList){
            boxSpecIds.add(vacBoxSpec.getId());
        }
        log.info("符合的仓位规格id:{}",JSON.toJSONString(boxSpecIds));
        if (!boxSpecIds.isEmpty()) {
            //正常上药
            return vacMachineService.findBox(boxSpecIds,num ,request);

//            //优先装满整个机器 每个仓位一个
//            return vacMachineService.findBoxTest(boxSpecIds,num ,request);
        }else {
            return null;
        }

    }

    /**
     *
     * 如果机械手检测到传感器 默认已经送到仓位
     * @param request  status 1 正常上药 2 多人份上药
     *
     */

    public void addDrugRecord(DrugRecordRequest request , int status){
        //上药记录
        vacDrugRecordService.addDrugRecord(request);
        //仓位更新
        vacMachineService.updateBox(request,status);

    }


    //获取板子的长度
    public int getDrugNum(int vacLong){
        ConfigData configData = configFunction.getAutoDrugConfigData();
        int num = configData.getLineLong()/(vacLong+5);

        if(5*num>vacLong*1.5){
            num = num+1;
        }

        return num;
    }




    /**
     *
     * 如果机械手检测到传感器 默认已经送到仓位
     * @param request  status 1 正常上药 2 多人份上药
     *
     */

    public void addDrugRecordPeople(DrugRecordRequest request , int status ,int bulkNum){

        log.info("新增上药记录");
        //上药记录
        vacDrugRecordService.addDrugRecord(request);
        //仓位更新
        vacMachineService.updateBox(request,status);

        //增加散装数量记录
        vacMachineDrugService.addNum(request,bulkNum);

    }




    /**
     * led
     */

    public void led(Integer command,Integer ledNum,CabinetConstants.LedMode mode){

        LedRequest request = new LedRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setStatus(CabinetConstants.LedStatus.GREEN);
        request.setLedNum(ledNum);
        request.setMode(mode);
        request.setCommand(command);
        cabinetAService.ledCommand(request);
    }

    /**
     * 判断疫苗长宽高是否在库
     */

    public VaccineData drugIsRight(DistanceServoData data , String code ){
        VaccineData data1 = new VaccineData();
        LambdaQueryWrapper<VacDrug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
        .ge(VacDrug::getVaccineWide,data.getVaccineWide()-5)
        .le(VacDrug::getVaccineWide,data.getVaccineWide()+5)
        .ge(VacDrug::getVaccineHigh,data.getVaccineHigh()-5)
        .le(VacDrug::getVaccineHigh,data.getVaccineHigh()+5)
        .ge(VacDrug::getVaccineLong,data.getVaccineLong()-5)
        .le(VacDrug::getVaccineLong,data.getVaccineLong()+5)
        .eq(VacDrug::getDeleted,0);

        if(code!=null){
            queryWrapper.eq(VacDrug::getProductNo,code);
        }

        List<VacDrug> vacDrugList = vacDrugMapper.selectList(queryWrapper);

        if(vacDrugList.isEmpty()){
            data1.setIsRight(false);
            return data1;
        }else {
            log.info("识别到的药品：{}",vacDrugList);
            if(code!=null){
                data1.setDrugWide(vacDrugList.get(0).getVaccineWide());
                data1.setDrugHigh(vacDrugList.get(0).getVaccineHigh());
                data1.setDrugLong(vacDrugList.get(0).getVaccineLong());
                data1.setProductName(vacDrugList.get(0).getProductName());
            }
            data1.setIsRight(true);
            return data1;
        }
    }

    //判断机械手上是否有药
    public Boolean handHasDrug(){
        String sensorIsPut;
        log.info("检测机械手上是否有药");
        //查询一下机械手上是否有药 有药则直接报警
        intPut(CabinetConstants.Cabinet.CAB_A, CabinetConstants.InPutCommand.QUERY, SettingConstants.SENSOR_CABINET_A_HAND_NUM);
        VacUntil.sleep(200);
        sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);

        boolean handFlag = false;
        if (sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)) {
            int count = 1;
            handFlag = true;
            while (count < 3) {
                //机械手上有药-检查3次传感器状态都为触发-直接报警-药物回退-关闭自动上药
                intPut(CabinetConstants.Cabinet.CAB_A, CabinetConstants.InPutCommand.QUERY, SettingConstants.SENSOR_CABINET_A_HAND_NUM);
                VacUntil.sleep(200);
                sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
                assert sensorIsPut != null;
                if (sensorIsPut.equals(CabinetConstants.SensorStatus.RESET.code)) {
                    handFlag = false;
                    break;
                }
                count++;
            }


        }

        //如果有药返回 true
        return handFlag;
    }

    //判断药是否掉入机械手
    public Boolean dropHasDrug(ConfigData configData , Integer longs){

        boolean handFlag = true;
        String sensorIsPut = null;
        //判断机械手是否有药（传感器）
        long handTimeouts = System.currentTimeMillis();
        while ((System.currentTimeMillis() - handTimeouts) < SettingConstants.DRUG_HAVE_HAND_WAIT_TIME){
            //查询 机械手底部传感器信号
            intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
            VacUntil.sleep(200);

            //判断机械手底部传感器信号是否被触发
            sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
            assert sensorIsPut != null;
            if(sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                break;
            }

        }

        //5秒以后 还检测不到药掉再机械手上 异常报警
        assert sensorIsPut != null;
        if(!sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
            //可能是提前走的距离让药卡在机械手中央 先回0
            cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
            waitCabinetAStepEnd(1);
            VacUntil.sleep(200);
            CabinetAStepRequest stepRequest = new CabinetAStepRequest();
            stepRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
            stepRequest.setCommand(CabinetConstants.CabinetAStepCommand.POSITION);
            stepRequest.setMode(CabinetConstants.CabinetAStepMode.CLAMP);
            //正常位置模式  空出200
            stepRequest.setStatus(CabinetConstants.CabinetAStepStatus.ZERO);
            stepRequest.setDistance(configData.getHandLen()-longs-configData.getGap());
            cabinetAService.step(stepRequest);
            //等待夹爪步进电机运动完成
            waitCabinetAStepEnd(1);
            intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
            //判断机械手底部传感器信号是否被触发
            sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
            assert sensorIsPut != null;
            if(!sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                stepRequest.setDistance(0);
                cabinetAService.step(stepRequest);
                waitCabinetAStepEnd(1);

                stepRequest.setDistance(configData.getHandLen()-longs-configData.getGap());
                cabinetAService.step(stepRequest);
                waitCabinetAStepEnd(1);
                sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
                VacUntil.sleep(200);
                if(!sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                    //TODO 药物异常报警 写入数据库
                    handFlag =false;
                }


            }
        }
        //如果有药返回 true
        return handFlag;



    }


    public VacDrug drugDistance(VacDrug vacDrug) throws ExecutionException, InterruptedException, IOException {
        //移动B柜滑台
        CabinetBServoRequest cabinetBServoRequest = new CabinetBServoRequest();
        cabinetBServoRequest.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        cabinetBServoRequest.setCommand(CabinetConstants.CabinetBServoCommand.SPEED);
        cabinetBServoRequest.setMode(4);
        cabinetBServoRequest.setSpeed(400);
        cabinetBServoRequest.setStatus(CabinetConstants.CabinetBServoStatus.COROTATION);
        cabinetBService.servo(cabinetBServoRequest);
        ConfigData configData = configFunction.getAutoDrugConfigData();


        boolean flag =false;
        long timeout = System.currentTimeMillis();
        intPut(CabinetConstants.Cabinet.CAB_B,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_B_TABLE_NUM);
        VacUntil.sleep(200);
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

            intPut(CabinetConstants.Cabinet.CAB_B,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_B_TABLE_NUM);
            //检测传感器是否触发
            if("true".equals(valueOperations.get(RedisKeyConstant.sensor.TABLE_SENSOR))){
                log.info("测量药盒：传感器触发");
                flag = true;
                break;
            }
            VacUntil.sleep(200);
        }

        if (flag){

            //滑台暂停
            cabinetBServoRequest.setCommand(CabinetConstants.CabinetBServoCommand.PAUSE);
            cabinetBServoRequest.setStatus(CabinetConstants.CabinetBServoStatus.ZERO);
            cabinetBService.servo(cabinetBServoRequest);

            //打开3个距离传感器 计算距离
            DistanceServoData distanceServoData =distanceServoAll(configData);
            vacDrug.setVaccineLong(distanceServoData.getVaccineLong());
            vacDrug.setVaccineWide(distanceServoData.getVaccineWide());
            vacDrug.setVaccineHigh(distanceServoData.getVaccineHigh());

            disTableReturn();
            return vacDrug;

        }else {
            disTableReturn();
            return null;
        }

    }


    private void disTableReturn(){


        //药盒回退
        CabinetBServoRequest cabinetBServoRequest = new CabinetBServoRequest();
        cabinetBServoRequest.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        cabinetBServoRequest.setCommand(CabinetConstants.CabinetBServoCommand.SPEED);
        cabinetBServoRequest.setMode(4);
        cabinetBServoRequest.setSpeed(400);
        cabinetBServoRequest.setStatus(CabinetConstants.CabinetBServoStatus.REVERSAL);
        cabinetBService.servo(cabinetBServoRequest);
        VacUntil.sleep(4000);
        //滑台暂停
        cabinetBServoRequest.setCommand(CabinetConstants.CabinetBServoCommand.PAUSE);
        cabinetBServoRequest.setStatus(CabinetConstants.CabinetBServoStatus.ZERO);
        cabinetBService.servo(cabinetBServoRequest);
    }



}
