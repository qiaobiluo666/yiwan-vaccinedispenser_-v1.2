package com.yiwan.vaccinedispenser.system.dispensing;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.camera.CameraSendMsg;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReleaseResult;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReleaseOutcome;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationManager;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationRequest;
import com.yiwan.vaccinedispenser.system.dispensing.reservation.AutoDrugReservationResult;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import com.yiwan.vaccinedispenser.system.sys.data.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.test.UploadController;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/15 9:32
 */
@Slf4j
@Component
public class SendDrugFunction {

    /**
     * 单Windows服务内协调“DB读取+Redis预留”和“DB提交+Redis释放”，
     * 避免两个数据源之间用旧DB库存形成短暂错误空位。
     */
    private final Object autoDrugReservationInventoryLock = new Object();

    /** 自动上药允许连续回退的最大次数（包含测距尺寸不合规）。 */
    private static final int MAX_CONSECUTIVE_SCAN_RETURN_COUNT = 10;

    /** 上药机械手X轴坐标单位为0.1mm，每次仅补偿1mm。 */
    private static final int HAND_SERVO_X_COMPENSATION_STEP = 10;

    /** 仓位X坐标有误时最多补偿5次，累计不超过5mm。 */
    private static final int MAX_HAND_SERVO_X_COMPENSATION_STEPS = 5;

    /** 每次1mm补偿到位后，等待药盒稳定再读取传感器。 */
    private static final int HAND_SERVO_X_STABLE_WAIT_TIME = 2000;

    /** 药盒开始落仓后先等待8秒，仍未离手才启动伺服7补偿。 */
    private static final int HAND_SERVO_X_COMPENSATION_START_WAIT_TIME = 8000;

    /** 1mm短行程必须在3秒内到位，超时立即停止补偿。 */
    private static final int HAND_SERVO_X_MOVE_WAIT_TIME = 3000;

    /** 覆盖首次判断、5次补偿稳定等待以及机械手回原，避免下一盒在20秒时误判超时。 */
    private static final int HAND_DROP_PROCESS_WAIT_TIME = 45000;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private CabinetBService cabinetBService;

    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;

    @Autowired
    private UploadController uploadController;


    @Autowired
    private CameraSendMsg cameraSendMsg;


    @Autowired
    private VacBoxSpecService vacBoxSpecService;


    @Autowired
    private VacMachineService vacMachineService;

    @Autowired
    private AutoDrugReservationManager autoDrugReservationManager;


    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private ZcyFunction zcyFunction;


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

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

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

            if (!aboveCamera()) {
                throw new ServiceException("上方扫码相机连接断开，自动上药已停止");
            }
            //打开3个距离传感器 计算距离
            DistanceServoData distanceServoData =distanceServoAll(configData);

            //判断药盒形态
            if(distanceServoData.getIsReturn()) {
                // 测距阶段的尺寸不合规/传感器异常也是一次真实回退，
                // 必须纳入连续10次保护。此时药盒仍在滑台，不需要先走伺服回退位。
                returnAfterScan(configData, "测距阶段药盒尺寸不合规或传感器异常，退回", false);
                return;
            }

            String result = valueOperations.get(RedisKeyConstant.scanCode.ABOVE);
            if("NoRead".equals(result)){
                if (!aboveCamera()) {
                    throw new ServiceException("上方扫码相机连接断开，自动上药已停止");
                }
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
    public void sendDrug(ConfigData configData, ConfigSetting configSetting, String sessionId) throws Exception {

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
                returnAfterScan(configData,"上方扫码识别到多个条码,退回", false);
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
                returnAfterScan(configData,"滑台有药 退回");
                return;
            }

            //启动自动上药流程
            autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);

