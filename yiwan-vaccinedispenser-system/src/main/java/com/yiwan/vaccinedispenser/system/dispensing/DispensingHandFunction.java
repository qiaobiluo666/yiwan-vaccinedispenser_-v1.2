package com.yiwan.vaccinedispenser.system.dispensing;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSendData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetAHandRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 78671
 */
@Slf4j
@Component
public class DispensingHandFunction {


    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Autowired
    private VacMachineMapper vacMachineMapper;

    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private CabinetCService cabinetCService;


    @Autowired
    private VacMachineExceptionMapper vacMachineExceptionMapper;


    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private VacMachineService vacMachineService;

    @Autowired
    private VacSendDrugRecordService vacSendDrugRecordService;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private WebsocketService websocketService;


    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private VacMachineDrugService vacMachineDrugService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    @Autowired
    private DispensingFunction dispensingFunction;





    /**
     * 条形码 扫码以后用政采云的数据 实现发药
     */
    public void addDrugList(VacGetVaccine vacGetVaccine) throws Exception {

        //最后一个发药结束10分钟后再关门
        valueOperations.set(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME, LocalDateTime.now().toString());
        ConfigSetting configSetting = configFunction.getSettingConfigData();

        //机器正在疫苗退回 不发药
        if("true".equals(valueOperations.get(RedisKeyConstant.DRUG_RETURN))){
            zcyFunction.sendResult(vacGetVaccine,"正在疫苗退回，无法发药！");
            throw new ServiceException("正在疫苗退回，无法发药！");
        }


        //抬升装置版本的C柜 如果复位按钮没有复位则 发不出来药
        if("true".equals(configSetting.getCLifting())){
            int workNum = vacGetVaccine.getWorkbenchNum();
            dispensingFunction.findCabinetReset(workNum);
            VacUntil.sleep(200);
            String isReset = valueOperations.get(String.format(RedisKeyConstant.CABINET_C_RESET,workNum));
            if("true".equals(isReset)){
                String msg = vacGetVaccine.getWorkbenchName()+"复位按钮没有复原";
                log.error(msg);
                if("true".equals(configSetting.getZcySend())){
                    zcyFunction.sendResult(vacGetVaccine,"复位按钮还没有复原，请回原后重新发药!");
                    vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDWARING.code,null,msg);
                }
                throw new ServiceException(msg);
            }
        }

        //TODO 检查A柜皮带、伺服 是否报警
        //异常情况不处理完成 不进入发药
        List<VacMachineException> machineExceptionList = vacMachineExceptionMapper.selectList(new LambdaQueryWrapper<VacMachineException>().eq(VacMachineException::getDeleted,0));
        List<String> boxNoList = new ArrayList<>();
        List<Integer> lineNumList = new ArrayList<>();

