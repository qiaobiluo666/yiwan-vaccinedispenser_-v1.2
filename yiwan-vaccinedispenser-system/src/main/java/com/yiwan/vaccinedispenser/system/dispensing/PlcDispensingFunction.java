package com.yiwan.vaccinedispenser.system.dispensing;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetDispenseRequest;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetInventoryRequest;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetManualFeedRequest;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSendData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.StepDisConfig;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.until.VacUntil;

import java.util.stream.Collectors;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@ConditionalOnExpression("${plc.enable:true}")
public class PlcDispensingFunction {

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

    @Autowired
    @Lazy
    private PlcSendService plcSendService;

    @Value("${plc.handle-mode}")
    private boolean handleMode;



    //plc 添加队列
    public void plcAddDrugList(VacGetVaccine vacGetVaccine) throws Exception{


        //最后一个发药结束10分钟后再关门
        valueOperations.set(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME, LocalDateTime.now().toString());

        ConfigSetting configSetting = configFunction.getSettingConfigData();

        //机器正在疫苗退回 不发药
        if("true".equals(valueOperations.get(RedisKeyConstant.DRUG_RETURN))){
            zcyFunction.sendResult(vacGetVaccine,"正在疫苗退回，无法发药！");
            throw new ServiceException("正在疫苗退回，无法发药！");
        }

        //机器正在疫苗退回 不发药
        if("true".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
            zcyFunction.sendResult(vacGetVaccine,"正在异常清理，无法发药！");
            throw new ServiceException("正在异常清理，无法发药！");
        }


        //TODO 检查是否有应急按钮没有复位


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
                    zcyFunction.sendResult(vacGetVaccine,msg);
                }
                throw new ServiceException(msg);

            }else if((Objects.equals(code,SettingConstants.MachineException.CONTROLLER.code))&&"PLC".equals(data.getDrugName())){

                String msg = "PLC设备掉线！无法正常发药";
                log.error(msg);
                if("true".equals(configSetting.getZcySend())){
                    zcyFunction.sendResult(vacGetVaccine,msg);
                }

                throw new ServiceException(msg);

            }
        }
        log.info("BoxNoList:{}",boxNoList);
        log.info("lineNumList:{}",lineNumList);
        //符合仓位的列表
        List<VacMachine>  drugList = null;
        //过滤掉 设备异常 导致的仓位 或者皮带问题
        LambdaQueryWrapper<VacMachine> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacMachine::getDeleted,"0")
                .gt(VacMachine::getVaccineUseNum,0)
                .in(VacMachine::getStatus, 1, 2);
        if (vacGetVaccine.getProductNoList() != null && !vacGetVaccine.getProductNoList().isEmpty()) {
            lambdaQueryWrapper.in(VacMachine::getProductNo, vacGetVaccine.getProductNoList());
        } else {
            lambdaQueryWrapper.eq(VacMachine::getProductNo, vacGetVaccine.getProductNo());
        }
        if(!boxNoList.isEmpty()){
            boxNoList.removeIf(Objects::isNull);
            if(!boxNoList.isEmpty()){
                lambdaQueryWrapper.notIn(VacMachine::getBoxNo,boxNoList);
            }
        }

        if(!lineNumList.isEmpty()){
            lineNumList.removeIf(Objects::isNull);
            if(!lineNumList.isEmpty()) {
                lambdaQueryWrapper.notIn(VacMachine::getLineNum, lineNumList);
            }
        }

        drugList =  vacMachineMapper.selectList(lambdaQueryWrapper);


        //////
        /// 新修改的批次逻辑
        //////////

        assert drugList != null;
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
            //获取发苗仓位
            VacMachine vacMachine = dispensingFunction.getDrugVacMachineMsg(drugList);

            int realBeltLine = (vacMachine.getLineNum() + 1) / 2;
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
            //将皮带层数存入redis
            redisDrugListData.setBeltNum(realBeltLine);
            redisDrugListData.setUuid(UUID.randomUUID());

            redisDrugListData.setVaccineNum(vacMachine.getVaccineNum());
            redisDrugListData.setVaccineUseNum(vacMachine.getVaccineUseNum());
            //将发药数据存入数据库
            listOps.rightPush(RedisKeyConstant.PLC_SEND_LIST,JSON.toJSONString(redisDrugListData));
            listOps.rightPush(RedisKeyConstant.BELT_LIST,JSON.toJSONString(redisDrugListData));

            //发药处方加入redis
            listOps.rightPush(RedisKeyConstant.SEND_LIST,JSON.toJSONString(redisDrugListData));

            List<String> dropList = listOps.range(RedisKeyConstant.PLC_SEND_LIST, 0, -1);
            List<String> sendList = listOps.range(RedisKeyConstant.SEND_LIST, 0, -1);

            log.info("dropList   总数：{} {}",listOps.size(RedisKeyConstant.PLC_SEND_LIST),JSON.toJSONString(dropList));
            log.info("sendList   总数：{} {}",listOps.size(RedisKeyConstant.SEND_LIST),JSON.toJSONString(sendList));

            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_START.getCode());
            commandData.put("data", redisDrugListData);
            websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);

            log.info("添加发药处方：{}",JSON.toJSONStringWithDateFormat(
                    redisDrugListData,
                    "yyyy-MM-dd HH:mm:ss",
                    SerializerFeature.WriteDateUseDateFormat
            ));
        }

    }


    //plc发药流程
    public void plcSendDrug(){
        try {
            ConfigSendData configSendData = configFunction.getSendDrugConfigData();
            //开始发药流程
            valueOperations.set(RedisKeyConstant.DRUG_RUN_START, "true");

            //拿到发药信息
            String moveMsgStr = listOps.index(RedisKeyConstant.PLC_SEND_LIST, 0);
            RedisDrugListData drugListData = JSON.parseObject(moveMsgStr, RedisDrugListData.class);
            assert drugListData != null;

            if ("true".equals(valueOperations.get(RedisKeyConstant.statusWord3.A_TASK_RECEIVED))) {
                ACabinetDispenseRequest aCabinetDispenseRequest = new ACabinetDispenseRequest();

                //单发版发苗
                if (handleMode) {
                    log.info("给PLC发送单发版发药指令");
                    StepDisConfig stepDisConfig = vacMachineService.getStepDisByProduct(drugListData.getProductNo());
                    aCabinetDispenseRequest.setLineNum(drugListData.getLineNum());
                    aCabinetDispenseRequest.setPositionNum(drugListData.getPositionNum());
                    aCabinetDispenseRequest.setWorkbenchNum(drugListData.getWorkbenchNum());
                    aCabinetDispenseRequest.setStepLiftDistance(stepDisConfig.getStepLiftDis());
                    aCabinetDispenseRequest.setStepExtendDistance(stepDisConfig.getStepExtendDis());
                } else {
                    log.info("给PLC发送电磁铁版发药指令");
                    //电磁铁款发苗
                    //根据疫苗 拿到IO时间
                    Integer ioTime = configSendData.getIoWaitTime();
                    Integer lastIoAddTime = configSendData.getLastIOAddTime();
                    ioTime = vacMachineService.getVacIoTimeByProduct(drugListData.getProductNo(), ioTime);
                    VacMachine vacMachine = vacMachineService.getMachineByBoxNo(drugListData.getBoxNo());
                    if (vacMachine != null) {
                        if (vacMachine.getVaccineNum() <= 1) {
                            ioTime = ioTime + lastIoAddTime;
                        }
                    }
                    aCabinetDispenseRequest.setLineNum(drugListData.getLineNum());
                    aCabinetDispenseRequest.setPositionNum(drugListData.getPositionNum());
                    aCabinetDispenseRequest.setWorkbenchNum(drugListData.getWorkbenchNum());
                    aCabinetDispenseRequest.setIoTime(ioTime);
                }

                //发送送苗信息
                plcSendService.sendACabinetDispense(aCabinetDispenseRequest);
                //保存当前发送的药品信息（供皮带异常重发使用）
                valueOperations.set(RedisKeyConstant.PLC_CURRENT_SEND_DRUG, JSON.toJSONString(drugListData));
                //去除一条发药信息+
                listOps.leftPop(RedisKeyConstant.PLC_SEND_LIST);
                //送药指令下发
                plcSendService.sendACabinetDispenseCmd();

            }
        } catch (Exception e) {
            log.error("[PLC] 发药流程异常 | {}", e.getMessage(), e);
        }
    }


    /**
     * PLC模式：A柜盘苗（库存盘点）
     * <p>
     * 1. 清除上次盘点结果及残留状态
     * 2. 写入盘苗层号/序号/偏移量到寄存器12-14
     * 3. 发送控制字3 bit1 启动盘苗
     *
     * @param layer  A柜层号 (1-10)
     * @param index  A柜序号 (1-24)
     * @param offset 偏移量 (1mm, 上下仓+-3mm保护)
     */
    public void plcInventory(Integer layer, Integer index, Integer offset) {
        try {
            //1. 清除上次盘点结果及残留状态
            valueOperations.set(RedisKeyConstant.PLC_INVENTORY_DISTANCE, "");
            //2. 写入盘苗坐标
            ACabinetInventoryRequest request = new ACabinetInventoryRequest();
            request.setLayer(layer);
            request.setIndex(index);
            request.setOffset(offset);
            plcSendService.sendACabinetInventory(request);
        } catch (Exception e) {
            log.error("[PLC] 盘苗异常 | 层={} 序号={} 偏移={} | {}", layer, index, offset, e.getMessage(), e);
        }
    }

    /**
     * PLC模式：A柜盘苗状态清除
     */
    public void plcInventoryClear() {
        plcSendService.sendACabinetInventoryClear();
    }

    /**
     * PLC模式：A柜手动上苗
     * <p>
     * 1. 写入层号/序号/药盒宽度到寄存器15-17
     * 2. 发送控制字3 bit2 启动手动上苗
     *
     * @param layer A柜层号 (1-10)
     * @param index A柜序号 (1-24)
     * @param width 药盒宽度 (1mm)
     */
    public void plcManualFeed(Integer layer, Integer index, Integer width) {
        try {
            //1. 写入手动上苗坐标
            ACabinetManualFeedRequest request = new ACabinetManualFeedRequest();
            request.setLayer(layer);
            request.setIndex(index);
            request.setWidth(width);
            plcSendService.sendACabinetManualFeed(request);
            VacUntil.sleep(100);

            //2. 启动手动上苗
            plcSendService.sendACabinetManualFeed();

        } catch (Exception e) {
            log.error("[PLC] 手动上苗异常 | 层={} 序号={} 宽度={} | {}", layer, index, width, e.getMessage(), e);
        }
    }


    public void addBoxDrugAgain(RedisDrugListData drugDataList) {
        try {
            //清除传送带redis 状态  之前还在的队列 正常发药
            boolean flag = true;
            List<String> sendDataList = listOps.range(RedisKeyConstant.PLC_SEND_LIST, 0, -1);
            assert sendDataList != null;
            for (String data : sendDataList) {
                //当队列到达这个处方时
                RedisDrugListData drugListData = JSON.parseObject(data, RedisDrugListData.class);
                //俩个uuid相同
                if (ObjectUtil.equals(drugListData.getUuid(), drugDataList.getUuid())) {
                    flag = false;
                }
                //后续这个仓位的发药list 全部清空
                if (Objects.equals(drugListData.getPositionNum(), drugDataList.getPositionNum()) && Objects.equals(drugListData.getLineNum(), drugDataList.getLineNum()) && !flag) {
                    listOps.remove(RedisKeyConstant.PLC_SEND_LIST, 1, data);
                }

            }

            //先将该仓位的掉药列表数据清楚
            //后续如果还有这个仓位发药 则重新发
            List<String> beltDataList = listOps.range(RedisKeyConstant.SEND_LIST, 0, -1);

            if (beltDataList != null) {
                for (String beltData : beltDataList) {
                    try {
                        RedisDrugListData drugData = JSON.parseObject(beltData, RedisDrugListData.class);
                        //后续还有这个仓位的掉药记录删除
                        if (Objects.equals(drugData.getPositionNum(), drugDataList.getPositionNum()) && Objects.equals(drugData.getLineNum(), drugDataList.getLineNum())) {
                            listOps.remove(RedisKeyConstant.SEND_LIST, 1, beltData);
                            VacGetVaccine vacGetVaccine = new VacGetVaccine();
                            BeanUtils.copyProperties(drugData, vacGetVaccine);
                            log.info("开始重新发药！");
                            log.info("vacGetVaccine:{}",JSON.toJSONString(vacGetVaccine));
                            //重新发药
                            plcAddDrugList(vacGetVaccine);
                        }
                    } catch (Exception e) {
                        log.error("[PLC] 重发药异常 | beltData={} | {}", beltData, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[PLC] 重发药异常 | {}", e.getMessage(), e);
        }
    }

}