            //步进走零
            boolean stepStatus = cabinetBStepPosition(CabinetConstants.CabinetBStepMode.ROTATE,0);
            if(!stepStatus){
                String msg = "B柜步进电机异常";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,null,msg);
                sendDrugThreadManager.stop();
                return;
            }

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
                returnAfterScan(configData,"没扫码到码,退回");
                return;

            }

            final String traceId = UUID.randomUUID().toString();
            log.info("[自动上药追踪] traceId={}, 阶段=扫码完成, 监管码={}", traceId, scanCodeData.getCode());


            //TODO 根据政采云接口 拿到药盒的长宽高
            log.info("========================开始运动到掉药区域==============================");
            DrugRecordRequest drugRecordData = null;
            if("true".equals(configSetting.getZcyAuto())){
//            if("true".equals(isSendOpen)){
                //跟政采云扫码 获得 药品信息
                drugRecordData = zcyFunction.getVaccineMsgByCode(scanCodeData.getCode());
                log.info("拿到政采云疫苗信息：{}",JSON.toJSONString(drugRecordData));
            }else {
//                 drugRecordData = uploadController.getZcyCode(scanCodeData.getCode());
//                 if(drugRecordData==null){
//                     drugRecordData = new DrugRecordRequest();
//                     drugRecordData.setIsReturn(true);
//                 }

                if(scanCodeData.getCode().length()>7){
                    drugRecordData = vacDrugService.sendDrugTest(scanCodeData.getCode());
                    drugRecordData.setExpiredAt(Date.from(LocalDate.now().atStartOfDay()
                            .atZone(ZoneId.systemDefault()).toInstant()));
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
                returnAfterScan(configData,"电子监管码请求失败,退回");
                return;
            }

            //查看尺寸对不对
            VaccineData drugFlag = drugIsRight(distanceServoData,drugRecordData.getProductNo());
            if(!drugFlag.getIsRight()){
                log.error("{}尺寸不合规，回滚重发。测量到的长宽高：{} {} {} 数据库的长宽高：{} {} {}"
                        ,drugFlag.getProductName(),distanceServoData.getVaccineLong(),distanceServoData.getVaccineWide(),distanceServoData.getVaccineHigh(),
                        drugFlag.getDrugLong(),drugFlag.getDrugWide(),drugFlag.getDrugHigh());
//                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),msg);
                returnAfterScan(configData,"药品尺寸不合规，退回");
                return;
            }


            //选仓成功的同时在Redis逐盒预留容量，防止上一盒尚未写库时再次选择最后一个空位。
            ReservedDrugBox reservedBox = findAndReserveBox(
                    distanceServoData, drugRecordData, sessionId, traceId);
            DrugRecordRequest drugRecordRequest = reservedBox == null
                    ? null : reservedBox.drugRecordRequest();

            //如果为null 没有这个药的规格或者仓位满了
            if(drugRecordRequest==null){
                //TODO 没有仓位可以装这个药
                String msg = "自动上药异常：没有仓位可以装:"+drugRecordData.getProductName();
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),msg);
                returnAfterScan(configData,"没有仓位能装退回");
                return;
            }

            drugRecordRequest.setUuid(traceId);
            AtomicReference<ReservationCustody> reservationCustody =
                    new AtomicReference<>(ReservationCustody.RESERVED_ON_B_CABINET);
            boolean aHandTaskRegisteredByMain = false;

            try {

            //检测Redis 里面有没有这个批次的list 没有按照顺序添加


            String key = String.format(RedisKeyConstant.BATCH_NO_LIST, drugRecordRequest.getProductNo());
            String batchNo = drugRecordRequest.getBatchNo();

            // 获取列表所有元素
            List<String> list = listOps.range(key, 0, -1);
            if (list == null || !list.contains(batchNo)) {
                listOps.rightPush(key, batchNo);
            }


            //等机械手回原以后才能开始掉药
            boolean flag = false;
            timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < HAND_DROP_PROCESS_WAIT_TIME){
                // 自动上药已经停止时立即退出，避免仍在途的药盒继续等待状态位直至超时。
                if(!"true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                    return;
                }
                VacUntil.sleep(200);
                if("true".equals(valueOperations.get(RedisKeyConstant.HANDLE_IS_DROP))){
                    flag = true;
                    break;
                }
            }

            if(!flag){
                log.error("机械手45秒还没回原！机器异常，停止上药！");
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

            if("false".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                return;
            }

            // 提前走夹药一段距离
            int longs = distanceServoData.getVaccineWide()*100;
            if((configData.getHandLen()-longs-configData.getEarly())>0){
                int earlyDis = configData.getHandLen()-longs-configData.getEarly();
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,earlyDis);
            }else {
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
            }
            if("false".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                return;
            }
            //移动到掉药区域
            isNormal = goDrop(distanceServoData,configData,scanCodeData);

            if("false".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                return;
            }
            if(!isNormal){
                String msg = "伺服报警 退出移动到掉药区域！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                returnAfterScan(configData,"伺服报警,退回");
                return;
            }

            //从释放吸盘开始，药盒可能已经进入A柜机械手；后续任何异常都不能再安全释放预留。
            reservationCustody.set(ReservationCustody.TRANSFER_TO_A_UNCERTAIN);
            log.info("[自动上药追踪] traceId={}, 阶段=释放吸盘到A柜机械手, 仓位={}({}), 监管码={}",
                    traceId, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId(),
                    drugRecordRequest.getSupervisedCode());
            //吸盘放下
            xiPangEnd();
            //等待1S后 伺服回到初始位置
            VacUntil.sleep(200);

            //先回扫码位置 不然要撞到
            startBServoReturnThread(reservedBox.sessionId(), reservedBox.reservationId());

            //查看机械手上是否有药 有药继续
            boolean  dropDrugFlag= dropHasDrug(configData,longs);
            //机械手没药！重新开始上药
            if (!dropDrugFlag) {
                //传感器一直不触发 检查机械手是否有药、或者传感器损坏
                String errorMsg = String.format("药盒释放后A柜机械手传感器未确认接药，物理位置不确定");
                markReservationUncertain(reservedBox, errorMsg, null);
                reservationCustody.set(ReservationCustody.CLOSED);
                servoTableReturn(configData,errorMsg);

                sendDrugThreadManager.stop();
                return;
            }

            autoDrugReservationManager.markInTransit(reservedBox.reservationId())
                    .orElseThrow(() -> new IllegalStateException(
                            "已确认药盒进入A柜机械手，但Redis逐盒预留不存在：" + reservedBox.reservationId()));
            reservationCustody.set(ReservationCustody.IN_TRANSIT);
            log.info("[自动上药追踪] traceId={}, 阶段=A柜机械手接药确认, 仓位={}({}), DB库存={}, pending={}, 最大容量={}, AUTO_DRUG_START={}",
                    traceId, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId(),
                    reservedBox.databaseCountAtReserve(), reservedBox.pendingCountAfterReserve(),
                    reservedBox.maxCapacity(),
                    valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START));

            // 本次扫码后的上药流程没有发生回退，连续回退次数立即清零。
            resetConsecutiveScanReturnCount();

            //掉药到机械手 重新开一个线程
            // 必须在线程启动前登记。stop后只有等这个原始A柜子线程的finally完成，
            // 才允许下一轮自动上药启动。
            sendDrugThreadManager.onAHandFlowStarted(
                    reservedBox.sessionId(), reservedBox.reservationId());
            aHandTaskRegisteredByMain = true;
            Thread thread = new Thread(() -> {
                try {
                    valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"false");
                    valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"false");
                    log.info("[自动上药追踪] traceId={}, 阶段=机械手送仓开始, 仓位={}({}), 监管码={}, AUTO_DRUG_START={}",
                            traceId, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId(),
                            drugRecordRequest.getSupervisedCode(),
                            valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START));
                    valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");

                    //夹紧
                    int clampDis = configData.getHandLen()-longs-configData.getGap();
                    //走的行程
                    int dropDis = configData.getHandLen()-longs+200;

                    if(!dropDrugHandleAuto(clampDis,dropDis,drugRecordRequest)){
                        String errorMsg = "机械手送仓后底部传感器未确认药盒离手，物理位置不确定";
                        markReservationUncertain(reservedBox, errorMsg, null);
                        reservationCustody.set(ReservationCustody.CLOSED);
                        try {
                            sendDrugThreadManager.stop();
                        } finally {
                            valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
                            valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");
                        }
                        return;
                    }

                    // HAND_SENSOR=RESET 是真实库存入库的唯一物理依据。
                    // 从确认离手到数据库提交之间不得读取Redis、回原或执行其他可能失败的动作。
                    log.info("[自动上药追踪] traceId={}, 阶段=机械手底部传感器灭-物理离手确认, 仓位={}({})；立即提交真实库存",
                            traceId, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId());

                    synchronized (autoDrugReservationInventoryLock) {
                        try {
                            commitReservedDrug(reservedBox);
                        } catch (Throwable commitError) {
                            markReservationCommitFailed(reservedBox, commitError);
                            reservationCustody.set(ReservationCustody.CLOSED);
                            sendDrugThreadManager.stop();
                            return;
                        }

                        try {
                            releaseReservationAfterCommit(reservedBox);
                        } catch (Throwable releaseError) {
                            autoDrugReservationManager.tripSafetyInterlock(
                                    "数据库已记账但Redis逐盒预留未能确认释放，traceId="
                                            + reservedBox.reservationId(), releaseError);
                            reservationCustody.set(ReservationCustody.CLOSED);
                            notifyReservationAlarm(reservedBox,
                                    "数据库已成功记账，但Redis预留释放失败；已停止自动上药并保留安全占位",
                                    releaseError);
                            sendDrugThreadManager.stop();
                            return;
                        }
                    }
                    reservationCustody.set(ReservationCustody.CLOSED);

                    // 数据库真实库存和Redis预留都处理完后再执行设备回原。
                    // 回原失败只能触发收尾报警，不能影响已经确认离手的真实库存。
                    cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
                    waitCabinetAStepEnd(1);
                    cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
                    moveHandServoInit(configData);

                    //可以进入掉药区域
                    valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
                    log.info("[自动上药追踪] traceId={}, 阶段=完成, 仓位={}({}), 监管码={}",
                            traceId, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId(),
                            drugRecordRequest.getSupervisedCode());

                } catch (Throwable t) {
                    if (reservationCustody.get() != ReservationCustody.CLOSED) {
                        markReservationUncertain(reservedBox,
                                "机械手线程发生未捕获异常，药盒物理状态不确定", t);
                        reservationCustody.set(ReservationCustody.CLOSED);
                    } else {
                        notifyReservationAlarm(reservedBox,
                                "本盒已完成库存处理，但机械手收尾发生异常", t);
                    }
                    try {
                        sendDrugThreadManager.stop();
                    } catch (Exception e) {
                        log.error("[自动上药追踪] traceId={}, 停止自动上药失败", traceId, e);
                    }
                    try {
                        valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
                        valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");
                    } catch (Exception e) {
                        log.error("[自动上药追踪] traceId={}, Redis流程标志复位失败", traceId, e);
                    }
                } finally {
                    sendDrugThreadManager.onAHandFlowFinished(
                            reservedBox.sessionId(), reservedBox.reservationId());
                }
            }, "auto-drug-hand-" + traceId.substring(0, 8));

            thread.start();
            // Thread.start()成功后，活动任务的结束责任已交给子线程finally。
            aHandTaskRegisteredByMain = false;
            reservationCustody.compareAndSet(
                    ReservationCustody.IN_TRANSIT, ReservationCustody.CHILD_THREAD_OWNED);

            long timeoutInit = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeoutInit) < SettingConstants.CABINET_INIT_WAIT_TIME) {

                if(Objects.equals(valueOperations.get(RedisKeyConstant.CABINET_B_INIT), "true")){
                    break;
                }
                VacUntil.sleep(200);
            }
            //重新开始走发药皮带
            autoInit();

            } finally {
                if (aHandTaskRegisteredByMain) {
                    // Thread构造/start失败时没有子线程替我们完成计数，主线程必须归还登记。
                    sendDrugThreadManager.onAHandFlowFinished(
                            reservedBox.sessionId(), reservedBox.reservationId());
                }
                finishMainThreadReservationCustody(reservedBox, reservationCustody);
            }

        }
        else {
            log.info("还未开始上药");
        }
    }

    /**
     * 启动B柜伺服回扫码位线程。登记必须先于Thread.start；start失败时由当前线程归还，
     * start成功后则只能由子线程的finally归还。
     */
    private void startBServoReturnThread(String sessionId, String traceId) {
        sendDrugThreadManager.onBServoReturnFlowStarted(sessionId, traceId);
        boolean started = false;
        try {
            Thread thread = createBServoReturnThread(() -> {
                try {
                    valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_IS_RETURN,"false");
                    cabinetBServoInit();
                    valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_IS_RETURN,"true");
                } catch (Throwable returnError) {
                    log.error("[自动上药追踪] traceId={}, B柜伺服回扫码位线程异常",
                            traceId, returnError);
                } finally {
                    sendDrugThreadManager.onBServoReturnFlowFinished(sessionId, traceId);
                }
            }, "auto-drug-b-servo-return-" + traceId.substring(0, Math.min(8, traceId.length())));
            thread.start();
            started = true;
        } finally {
            if (!started) {
                sendDrugThreadManager.onBServoReturnFlowFinished(sessionId, traceId);
            }
        }
    }

    /** 仅作为线程构造点，便于验证Thread.start失败的计数补偿。 */
    Thread createBServoReturnThread(Runnable task, String name) {
        return new Thread(task, name);
    }


    //机械手挡片 夹药回原   掉药
    public boolean dropDrugHandleAuto(int clampDis , int dropDis ,DrugRecordRequest drugRecordRequest) throws IOException {

        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,clampDis);
        //等待夹爪步进电机运动完成
        waitCabinetAStepEnd(1);
        log.info("检测B柜伺服电机是否异常；当前药盒已在A柜机械手，停止按钮不再中断本盒");
        long timeouts ;
        boolean flag = false;
        timeouts = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeouts) < SettingConstants.DRUG_HAVE_HAND_WAIT_TIME){
            if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_IS_RETURN))){
                flag =true;
                break;
            }
            VacUntil.sleep(200);
        }
        if(!flag){
            String msg = "B柜伺服电机运动异常 停止上药";
            log.error(msg);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,null,msg);
            sendDrugThreadManager.stop();
            return false;
        }
        log.info("夹爪步进完成，开始等待B柜伺服回原");
        String autoStartFlag = valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START);
        log.info("本盒继续送仓，AUTO_DRUG_START={}，仓位={}({})",
                autoStartFlag, drugRecordRequest.getMachineNo(), drugRecordRequest.getMachineId());
        moveHandServo(drugRecordRequest.getAutoX(),drugRecordRequest.getAutoZ());

        //挡片步进电机旋转90
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.BLOCK,900);
        waitCabinetAStepEnd(2);

        //A柜机械手 步进电机走 往外走5mm
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,clampDis-200);
        waitCabinetAStepEnd(1);


        String sensorIsPuts= null;
        boolean inputFlag = false;
        List<Integer> servoXMoveTargets = null;
        int servoXMoveIndex = 0;
        boolean servoXCompensationUnavailable = false;
         timeouts = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeouts) < SettingConstants.WAIT_DROP_TIME){
            //查询 机械手底部传感器信号
            intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
            VacUntil.sleep(200);
            //判断机械手底部传感器信号是否被触发
            sensorIsPuts = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
            assert sensorIsPuts != null;
            //如果传感器触发 一直等待 不触发结束
            if(sensorIsPuts.equals(CabinetConstants.SensorStatus.RESET.code)){
                // 传感器灭即确认这一盒已经离开机械手。立即把控制权交给调用方提交数据库，
                // 此处不能再做回原或Redis读取，避免“实物已入仓但库存未记账”。
                return true;
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
            }

            if((System.currentTimeMillis() - timeouts)>HAND_SERVO_X_COMPENSATION_START_WAIT_TIME
                    && (System.currentTimeMillis() - timeouts)<SettingConstants.WAIT_DROP_TIME){
                if (servoXMoveTargets == null && !servoXCompensationUnavailable) {
                    Integer direction = getHandServoXMoveDirection(drugRecordRequest);
                    if (direction == null) {
                        servoXCompensationUnavailable = true;
                    } else {
                        servoXMoveTargets = buildHandServoXMoveTargets(
                                drugRecordRequest.getAutoX(), direction);
                    }
                }

                if (servoXMoveTargets != null && servoXMoveIndex < servoXMoveTargets.size()) {
                    int moveTargetX = servoXMoveTargets.get(servoXMoveIndex);
                    int currentStep = servoXMoveIndex + 1;
                    log.info("药盒未落入仓位，仓位:{}，上药X轴伺服7执行第{}/{}次补偿，目标坐标:{}（本次1mm）",
                            drugRecordRequest.getMachineNo(), currentStep,
                            MAX_HAND_SERVO_X_COMPENSATION_STEPS, moveTargetX);
                    if (!moveHandServoX(moveTargetX)) {
                        log.error("上药X轴伺服7补偿失败，仓位:{}，第{}次目标坐标:{}，未收到到位信号",
                                drugRecordRequest.getMachineNo(), currentStep, moveTargetX);
                        return false;
                    }
                    servoXMoveIndex++;
                    log.info("上药X轴伺服7第{}次1mm补偿已到位，等待{}ms后重新检测药盒传感器",
                            currentStep, HAND_SERVO_X_STABLE_WAIT_TIME);
                    VacUntil.sleep(HAND_SERVO_X_STABLE_WAIT_TIME);
                    // 回到循环顶部先读传感器；确认药盒离手就立即结束补偿，否则再走1mm。
                    continue;
                }

                if (servoXMoveTargets != null
                        && servoXMoveIndex >= MAX_HAND_SERVO_X_COMPENSATION_STEPS) {
                    log.error("仓位:{}的上药X轴伺服7已分5次累计补偿5mm，药盒仍未落入仓位，停止自动上药",
                            drugRecordRequest.getMachineNo());
                    return false;
                }

                //A柜 步进电机 回原
                cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
                waitCabinetAStepEnd(1);

                //夹紧
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,dropDis);
                waitCabinetAStepEnd(1);

                //震动电机
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
                inputFlag = true;

                //空出 1cm缝隙
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
                waitCabinetAStepEnd(1);
                VacUntil.sleep(500);


            }