        for(VacMachineException data :machineExceptionList){
            int code = data.getCode();
            //电磁铁报警
            if(Objects.equals(code, SettingConstants.MachineException.IO.code)){
                boxNoList.add(data.getBoxNo());

            }else if(Objects.equals(code,SettingConstants.MachineException.BELT.code)||(Objects.equals(code,SettingConstants.MachineException.SERVO.code))){
                //皮带报警 或者伺服报警 整层禁用
                lineNumList.add(data.getLineNum());
                //抬升伺服报警 无法发药！
            }else if(Objects.equals(code,SettingConstants.MachineException.SENDDRUG.code)){
                String msg = "发药伺服异常！无法正常发药";
                log.error(msg);
                if("true".equals(configSetting.getZcySend())){
                    vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDDRUG.code,null,msg);
                    zcyFunction.sendResult(vacGetVaccine,"机器发药异常！请联系售后");
                }
            }
        }

        //过滤掉 设备异常 导致的仓位 或者皮带问题
        LambdaQueryWrapper<VacMachine> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacMachine::getDeleted,"0")
                .eq(VacMachine::getProductNo,vacGetVaccine.getProductNo())
                .gt(VacMachine::getVaccineUseNum,0)
                .in(VacMachine::getStatus, 1, 2);
        if(!boxNoList.isEmpty()){
            lambdaQueryWrapper.notIn(VacMachine::getBoxNo,boxNoList);
        }

        if(!lineNumList.isEmpty()){
            lambdaQueryWrapper.notIn(VacMachine::getLineNum,lineNumList);
        }

        //先判断该药品是否有余量
        List<VacMachine>  drugList =  vacMachineMapper.selectList(lambdaQueryWrapper);

        if(drugList.isEmpty()){

            if("true".equals(configSetting.getZcySend())){
                zcyFunction.sendResult(vacGetVaccine,"机器没有库存！");
            }
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
            commandData.put("data", vacGetVaccine);
            websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
            String msg = String.format("%s 可使用量不足，请装药！",vacGetVaccine.getProductName());
            RedisDrugListData redisDrugListDataError = new RedisDrugListData();
            redisDrugListDataError.setProductNo(vacGetVaccine.getProductNo());
            vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDWARING.code,redisDrugListDataError,msg);
            throw new ServiceException(msg);

        }else {

            //多人份疫苗先发--->近效期--->先上药的批次--->数量最少的仓--->层数最低的

            // 多人份疫苗先发  选取有效期最早的
            Optional<VacMachine> priorityVacMachine = drugList.stream()
                    .filter(drug -> drug.getStatus() == 2)
                    .min(Comparator.comparing(VacMachine::getExpiredAt));

            VacMachine vacMachine;
            if (priorityVacMachine.isPresent()) {
                // 直接选用 `status == 2` 的疫苗
                vacMachine = priorityVacMachine.get();
            } else {

                // 找到有效期最近的疫苗
                List<VacMachine> nearestExpiryDrugList = drugList.stream()
                        .filter(drug -> drug.getExpiredAt() != null)
                        .sorted(Comparator.comparing(VacMachine::getExpiredAt))
                        .toList();

                // 获取最近的有效期
                Date nearestExpiryDate = nearestExpiryDrugList.get(0).getExpiredAt();

                // 筛选出所有有效期等于最近有效期的疫苗
                List<String> distinctBatchNos = nearestExpiryDrugList.stream()
                        .filter(drug -> drug.getExpiredAt().equals(nearestExpiryDate))
                        .map(VacMachine::getBatchNo)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                        .stream()
                        .toList();


                List<VacMachine> drugsWithNearestExpiry;
                //如果批号为空 直接按照有效期发苗
                if(distinctBatchNos.isEmpty()){
                    drugsWithNearestExpiry = nearestExpiryDrugList.stream()
                            .filter(drug -> drug.getExpiredAt().equals(nearestExpiryDate))
                            .toList();

                }else {
                    //通过疫苗列表 查早 最早的记录
                    String batchNo = vacDrugRecordService.getBatchNoEarly(distinctBatchNos);
                    if(batchNo!=null){
                        drugsWithNearestExpiry = nearestExpiryDrugList.stream()
                                .filter(drug -> drug.getExpiredAt().equals(nearestExpiryDate))
                                .filter(drug -> Objects.equals(drug.getBatchNo(), batchNo))
                                .toList();
                    }else {
                        drugsWithNearestExpiry = nearestExpiryDrugList.stream()
                                .filter(drug -> drug.getExpiredAt().equals(nearestExpiryDate))
                                .toList();
                    }
                }


                // 2. 如果有效期最近的疫苗有多个仓位，再根据皮带队列大小选择
                if (drugsWithNearestExpiry.size() > 1) {
                    List<VacMachine> sortedList = drugsWithNearestExpiry.stream()
                            .sorted(Comparator.comparingInt(VacMachine::getVaccineNum)
                                    .thenComparingInt(VacMachine::getLineNum))
                            .toList();
                    vacMachine = sortedList.get(0);
                } else {
                    // 如果只有一个有效期最近的仓位，直接选择
                    vacMachine = drugsWithNearestExpiry.get(0);
                }

            }

            //药品可使用量-1
            vacMachine.setVaccineUseNum(vacMachine.getVaccineUseNum()-1);
            //可用数量-1
            vacMachineMapper.updateById(vacMachine);

            RedisDrugListData redisDrugListData = new RedisDrugListData();
            BeanUtils.copyProperties(vacMachine,redisDrugListData);
            redisDrugListData.setMachineStatus(vacMachine.getStatus());
            redisDrugListData.setBoxNo(vacMachine.getBoxNo());
            redisDrugListData.setProductName(vacMachine.getProductName());
            redisDrugListData.setMachineId(vacMachine.getId());
            //将发药工作台存入
            redisDrugListData.setTaskId(vacGetVaccine.getTaskId());
            redisDrugListData.setRequestNo(vacGetVaccine.getRequestNo());
            redisDrugListData.setWorkbenchNum(vacGetVaccine.getWorkbenchNum());
            redisDrugListData.setWorkbenchNo(vacGetVaccine.getWorkbenchNo());
            redisDrugListData.setWorkbenchName(vacGetVaccine.getWorkbenchName());


            redisDrugListData.setUuid(UUID.randomUUID());
            //将发药数据存入数据库
            listOps.rightPush(RedisKeyConstant.DROP_HAND_LIST, JSON.toJSONString(redisDrugListData));

            //发药处方加入redis
            listOps.rightPush(RedisKeyConstant.SEND_LIST,JSON.toJSONString(redisDrugListData));


            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_START.getCode());
            commandData.put("data", redisDrugListData);
            websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);

            //机器配有挡片 则检查挡片是否开启
            if("true".equals(configSetting.getCBlank())){
                //开启挡片
                Thread thread = new Thread(() -> dispensingFunction.openBlank());
                thread.start();
            }
            log.info("添加发药处方：{}",JSON.toJSONStringWithDateFormat(
                    redisDrugListData,
                    "yyyy-MM-dd HH:mm:ss",
                    SerializerFeature.WriteDateUseDateFormat
            ));


        }
    }


    //机械手掉药
    public void dropHandDrugs() throws Exception {
        //获取list
        String drugStr = listOps.index(RedisKeyConstant.DROP_HAND_LIST, 0);
        String dropStart = valueOperations.get(RedisKeyConstant.handMachine.HAND_DROP_START);
        if (drugStr != null) {
            assert dropStart != null;
            if ("true".equals(dropStart)) {

                valueOperations.set(RedisKeyConstant.handMachine.HAND_DROP_START,"false");
                RedisDrugListData drugListData = JSON.parseObject(drugStr, RedisDrugListData.class);
                //发送拿药指令
                String isDrop = dropServo(drugListData);
                //去除队列
                if ("empty".equals(isDrop)) {
                    //重新发药
                    String errorMsg = String.format("仓位：%s 疫苗名称：%s  未检测到药品，重新发药", drugListData.getBoxNo(), drugListData.getProductName());
                    log.error(errorMsg);
                    vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDWARING.code, drugListData, errorMsg);
                    //禁用该仓位
                    vacMachineService.vacMachineIOById(drugListData.getMachineId(), 0);
                    //重新发药
                    addHandleDrugAgain(drugListData);
                    valueOperations.set(RedisKeyConstant.handMachine.HAND_DROP_START,"true");
                } else if ("success".equals(isDrop)) {
                    //掉药到C柜
                    dispensingFunction.dropRecordAndMachine(drugListData, 1, "发药正常");
                    moveToC(drugListData);
                }

            }
        }

    }

    public void handServo(CabinetConstants.CabinetAHandCommand command,Integer servoX,Integer servoZ,Integer distance ){

        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        CabinetAHandRequest request = new CabinetAHandRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(command);
        request.setServoX(SettingConstants.CABINET_A_HANDLE_SERVO_X);
        request.setDistanceX(servoX);
        request.setServoZ(SettingConstants.CABINET_A_HANDLE_SERVO_Z);
        request.setDistanceZ(servoZ);
        request.setDistance(distance);

        if(command.num==1){
            request.setStepDistance(configSendData.getHandDropStepDis());
        }

        cabinetAService.handGetDrug(request);

    }


    //取药
    public String dropServo(RedisDrugListData drugListData ) throws Exception {
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        valueOperations.set(RedisKeyConstant.handMachine.HAND_DROP_STATUS,"false");
        //运动伺服
        handServo(CabinetConstants.CabinetAHandCommand.FIND,drugListData.getDropX(),drugListData.getDropZ(),configSendData.getHandUpDistance());

        long timeout = System.currentTimeMillis();
        //等待A柜下药动作反馈
        while ((System.currentTimeMillis() - timeout) < SettingConstants.HAND_DROP_WAIT_TIMEOUT) {
            String status = valueOperations.get(RedisKeyConstant.handMachine.HAND_DROP_STATUS);
                if("success".equals(status)){
                    log.info("下药成功");
                    return "success";
                }else if("empty".equals(status)){
                    log.warn("空仓");
                    return "empty";
                }else if ("error".equals(status)){
                    log.error("设备故障");
                    return "error";
                }
                VacUntil.sleep(100);
        }

        return "timout";
    }


    //取药
    public void moveToC(RedisDrugListData drugListData ){
        valueOperations.set(RedisKeyConstant.handMachine.HAND_MOVE_STATUS,"false");
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        handServo(CabinetConstants.CabinetAHandCommand.DROP,configSendData.getHandMoveCX(),configSendData.getHandMoveCZ(),configSendData.getHandMoveCStepDis());
        long timeout = System.currentTimeMillis();
        String result ;
        //等待A柜下药动作反馈
        while ((System.currentTimeMillis() - timeout) < SettingConstants.HAND_DROP_WAIT_TIMEOUT) {
            result = valueOperations.get(RedisKeyConstant.handMachine.HAND_MOVE_STATUS);
            if(!"false".equals(result)){
                log.info(result);
                break;
            }
            VacUntil.sleep(100);
        }

        listOps.leftPop(RedisKeyConstant.DROP_HAND_LIST);
        valueOperations.set(RedisKeyConstant.handMachine.HAND_DROP_START,"true");
//        //发药成功
//        if("success".equals(result)){
//            dispensingFunction.moveBeltToC(drugListData,configSetting);
//        }

    }


    //数据清空 重新发药
    public void addHandleDrugAgain(RedisDrugListData drugDataList) throws Exception {
        //清除传送带redis 状态  之前还在的队列 正常发药
        boolean flag = true;
        List<String> sendDataList = listOps.range(RedisKeyConstant.SEND_LIST,0,-1);
        assert sendDataList != null;
        for(String data : sendDataList){
            //当队列到达这个处方时
            RedisDrugListData drugListData = JSON.parseObject(data, RedisDrugListData.class);
            //俩个uuid相同
            if(ObjectUtil.equals(drugListData.getUuid(),drugDataList.getUuid())){
                flag =false;
            }
            //后续这个仓位的发药list 全部清空
            if(Objects.equals(drugListData.getPositionNum(), drugDataList.getPositionNum())&&Objects.equals(drugListData.getLineNum(),drugDataList.getLineNum())&&!flag){
                listOps.remove(RedisKeyConstant.SEND_LIST,1,data);
            }

        }

        //先将该仓位的掉药列表数据清楚
        //后续如果还有这个仓位发药 则重新发
        List<String> beltDataList = listOps.range(RedisKeyConstant.DROP_HAND_LIST,0,-1);

        if(beltDataList != null){
            for(String beltData : beltDataList){
                try {
                    RedisDrugListData drugData = JSON.parseObject(beltData, RedisDrugListData.class);
                    //后续还有这个仓位的掉药记录删除
                    if(Objects.equals(drugData.getPositionNum(), drugDataList.getPositionNum())&&Objects.equals(drugData.getLineNum(),drugDataList.getLineNum())){
                        listOps.remove(RedisKeyConstant.DROP_HAND_LIST,1,beltData);
                        VacGetVaccine vacGetVaccine = new VacGetVaccine();
                        BeanUtils.copyProperties(drugData,vacGetVaccine);
                        //重新发药
                        addDrugList(vacGetVaccine);
                    }
                }catch (Exception ignored){

                }

            }
        }


    }
}