//            //震动电机
//            if((System.currentTimeMillis() - timeouts)>10000 && inputFlag ){
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
//                inputFlag = false;
//            }


        }

        return false;

    }


    //机械手挡片 夹药回原   掉药
    public boolean dropDrugHandle(int clampDis , int dropDis ,DrugRecordRequest drugRecordRequest) throws IOException {

        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,clampDis);
        //等待夹爪步进电机运动完成
        waitCabinetAStepEnd(1);
        long timeouts ;
        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            boolean flag = false;
            timeouts = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeouts) < SettingConstants.DRUG_HAVE_HAND_WAIT_TIME){
                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_B_SERVO_IS_RETURN))){
                    flag =true;
                    break;
                }
                VacUntil.sleep(200);
            }
            if(!flag){
                String msg = "B柜伺服电机运动异常 停止上药";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,null,msg);
                sendDrugThreadManager.stop();
                return false;
            }
        }


        //运动伺服
        moveHandServo(drugRecordRequest.getAutoX(),drugRecordRequest.getAutoZ());

        //挡片步进电机旋转90
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.BLOCK,900);
        waitCabinetAStepEnd(2);

        //A柜机械手 步进电机走 往外走5mm
        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,clampDis-200);
        waitCabinetAStepEnd(1);


        String sensorIsPuts= null;
        boolean drugHand = false;
        boolean inputFlag = false;
        timeouts = System.currentTimeMillis();
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
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
                break;
            }

            if((System.currentTimeMillis() - timeouts)>3000&&(System.currentTimeMillis() - timeouts)<9000){
                //A柜 步进电机 回原
                cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
                waitCabinetAStepEnd(1);

                //夹紧
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,dropDis);
                waitCabinetAStepEnd(1);

                //震动电机
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
                inputFlag = true;

                //空出 1cm缝隙
                cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
                waitCabinetAStepEnd(1);
                VacUntil.sleep(500);


            }

//            //震动电机
//            if((System.currentTimeMillis() - timeouts)>10000 && inputFlag ){
//                suckerOutput(CabinetConstants.Cabinet.CAB_A,SettingConstants.CABINET_A_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
//                inputFlag = false;
//            }


        }

        if(!drugHand){
            log.error("自动上药异常：药物异常报警,药没掉入药仓");
            return false;
        }

        //回原
        //A柜步进电机回原
        cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
//        cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,0);
        waitCabinetAStepEnd(1);
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


    //B柜步进电机位置模式
    public boolean cabinetBStepPosition(CabinetConstants.CabinetBStepMode mode , int distance) throws IOException {
        CabinetBStepRequest stepRequests = new CabinetBStepRequest();
        stepRequests.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        stepRequests.setCommand(CabinetConstants.CabinetBStepCommand.POSITION);
        stepRequests.setMode(mode);
        stepRequests.setDistance(distance);
        stepRequests.setStatus(CabinetConstants.CabinetBStepStatus.ZERO);
        // 必须在发送前清零。若控制板快速回包后再清零，会覆盖已经收到的成功状态并误报超时。
        valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"false");
        cabinetBService.step(stepRequests);
        //等待B柜步进电机运动结束
        return waitCabinetBStepEnd();
    }



    public void xiPangStart(){

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_END_NUM,CabinetConstants.OutPutCommand.OUTPUT);
        VacUntil.sleep(200);

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_NUM,CabinetConstants.OutPutCommand.OUTPUT);
        VacUntil.sleep(200);

    }



    public void xiPangEnd(){

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
        VacUntil.sleep(200);

        suckerOutput(CabinetConstants.Cabinet.CAB_B,SettingConstants.CABINET_B_SUCKER_END_NUM,CabinetConstants.OutPutCommand.NOT_OUTPUT);
        VacUntil.sleep(200);


    }



    //移动A柜上药伺服
    public void moveHandServo(int x,int z){
        log.info("伺服运动距离：X:{} Z:{}",x,z);
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

    //移动A柜上药伺服X
    private boolean moveHandServoX(int x){
        valueOperations.set(RedisKeyConstant.handServo.X,"false");

        CabinetAServoRequest request = new CabinetAServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAServoCommand.POSITION);
        request.setMode(CabinetConstants.CabinetAServoMode.APPLY_SERVO_X.num);
        request.setStatus(CabinetConstants.CabinetAServoStatus.ZERO);
        request.setDistance(x);
        cabinetAService.servo(request);

        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < HAND_SERVO_X_MOVE_WAIT_TIME) {
            if("true".equals(valueOperations.get(RedisKeyConstant.handServo.X))){
                return true;
            }
            VacUntil.sleep(200);
        }
        return false;
    }

    //根据同层仓位的boxNo顺序，判断上药X轴伺服7的补偿方向
    private Integer getHandServoXMoveDirection(DrugRecordRequest request){
        if (request.getAutoX() == null) {
            log.warn("仓位:{}的autoX为空，无法执行上药X轴补偿", request.getMachineNo());
            return null;
        }

        List<VacMachine> machineList = vacMachineService.list(
                new LambdaQueryWrapper<VacMachine>()
                        .eq(VacMachine::getDeleted,0)
                        .eq(VacMachine::getLineNum,request.getLineNum())
                        .orderByAsc(VacMachine::getBoxNo));

        int currentIndex = -1;
        for (int i = 0; i < machineList.size(); i++) {
            if (Objects.equals(machineList.get(i).getBoxNo(), request.getMachineNo())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0 || machineList.size() < 2) {
            log.warn("仓位:{}无法根据同层仓位判断X轴移动方向", request.getMachineNo());
            return null;
        }

        int targetX = request.getAutoX();
        VacMachine referenceMachine;
        if (currentIndex < machineList.size() - 1) {
            referenceMachine = machineList.get(currentIndex + 1);
        } else {
            referenceMachine = machineList.get(currentIndex - 1);
        }

        if (referenceMachine.getAutoX() == null) {
            log.warn("参照仓位:{}的autoX为空，跳过X轴移动", referenceMachine.getBoxNo());
            return null;
        }

        int direction;
        if (currentIndex < machineList.size() - 1) {
            direction = Integer.compare(referenceMachine.getAutoX(), targetX);
        } else {
            direction = Integer.compare(targetX, referenceMachine.getAutoX());
        }

        if (direction == 0) {
            log.warn("仓位:{}与参照仓位:{}的autoX相同，跳过X轴移动", request.getMachineNo(), referenceMachine.getBoxNo());
            return null;
        }

        log.info("仓位:{}上药X轴补偿方向已确定，参照仓位:{}，起始坐标:{}，方向:{}",
                request.getMachineNo(), referenceMachine.getBoxNo(), targetX, direction);
        return direction;
    }

    static List<Integer> buildHandServoXMoveTargets(int startX, int direction){
        if (direction != -1 && direction != 1) {
            throw new IllegalArgumentException("上药X轴补偿方向只能为-1或1");
        }
        List<Integer> targets = new ArrayList<>(MAX_HAND_SERVO_X_COMPENSATION_STEPS);
        for (int step = 1; step <= MAX_HAND_SERVO_X_COMPENSATION_STEPS; step++) {
            targets.add(startX + direction * HAND_SERVO_X_COMPENSATION_STEP * step);
        }
        return targets;
    }

    //A柜机械手回零
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
            log.info("Z轴最后走！");
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

        } else if(type==2) {
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
        }else {
            //清港Z轴先走 走完再走Y 最后走X
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

                request.setDistance(distanceY);
                request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_Y.num);
                cabinetBService.servo(request);
                VacUntil.sleep(50);

                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

                    if ( "true".equals(valueOperations.get(RedisKeyConstant.scanServo.Y))) {
                        break;
                    }
                    VacUntil.sleep(200);
                }

                request.setMode(CabinetConstants.CabinetBServoMode.SCAN_SERVO_X.num);
                request.setDistance(distanceX);
                cabinetBService.servo(request);


                timeout = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {

                    if ("true".equals(valueOperations.get(RedisKeyConstant.scanServo.X)) ) {
                        break;
                    }
                    VacUntil.sleep(200);
                }
            }



        }

        //如果伺服报警 则显示3轴运动失败  true 运动正常 false 运动异常
        return servoNormalFlag;

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


        cabinetBStepInit();
        cabinetBServoInit();
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_RUN,"true");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"false");
//        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"false");


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
        data.setLeftDis(left);
        data.setRightDis(right);
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
        log.info(JSON.toJSONString(distanceServoData));
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

        //判断一下 药盒上方的高度 防止传感器误差导致将药盒夹碎
        if(high<=15){
            log.error("药盒没有低于15mm的高度 传感器有误差 药盒退回！");
            data.setIsReturn(true);
            return data;
        }

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

    public DistanceServoData distanceSerVoGetXY(ConfigData configData)throws ExecutionException, InterruptedException, IOException {
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
        data.setLeftDis(left);
        data.setRightDis(right);
        //移动中心点 测量激光
        DistanceServoData distanceServoData = new DistanceServoData();
        int moveX;
        int moveY ;

        if("true".equals(configSetting.getBFindX())){
            //计算中心点的XY走的距离
            data = VacUntil.findRectangleCenterX(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX();

        }else {
            data = VacUntil.findRectangleCenterY(left, right, configData.getTableAngle(), configData.getTableX(), configData.getTableY());
            moveX= data.getServoX();
        }
        moveY = data.getServoY();

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


        //走的高
        data.setServoZ(null);
        data.setVaccineWide(left);
        data.setVaccineLong(right);
        //将疫苗的长宽高和信息都存入redis
        return data;
    }

    //获取所有传感器参数
    public  DistanceServoData getDistanceSensor(){

        DistanceServoData distanceServoData = new DistanceServoData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        int count=0;
        Integer resultLeft = getDistanceLeft();
        Integer resultRight = getDistanceRight();
        Integer resultAbove = getDistanceHigh();
        Integer resultInventory = getDistanceCount();
        if(resultInventory!=null){
            resultInventory = resultInventory-configSetting.getInventoryCountValue();
        }



        //左右距离传感器会出现 多次给数据情况
        while (count<5){
            if(resultLeft==null || resultRight==null  || resultAbove==null  || resultInventory ==null){
                if(resultLeft==null){
                    log.error("B柜左方传感器异常");
                    resultLeft = getDistanceLeft();
                }else if(resultRight==null){
                    log.error("B柜右方传感器异常");
                    resultRight = getDistanceRight();
                }else if(resultAbove==null){
                    log.error("B柜上方传感器异常");
                    resultAbove = getDistanceHigh();
                }else {
                    log.error("库存盘点传感器异常");
                    resultInventory = getDistanceCount();
                    if(resultInventory==null){
                        resultInventory=0;
                    }
                    resultInventory = resultInventory-configSetting.getInventoryCountValue();
                }


            }else {
                break;
            }
            count++;
        }
        log.info("传感器数据：{}   误差数据：{}  ",resultInventory,configSetting.getInventoryCountValue());
        distanceServoData.setLeftDis(resultLeft);
        distanceServoData.setRightDis(resultRight);
        distanceServoData.setAboveDis(resultAbove);
        distanceServoData.setInventoryDis(resultInventory);
        return distanceServoData;
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

    /**
     *
     * @return
     * 上方扫码器
     */
    public boolean aboveCamera(){
        //运动到上方扫码伺服
        valueOperations.set(RedisKeyConstant.scanCode.ABOVE,"NoRead");
        return cameraSendMsg.sendCommandToAboveCamera();
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
        if (!cameraSendMsg.sendCommandToSideCamera()) {
            throw new ServiceException("侧方扫码相机连接断开，自动上药已停止");
        }
        scanCodeData = getSideCamera(scanCodeData);


        //下方扫码线程
        AtomicReference<Throwable> belowScanError = new AtomicReference<>();
        Thread belowScan = new Thread(() -> {
            try {
                long timeout = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_BELOW_WAIT_TIME) {
                    if (!cameraSendMsg.sendCommandToBelowCamera()) {
                        throw new ServiceException("下方扫码相机连接断开，自动上药已停止");
                    }
                    VacUntil.sleep(50);
                }
            } catch (Throwable scanError) {
                belowScanError.compareAndSet(null, scanError);
                log.error("自动上药下方相机扫码线程异常", scanError);
            }
        }, "auto-drug-below-scan");


        if("NoRead".equals(scanCodeData.getSideCode())){

            //下方扫码、侧方扫码开始
            boolean belowScanStarted = false;
            try {
                belowScan.start();
                belowScanStarted = true;
                boolean flag = false;
                long timeout = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_SIDE_WAIT_TIME) {
                    if (!flag){
                        request.setDistance(SettingConstants.AngleDistance.ALL);
                        cabinetBService.step(request);
                        flag = true;
                    }
                    if (!cameraSendMsg.sendCommandToSideCamera()) {
                        throw new ServiceException("侧方扫码相机连接断开，自动上药已停止");
                    }
                    VacUntil.sleep(50);
                }
            } finally {
                if (belowScanStarted) {
                    waitForThreadToFinishUninterruptibly(belowScan, "下方相机扫码线程");
                }
            }
            Throwable scanError = belowScanError.get();
            if (scanError != null) {
                if (scanError instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new ServiceException("下方扫码相机扫码线程异常，自动上药已停止", scanError);
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

    /**
     * 保证短时扫码子线程不会逃逸出rotateAngle方法。等待期间如收到中断，
     * 仍先等子线程结束，然后恢复主线程的中断标记。
     */
    private void waitForThreadToFinishUninterruptibly(Thread thread, String taskName) {
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException waitInterrupted) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            log.warn("等待{}期间收到中断；已在子线程结束后恢复中断标记", taskName);
        }
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
    public void cabinetBStepInit() throws IOException {
        CabinetBStepRequest request = new CabinetBStepRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_B);
        request.setMode(CabinetConstants.CabinetBStepMode.ROTATE);
        request.setCommand(CabinetConstants.CabinetBStepCommand.ZERO);
        request.setStatus(CabinetConstants.CabinetBStepStatus.ZERO);
        // 与位置模式相同，先清状态再发指令，避免快速成功回包被等待方法覆盖。
        valueOperations.set(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS,"false");
        cabinetBService.step(request);
        waitCabinetBStepEnd();
    }





    //B柜伺服电机走到扫码位置
    public void cabinetBServoInit() throws IOException {
        ConfigSetting configSetting = configFunction.getSettingConfigData();

        valueOperations.set(RedisKeyConstant.CABINET_B_INIT,"false");
        ConfigData configData = configFunction.getAutoDrugConfigData();
        DistanceServoData distanceServoData = new DistanceServoData();
        distanceServoData.setServoZ(configData.getAboveScanZ());
        distanceServoData.setServoY(configData.getAboveScanY());
        distanceServoData.setServoX(configData.getAboveScanX());

        if ("台州玉环清港".equals(configSetting.getHospitalName())){
            moveScanServo(distanceServoData,3);
        }else {
            moveScanServo(distanceServoData,2);
        }

        valueOperations.set(RedisKeyConstant.CABINET_B_INIT,"true");

    }


    /**
     * 等待步进电机运动完成
     */
    
    public boolean waitCabinetBStepEnd() throws IOException {

        boolean stepErrorFlag = true;
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_WAIT_TIME) {
            String stepStatus = valueOperations.get(RedisKeyConstant.CABINET_B_SCAN_STEP_STATUS);
            if("true".equals(stepStatus)){
                stepErrorFlag = false;
                break;
            }
            if ("error".equals(stepStatus)) {
                log.error("B柜步进电机已返回执行异常，立即停止等待");
                break;
            }
            VacUntil.sleep(200);
        }

        if(stepErrorFlag){
            String msg = "B柜步进电机运动异常 停止上药";
            log.error(msg);
            // 通用设备报警不带药品名，使用二参数接口按“编码+文案”直接去重。
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,msg);
            sendDrugThreadManager.stop();
        }else {
            log.info("===========================B柜步进电机运动结束==========================");
        }

        return !stepErrorFlag;
    }



    /**
     * 等待步进电机运动完成
     */

    public void waitCabinetAStepEnd(int type) throws IOException {
        boolean flag = false;
        //夹爪步进电机
        if(type==1){
            valueOperations.set(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS,"false");
            long timeout = System.currentTimeMillis();
            while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_STEP_WAIT_TIME) {

                if("true".equals(valueOperations.get(RedisKeyConstant.CABINET_A_CLAMP_STEP_STATUS))){
                    flag=true;
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
                    flag=true;
                    break;
                }
                VacUntil.sleep(200);
            }
            log.info("===========================挡片步进电机运动结束==========================");

        }
        log.info("步进电机fla:{}",flag);
        if(!flag){
            String msg;
            if(type==1){
                 msg = "A柜夹爪步进电机回原失败！自动上药停止";
            }else {
                 msg = "A柜挡片步进电机回原失败！自动上药停止";
            }

            log.error(msg);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,null,msg);
            sendDrugThreadManager.stop();
        }

    }



    /**
     * 滑台反转
     */

    public void  tableReturn() throws IOException {

        log.info("回滚，重新上苗");
        reverseTable();
        autoInit();
        autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.ZERO);

    }

    /**
     * 停止自动上药时使用的滑台退回。
     * 只完成退回动作，不再执行autoInit，也不重新启动自动上药，避免停止流程再次进入
     * B柜步进等待并递归调用stop。
     */
    public void tableReturnForStop() throws IOException {
        log.info("停止自动上药，滑台退回后不再重新初始化");
        reverseTable();
    }

    private void reverseTable() {
        //暂停自动上药流程
        autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);
        VacUntil.sleep(500);
        //滑台反转
        autoDrug(CabinetConstants.CabinetBApplyCommand.TABLE, CabinetConstants.CabinetBApplyMode.START, CabinetConstants.CabinetBApplyStatus.REVERSAL);
        //等待2秒
        VacUntil.sleep(SettingConstants.RETURN_DRUG_TABLE_WAIT_TIME);
        //反转停止
        autoDrug(CabinetConstants.CabinetBApplyCommand.TABLE, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);
    }


    /**
     * 移动到掉药区 再滑台反转
     *
     */

    public void  servoTableReturn(ConfigData configData,String msg) throws IOException {
        servoTableReturn(configData, msg, true);
    }

    /** 停止/异常关闭专用：完成退回后不执行autoInit，不重新启动自动上药。 */
    public void servoTableReturnForStop(ConfigData configData, String msg) throws IOException {
        servoTableReturn(configData, msg, false);
    }

    private void servoTableReturn(ConfigData configData, String msg, boolean restartAutoDrug) throws IOException {
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
            if( "false".equals(valueOperations.get(RedisKeyConstant.scanServo.X)) &&  "false".equals(valueOperations.get(RedisKeyConstant.scanServo.Y)) && "false".equals(valueOperations.get(RedisKeyConstant.scanServo.Z)) ){
                break;
            }
            VacUntil.sleep(200);
        }

        VacUntil.sleep(1000);
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        log.warn(msg);
        DistanceServoData distanceServoData = new DistanceServoData();
        distanceServoData.setServoX(configData.getWasteX());
        distanceServoData.setServoY(configData.getWasteY());
        distanceServoData.setServoZ(0);

        if("台州玉环清港".equals(configSetting.getHospitalName())){
            moveScanServo(distanceServoData,3);
        }else {
            moveScanServo(distanceServoData,2);
        }

        distanceServoData.setServoZ(configData.getWasteZ());

        if("台州玉环清港".equals(configSetting.getHospitalName())){
            moveScanServo(distanceServoData,3);
        }else {
            moveScanServo(distanceServoData,2);
        }

        xiPangEnd();
        if (restartAutoDrug) {
            tableReturn();
        } else {
            tableReturnForStop();
        }
    }

    /**
     * 记录一次自动上药的真实回退。连续达到上限时，当前药盒完成回退后停止自动上药并报警。
     */
    private void returnAfterScan(ConfigData configData, String reason) throws IOException {
        returnAfterScan(configData, reason, true);
    }

    private void returnAfterScan(ConfigData configData, String reason, boolean moveServoToReturnArea) throws IOException {
        if (moveServoToReturnArea) {
            servoTableReturn(configData, reason);
        } else {
            log.warn(reason);
            tableReturn();
        }

        long count = incrementConsecutiveScanReturnCount();
        log.warn("自动上药连续回退计数：{}/{}，Redis键={}，本次原因：{}",
                count, MAX_CONSECUTIVE_SCAN_RETURN_COUNT,
                RedisKeyConstant.CABINET_B_ERROR_COUNT, reason);

        if (count < MAX_CONSECUTIVE_SCAN_RETURN_COUNT) {
            return;
        }

        String alarmMsg = String.format(
                "自动上药连续%d次回退，已自动停止，请检查扫码相机、距离传感器、药盒尺寸及上药配置。本次原因：%s",
                MAX_CONSECUTIVE_SCAN_RETURN_COUNT, reason);
        log.error(alarmMsg);

        try {
            // 当前药盒已经退回，停止时不再执行第二次滑台回退。
            sendDrugThreadManager.stopAfterScanReturn();
        } finally {
            vacMachineExceptionService.sendException(
                    SettingConstants.MachineException.SEND.code, "", alarmMsg);
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.MACHINE_STATUS_COMMAND.getCode());
            commandData.put("data", alarmMsg);
            websocketService.sendInfo(CommandEnums.ON_CABINET_WEB.getCode(), commandData);
        }
    }

    /**
     * 当前项目的通用RedisTemplate会把字符串按JSON保存，逻辑值"0"在Redis底层并不是
     * INCR可识别的原生整数。连续回退只由自动上药主线程更新，因此在Java侧完成兼容
     * 读取、校验和写回，避免历史false/JSON数字导致主线程在第一次回退时直接异常退出。
     */
    private synchronized long incrementConsecutiveScanReturnCount() {
        String storedValue = valueOperations.get(RedisKeyConstant.CABINET_B_ERROR_COUNT);
        long currentCount = 0L;
        if (storedValue != null && !storedValue.trim().isEmpty()) {
            try {
                currentCount = Long.parseLong(storedValue.trim());
                if (currentCount < 0L) {
                    log.warn("连续回退计数Redis值小于0，已按0修复：key={}, oldValue={}",
                            RedisKeyConstant.CABINET_B_ERROR_COUNT, storedValue);
                    currentCount = 0L;
                }
            } catch (NumberFormatException invalidCount) {
                log.warn("连续回退计数Redis值不是数字，已按0修复：key={}, oldValue={}",
                        RedisKeyConstant.CABINET_B_ERROR_COUNT, storedValue);
                currentCount = 0L;
            }
        }

        long nextCount = currentCount >= MAX_CONSECUTIVE_SCAN_RETURN_COUNT
                ? MAX_CONSECUTIVE_SCAN_RETURN_COUNT : currentCount + 1L;
        valueOperations.set(RedisKeyConstant.CABINET_B_ERROR_COUNT, String.valueOf(nextCount));
        return nextCount;
    }

    private void resetConsecutiveScanReturnCount() {
        String count = valueOperations.get(RedisKeyConstant.CABINET_B_ERROR_COUNT);
        if (!"0".equals(count)) {
            valueOperations.set(RedisKeyConstant.CABINET_B_ERROR_COUNT, "0");
            log.info("本次扫码上药正常，连续回退计数清零，清零前计数：{}", count);
        }
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
        ConfigData configData = configFunction.getAutoDrugConfigData();
        //计算一个仓位最多能存储多少只药品  误差要加 5mm
        int num = getDrugNum(distanceServoData.getVaccineLong(), request);

        //根据初始位置拿到仓位的长宽高
        VacDrug vacDrug = getValidDrugForAutoLoading(request);
        if (vacDrug == null) {
            return null;
        }

        //确定是什么型号的仓柜
        List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(vacDrug.getVaccineWide());

        List<Long> boxSpecIds = new ArrayList<>();
        for(VacBoxSpec vacBoxSpec:vacBoxSpecList){
            boxSpecIds.add(vacBoxSpec.getId());
        }

        log.info("符合的仓位规格id:{}",JSON.toJSONString(boxSpecIds));
        if (!boxSpecIds.isEmpty()) {
            if("true".equals(configData.getSendDrugTest())){
                //优先装满整个机器 每个仓位一个
                return vacMachineService.findBoxTest(boxSpecIds,num ,request);
            }else {
                //正常上药
                return vacMachineService.findBox(boxSpecIds,num ,request);
            }

        }else {
            return null;
        }

    }

    /**
     * 选择仓位并立即逐盒预留。候选仓位如果已被在途药盒占满或身份冲突，
     * 会排除后继续寻找下一个仓位；Redis异常则直接失败关闭，绝不退化成只查数据库。
     */
    private ReservedDrugBox findAndReserveBox(DistanceServoData distanceServoData,
                                               DrugRecordRequest source,
                                               String sessionId,
                                               String traceId) {
        int maxCapacity = getDrugNum(distanceServoData.getVaccineLong(), source);
        if (maxCapacity <= 0) {
            log.error("[自动上药追踪] traceId={}, 阶段=选仓失败, 产品编码={}, 最大容量={}",
                    traceId, source.getProductNo(), maxCapacity);
            return null;
        }

        Set<Long> excludedMachineIds = new HashSet<>();
        while (true) {
            autoDrugReservationManager.assertSession(sessionId);
            DrugRecordRequest candidate;
            int databaseCount;
            AutoDrugReservationResult reservationResult;
            synchronized (autoDrugReservationInventoryLock) {
                candidate = findBoxExcludeMachineIds(
                        distanceServoData, source, excludedMachineIds);
                if (candidate == null || candidate.getMachineId() == null) {
                    log.warn("[自动上药追踪] traceId={}, 阶段=无可用仓位, 产品编码={}, 批号={}, 有效期={}, 已排除仓位={}",
                            traceId, source.getProductNo(), source.getBatchNo(),
                            source.getExpiredAt(), excludedMachineIds);
                    return null;
                }
                if (candidate.getAutoX() == null || candidate.getAutoZ() == null) {
                    log.warn("[自动上药追踪] traceId={}, 阶段=过滤空坐标仓位, 仓位={}({}), autoX={}, autoZ={}",
                            traceId, candidate.getMachineNo(), candidate.getMachineId(),
                            candidate.getAutoX(), candidate.getAutoZ());
                    excludedMachineIds.add(candidate.getMachineId());
                    continue;
                }

                VacMachine currentMachine = vacMachineService.getById(candidate.getMachineId());
                if (currentMachine == null || currentMachine.getDeleted() != 0) {
                    log.warn("[自动上药追踪] traceId={}, 候选仓位不存在或已删除, machineId={}",
                            traceId, candidate.getMachineId());
                    excludedMachineIds.add(candidate.getMachineId());
                    continue;
                }

                databaseCount = Optional.ofNullable(currentMachine.getVaccineNum()).orElse(0);
                reservationResult = autoDrugReservationManager.reserve(
                        AutoDrugReservationRequest.builder()
                                .reservationId(traceId)
                                .sessionId(sessionId)
                                .machineId(candidate.getMachineId())
                                .boxNo(candidate.getMachineNo())
                                .productNo(candidate.getProductNo())
                                .productName(candidate.getProductName())
                                .batchNo(candidate.getBatchNo())
                                .expiredAt(candidate.getExpiredAt())
                                .supervisedCode(candidate.getSupervisedCode())
                                .databaseCount(databaseCount)
                                .maxCapacity(maxCapacity)
                                .build());

                if (reservationResult.isReserved()) {
                    // 预留后再次读取DB，防御其他进程在“首次读取→Redis预留”之间改变仓位。
                    VacMachine latestMachine = vacMachineService.getById(candidate.getMachineId());
                    int latestDatabaseCount = latestMachine == null
                            ? Integer.MAX_VALUE
                            : Optional.ofNullable(latestMachine.getVaccineNum()).orElse(0);
                    boolean capacityStillValid = latestMachine != null
                            && latestMachine.getDeleted() == 0
                            && (long) latestDatabaseCount + reservationResult.getPendingCount()
                            <= maxCapacity;
                    boolean identityStillValid = latestMachine != null
                            && isMachineIdentityCompatibleForReservation(latestMachine, candidate);
                    boolean coordinatesStillValid = latestMachine != null
                            && latestMachine.getAutoX() != null
                            && latestMachine.getAutoZ() != null;
                    if (!capacityStillValid || !identityStillValid || !coordinatesStillValid) {
                        AutoDrugReleaseResult rollback =
                                autoDrugReservationManager.release(traceId);
                        if (rollback.getOutcome() != AutoDrugReleaseOutcome.RELEASED) {
                            IllegalStateException rollbackError = new IllegalStateException(
                                    "预留后二次校验失败，但Redis回滚未得到RELEASED结果：traceId="
                                            + traceId + "，outcome=" + rollback.getOutcome());
                            autoDrugReservationManager.tripSafetyInterlock(
                                    "预留后二次校验回滚结果异常，traceId=" + traceId,
                                    rollbackError);
                            throw rollbackError;
                        }
                        log.warn("[自动上药追踪] traceId={}, 阶段=预留后二次校验失败并回滚, 仓位={}({}), 最新DB库存={}, pending={}, 最大容量={}, 身份一致={}, 坐标完整={}, autoX={}, autoZ={}, releaseOutcome={}",
                                traceId, candidate.getMachineNo(), candidate.getMachineId(),
                                latestDatabaseCount, reservationResult.getPendingCount(),
                                maxCapacity, identityStillValid, coordinatesStillValid,
                                latestMachine == null ? null : latestMachine.getAutoX(),
                                latestMachine == null ? null : latestMachine.getAutoZ(),
                                rollback.getOutcome());
                        excludedMachineIds.add(candidate.getMachineId());
                        continue;
                    }
                    databaseCount = latestDatabaseCount;
                }
            }

            log.info("[自动上药追踪] traceId={}, 阶段=Redis预留结果, outcome={}, 仓位={}({}), 产品编码={}, 批号={}, 有效期={}, DB库存={}, 预留后pending={}, 最大容量={}",
                    traceId, reservationResult.getOutcome(), candidate.getMachineNo(),
                    candidate.getMachineId(), candidate.getProductNo(), candidate.getBatchNo(),
                    candidate.getExpiredAt(), databaseCount,
                    reservationResult.getPendingCount(), maxCapacity);

            if (reservationResult.isReserved()) {
                return new ReservedDrugBox(candidate, traceId, sessionId, databaseCount,
                        reservationResult.getPendingCount(), maxCapacity);
            }

            switch (reservationResult.getOutcome()) {
                case CAPACITY_FULL, MACHINE_PRODUCT_CONFLICT ->
                        excludedMachineIds.add(candidate.getMachineId());
                case RESERVATION_ID_CONFLICT -> throw new IllegalStateException(
                        "自动上药traceId发生Redis预留冲突：" + traceId);
                default -> throw new IllegalStateException(
                        "自动上药未知预留结果：" + reservationResult.getOutcome());
            }
        }
    }

    private boolean isMachineIdentityCompatibleForReservation(
            VacMachine machine, DrugRecordRequest candidate) {
        int databaseCount = Optional.ofNullable(machine.getVaccineNum()).orElse(0);
        if (databaseCount == 0 || machine.getProductNo() == null) {
            return true;
        }
        boolean batchCompatible = machine.getBatchNo() == null
                || Objects.equals(machine.getBatchNo(), candidate.getBatchNo());
        return Objects.equals(machine.getProductNo(), candidate.getProductNo())
                && batchCompatible
                && Objects.equals(machine.getExpiredAt(), candidate.getExpiredAt());
    }

    private void finishMainThreadReservationCustody(
            ReservedDrugBox reservedBox,
            AtomicReference<ReservationCustody> custody) {
        ReservationCustody current = custody.get();
        if (current == ReservationCustody.CHILD_THREAD_OWNED
                || current == ReservationCustody.CLOSED) {
            return;
        }

        if (current == ReservationCustody.RESERVED_ON_B_CABINET) {
            try {
                releaseReservationSafely(reservedBox,
                        "主流程在药盒进入A柜机械手前结束，预留已安全释放");
            } catch (Throwable releaseError) {
                autoDrugReservationManager.tripSafetyInterlock(
                        "药盒退回后Redis逐盒预留未能确认释放，traceId="
                                + reservedBox.reservationId(), releaseError);
                notifyReservationAlarm(reservedBox,
                        "药盒尚未进入A柜机械手，但Redis预留安全释放失败", releaseError);
                stopAutoDrugAfterReservationFailure();
            } finally {
                custody.set(ReservationCustody.CLOSED);
            }
            sendDrugThreadManager.onReservationFlowFinished(reservedBox.sessionId());
            return;
        }

        markReservationUncertain(reservedBox,
                "主流程在药盒释放到A柜机械手后异常结束，物理位置不确定", null);
        custody.set(ReservationCustody.CLOSED);
        stopAutoDrugAfterReservationFailure();
        sendDrugThreadManager.onReservationFlowFinished(reservedBox.sessionId());
    }

    private void releaseReservationSafely(ReservedDrugBox reservedBox, String reason) {
        AutoDrugReleaseResult releaseResult =
                autoDrugReservationManager.release(reservedBox.reservationId());
        if (releaseResult.getOutcome() != AutoDrugReleaseOutcome.RELEASED) {
            throw new IllegalStateException("安全释放预留未得到RELEASED结果：traceId="
                    + reservedBox.reservationId() + "，outcome="
                    + releaseResult.getOutcome());
        }
        log.info("[自动上药追踪] traceId={}, 阶段=安全释放预留, 仓位={}({}), 原因={}, outcome={}, 剩余pending={}",
                reservedBox.reservationId(), reservedBox.drugRecordRequest().getMachineNo(),
                reservedBox.drugRecordRequest().getMachineId(), reason,
                releaseResult.getOutcome(), releaseResult.getPendingCount());
    }

    private void commitReservedDrug(ReservedDrugBox reservedBox) {
        DrugRecordRequest request = reservedBox.drugRecordRequest();
        // 药盒已经确认离开机械手。此处先做唯一关键动作：事务写入真实库存和上药记录。
        // 任何Redis读取都只能用于提交后的诊断，不能让Redis瞬断阻断已离手药盒的记账。
        log.info("[自动上药追踪] traceId={}, 阶段=数据库提交开始, 仓位={}({}), 预留时DB库存={}, 预留后pending={}, 最大容量={}",
                reservedBox.reservationId(), request.getMachineNo(), request.getMachineId(),
                reservedBox.databaseCountAtReserve(),
                reservedBox.pendingCountAfterReserve(), reservedBox.maxCapacity());

        vacDrugRecordService.addAutoDrugRecordAndInventory(request, 1, reservedBox.maxCapacity());

        try {
            VacMachine after = vacMachineService.getById(request.getMachineId());
            long pending = autoDrugReservationManager.getPendingCount(request.getMachineId());
            String autoDrugStart = valueOperations.get(
                    RedisKeyConstant.autoDrug.AUTO_DRUG_START);
            log.info("[自动上药追踪] traceId={}, 阶段=数据库提交成功, 仓位={}({}), 入库后真实库存={}, 入库后可用库存={}, 当前pending={}, AUTO_DRUG_START={}, 产品编码={}, 批号={}, 有效期={}",
                    reservedBox.reservationId(), request.getMachineNo(), request.getMachineId(),
                    after == null ? null : after.getVaccineNum(),
                    after == null ? null : after.getVaccineUseNum(), pending, autoDrugStart,
                    request.getProductNo(), request.getBatchNo(), request.getExpiredAt());
        } catch (Throwable diagnosticError) {
            log.warn("[自动上药追踪] traceId={}, 仓位={}({})数据库已提交成功，但提交后诊断信息读取失败；不回滚已离手药盒记账",
                    reservedBox.reservationId(), request.getMachineNo(),
                    request.getMachineId(), diagnosticError);
        }
    }

    private void releaseReservationAfterCommit(ReservedDrugBox reservedBox) {
        AutoDrugReleaseResult result =
                autoDrugReservationManager.release(reservedBox.reservationId());
        if (result.getOutcome() != AutoDrugReleaseOutcome.RELEASED) {
            throw new IllegalStateException("数据库提交成功但预留未得到RELEASED结果：traceId="
                    + reservedBox.reservationId() + "，outcome=" + result.getOutcome());
        }
        log.info("[自动上药追踪] traceId={}, 阶段=数据库提交后释放预留, 仓位={}({}), outcome={}, 剩余pending={}",
                reservedBox.reservationId(), reservedBox.drugRecordRequest().getMachineNo(),
                reservedBox.drugRecordRequest().getMachineId(), result.getOutcome(),
                result.getPendingCount());
    }

    private void markReservationUncertain(
            ReservedDrugBox reservedBox, String reason, Throwable cause) {
        try {
            autoDrugReservationManager.markUncertain(reservedBox.reservationId(), reason)
                    .orElseThrow(() -> new IllegalStateException(
                            "需要标记UNCERTAIN时预留已不存在"));
        } catch (Throwable redisError) {
            autoDrugReservationManager.tripSafetyInterlock(
                    "UNCERTAIN预留状态无法写入Redis，traceId="
                            + reservedBox.reservationId(), redisError);
            if (cause == null) {
                cause = redisError;
            } else {
                cause.addSuppressed(redisError);
            }
        }
        notifyReservationAlarm(reservedBox,
                reason + "；Redis预留保留并阻止再次启动", cause);
    }

    private void markReservationCommitFailed(
            ReservedDrugBox reservedBox, Throwable commitError) {
        String reason = "机械手底部传感器已确认药盒离手，但数据库库存/上药记录事务提交失败";
        try {
            autoDrugReservationManager.markCommitFailed(reservedBox.reservationId(), reason)
                    .orElseThrow(() -> new IllegalStateException(
                            "需要标记COMMIT_FAILED时预留已不存在"));
        } catch (Throwable redisError) {
            autoDrugReservationManager.tripSafetyInterlock(
                    "COMMIT_FAILED预留状态无法写入Redis，traceId="
                            + reservedBox.reservationId(), redisError);
            commitError.addSuppressed(redisError);
        }
        notifyReservationAlarm(reservedBox,
                reason + "；Redis预留保留并阻止再次启动", commitError);
    }

    private void notifyReservationAlarm(
            ReservedDrugBox reservedBox, String reason, Throwable cause) {
        DrugRecordRequest request = reservedBox.drugRecordRequest();
        long pending;
        try {
            pending = autoDrugReservationManager.getPendingCount(request.getMachineId());
        } catch (Throwable ignored) {
            pending = -1L;
        }
        String message = String.format(
                "自动上药保护报警：traceId=%s，仓位=%s(%s)，药品=%s，产品编码=%s，批号=%s，有效期=%s，监管码=%s，预留时DB库存=%d，当前pending=%d，最大容量=%d，原因=%s",
                reservedBox.reservationId(), request.getMachineNo(), request.getMachineId(),
                request.getProductName(), request.getProductNo(), request.getBatchNo(),
                request.getExpiredAt(), request.getSupervisedCode(),
                reservedBox.databaseCountAtReserve(), pending,
                reservedBox.maxCapacity(), reason);
        if (cause == null) {
            log.error(message);
        } else {
            log.error(message, cause);
        }

        try {
            RedisDrugListData exceptionData = new RedisDrugListData();
            exceptionData.setMachineId(request.getMachineId());
            exceptionData.setBoxNo(request.getMachineNo());
            exceptionData.setLineNum(request.getLineNum());
            exceptionData.setProductName(request.getProductName());
            vacMachineExceptionService.dropException(
                    SettingConstants.MachineException.SEND.code, exceptionData, message);
        } catch (Throwable alarmError) {
            log.error("[自动上药追踪] traceId={}, 写入异常管理失败",
                    reservedBox.reservationId(), alarmError);
        }

        try {
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.MACHINE_STATUS_COMMAND.getCode());
            commandData.put("data", message);
            websocketService.sendInfo(CommandEnums.ON_CABINET_WEB.getCode(), commandData);
        } catch (Throwable websocketError) {
            log.error("[自动上药追踪] traceId={}, 前端报警推送失败",
                    reservedBox.reservationId(), websocketError);
        }
    }

    private void stopAutoDrugAfterReservationFailure() {
        try {
            sendDrugThreadManager.stop();
        } catch (Throwable stopError) {
            log.error("自动上药预留异常后停止流程失败", stopError);
        }
    }

    private enum ReservationCustody {
        RESERVED_ON_B_CABINET,
        TRANSFER_TO_A_UNCERTAIN,
        IN_TRANSIT,
        CHILD_THREAD_OWNED,
        CLOSED
    }

    private record ReservedDrugBox(
            DrugRecordRequest drugRecordRequest,
            String reservationId,
            String sessionId,
            int databaseCountAtReserve,
            long pendingCountAfterReserve,
            int maxCapacity) {
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

    /**
     * 查找仓位（排除已预留的machineId，用于PLC模式防并发）
     */
    public DrugRecordRequest findBoxExcludeMachineIds(DistanceServoData distanceServoData, DrugRecordRequest request, Set<Long> excludeMachineIds) {
        ConfigData configData = configFunction.getAutoDrugConfigData();
        int num = getDrugNum(distanceServoData.getVaccineLong(), request);
        //根据初始位置拿到仓位的长宽高
        VacDrug vacDrug = getValidDrugForAutoLoading(request);
        if (vacDrug == null) {
            return null;
        }

        List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(vacDrug.getVaccineWide());
        List<Long> boxSpecIds = new ArrayList<>();
        for (VacBoxSpec vacBoxSpec : vacBoxSpecList) {
            boxSpecIds.add(vacBoxSpec.getId());
        }

        log.info("符合的仓位规格id(排除预留):{}", JSON.toJSONString(boxSpecIds));
        if (!boxSpecIds.isEmpty()) {
            if ("true".equals(configData.getSendDrugTest())) {
                return vacMachineService.findBoxTestExcludeMachineIds(
                        boxSpecIds, num, request, excludeMachineIds);
            } else {
                return vacMachineService.findBoxExcludeMachineIds(boxSpecIds, num, request, excludeMachineIds);
            }
        }
        return null;
    }

    //获取板子的长度
    public int getDrugNum(int vacLong,DrugRecordRequest request){
        ConfigData configData = configFunction.getAutoDrugConfigData();
        int num = configData.getLineLong()/(vacLong+5);

        if(5*num>vacLong*1.5){
            num = num+1;
        }

        //查询疫苗最大存储数量
        //这里只读取最大存储数量，不参与尺寸校验；尺寸由后续选仓专用查询校验。
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());
        Integer maxNum = vacDrug == null ? null : vacDrug.getMaxNum();

        //取最小值
        if(maxNum!=null){
            log.info("疫苗：{},最大限制数量：{}",request.getProductName(),maxNum);
            num = Math.min(num, maxNum);
        }

        log.info("药品：{}，最大支持几只：{}",request.getProductNo(),num);

        return num;

    }

    private VacDrug getValidDrugForAutoLoading(DrugRecordRequest request) {
        VacDrug vacDrug = vacDrugService.vacDrugGetValidDimensionsByProductNo(request.getProductNo());
        if (vacDrug != null
                && vacDrug.getVaccineLong() != null && vacDrug.getVaccineLong() > 0
                && vacDrug.getVaccineWide() != null && vacDrug.getVaccineWide() > 0
                && vacDrug.getVaccineHigh() != null && vacDrug.getVaccineHigh() > 0) {
            return vacDrug;
        }

        String msg = String.format(
                "自动上药药品尺寸异常：产品编码=%s，药品名称=%s，vaccineId=%s，长=%s，宽=%s，高=%s，已停止为该药品分配仓位。",
                request.getProductNo(),
                request.getProductName(),
                vacDrug == null ? null : vacDrug.getId(),
                vacDrug == null ? null : vacDrug.getVaccineLong(),
                vacDrug == null ? null : vacDrug.getVaccineWide(),
                vacDrug == null ? null : vacDrug.getVaccineHigh());
        log.error(msg);
        vacMachineExceptionService.sendException(
                SettingConstants.MachineException.SEND.code,
                request.getProductName() == null ? "" : request.getProductName(),
                msg);
        return null;
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
        log.info(JSON.toJSONString(vacDrugList));
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
    public Boolean dropHasDrug(ConfigData configData , Integer longs) throws IOException {

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
