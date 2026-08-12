package com.yiwan.vaccinedispenser.system.plc.data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.dispensing.PlcDispensingFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.plc.model.BCabinetSendDrugRequest;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.VaccineData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 寄存器解析结果分发器
 * <p>
 * 直接根据 PlcRegisterTable 枚举常量分发, IDE 中 Ctrl+单击可直接跳转到枚举定义
 * 后续业务处理直接在各 case 中扩展
 *
 * @author yiwan
 */
@Slf4j
@Component
@ConditionalOnExpression("${plc.enable:true}")
public class PlcStatusDispatcher {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private HashOperations<String, String, String> hashOps;

    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    @Lazy
    private PlcSendService plcSendService;

    @Autowired
    private ZcyFunction zcyFunction;
    @Autowired
    private VacDrugService vacDrugService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private VacMachineService vacMachineService;


    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Autowired
    private PlcDispensingFunction plcDispensingFunction;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    /** 报警触发时记录待读取的报警编号地址及对应报警定义 */
    private final Map<Integer, AlarmBitTable> pendingAlarmCodes = new HashMap<>();

    /** 报警编号读取专用线程（不阻塞 Netty IO 线程） */
    private final ExecutorService alarmCodeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "t_alarm_code");
        t.setDaemon(true);
        return t;
    });

    public void dispatch(List<RegisterInfo> registerList) {
        if (registerList == null || registerList.isEmpty()) {
            return;
        }
        pendingAlarmCodes.clear();
        Map<PlcRegisterTable, RegisterInfo> registerMap = new EnumMap<>(PlcRegisterTable.class);
        for (RegisterInfo info : registerList) {
            if (info != null && info.getMeta() != null) {
                registerMap.put(info.getMeta(), info);
            }
        }
        for (RegisterInfo info : registerList) {
            try {
                dispatchSingle(info, registerMap);
            } catch (Exception e) {
                log.error("[PLC] 寄存器分发异常 | address={} | {}", info != null ? info.getAddress() : "null", e.getMessage(), e);
            }
        }
        //所有寄存器分发完成后，提交到独立线程读取报警编号（不阻塞 Netty IO 线程）
        for (Map.Entry<Integer, AlarmBitTable> entry : pendingAlarmCodes.entrySet()) {
            int addr = entry.getKey();
            AlarmBitTable alarm = entry.getValue();
            alarmCodeExecutor.submit(() -> readAlarmCodeAndLog(addr, alarm));
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void dispatchSingle(RegisterInfo info, Map<PlcRegisterTable, RegisterInfo> registerMap) {
        if (info == null || info.getMeta() == null) {
            return;
        }
        try {
            String lengthStr;
            String widthStr;
            String heightStr;

            switch (info.getMeta()) {
            case STATUS_WORD_1:
                //解析状态字1：bit2=上苗运行中
                if (((info.getRawValue() >> StatusWord1.FEED_RUNNING.getBit()) & 1) == 0) {
                    valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false");
//                    log.info("[PLC] 状态字1: 上苗停止");
                }
                break;
            case STATUS_WORD_2:
                //解析状态字2：bit0=长宽厚请求判断  bit1=条码判断请求
                if (((info.getRawValue() >> StatusWord2.JUDGE_REQUEST.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusRequest.JUDGE_REQUEST, "true");
                    log.info("[PLC] 状态字2: 长宽厚请求判断");
                } else {
                    valueOperations.set(RedisKeyConstant.statusRequest.JUDGE_REQUEST, "false");
                }
                if (((info.getRawValue() >> StatusWord2.BARCODE_REQUEST.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusRequest.BARCODE_REQUEST, "true");
                    log.info("[PLC] 状态字2: 条码判断请求");
                } else {
                    valueOperations.set(RedisKeyConstant.statusRequest.BARCODE_REQUEST, "false");
                }
                break;
            case STATUS_WORD_3:
                //解析状态字3：bit0=A柜_收到任务  bit1=A柜_盘苗完成  bit3=A柜_进药完成  bit4=A柜_退苗完成
                //bit6=A柜允许退苗 bit7=A柜退苗运行中 bit11=A柜允许异常清苗 bit12=A柜异常清苗运行中 bit13=A柜异常清苗完成 bit14=A柜异常清苗出苗异常
                if (((info.getRawValue() >> StatusWord3.A_TASK_RECEIVED.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_TASK_RECEIVED, "false");

                } else {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_TASK_RECEIVED, "true");
                }

                if (((info.getRawValue() >> StatusWord3.A_INVENTORY_DONE.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_INVENTORY_DONE, "true");
                    log.info("[PLC] 状态字3: A柜_盘苗完成");
                    plcDispensingFunction.plcInventoryClear();

                } else {
//                    log.info("[PLC] 状态字3: A柜_盘苗完成 为0");
                    valueOperations.set(RedisKeyConstant.statusWord3.A_INVENTORY_DONE, "false");
                }


                if (((info.getRawValue() >> StatusWord3.A_FILL_DONE.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_FILL_DONE, "true");
                    log.info("[PLC] 状态字3: A柜_进药完成");
                    //从Redis队列取上药信息Json（先进先出）
                    String msg = listOps.leftPop(RedisKeyConstant.PLC_SEND_DRUG_MSG);
                    if (msg != null) {
                        DrugRecordRequest drugRecordRequest = JSON.parseObject(msg, DrugRecordRequest.class);
                        log.info("上药信息：{}", JSON.toJSONString(drugRecordRequest));
                        //释放仓位预留
                        releaseMachine(drugRecordRequest.getMachineId());
                        //机械手上有药，仓位药品数量+1，新增上药记录
                        sendDrugFunction.addDrugRecord(drugRecordRequest, 1);
                    }

                    //发送上药完成应答 清除状态
                    plcSendService.sendACabinetFillAck();


                } else {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_FILL_DONE, "false");
                }

                if (((info.getRawValue() >> StatusWord3.A_RETURN_DONE.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_RETURN_DONE, "true");
                    log.info("[PLC] 状态字3: A柜_退苗完成，清除地址60");
                    plcSendService.clearACabinetReturn();
                    valueOperations.set(RedisKeyConstant.PLC_RETURN_DONE, "true");
                } else {
                    valueOperations.set(RedisKeyConstant.statusWord3.A_RETURN_DONE, "false");
                }
                valueOperations.set(RedisKeyConstant.statusWord3.A_RETURN_ALLOWED,
                        isStatusBitSet(info, StatusWord3.A_RETURN_ALLOWED) ? "true" : "false");
                valueOperations.set(RedisKeyConstant.statusWord3.A_RETURN_RUNNING,
                        isStatusBitSet(info, StatusWord3.A_RETURN_RUNNING) ? "true" : "false");
                valueOperations.set(RedisKeyConstant.statusWord3.A_ABNORMAL_CLEAN_ALLOWED,
                        isStatusBitSet(info, StatusWord3.A_ABNORMAL_CLEAN_ALLOWED) ? "true" : "false");
                valueOperations.set(RedisKeyConstant.statusWord3.A_ABNORMAL_CLEAN_RUNNING,
                        isStatusBitSet(info, StatusWord3.A_ABNORMAL_CLEAN_RUNNING) ? "true" : "false");
                boolean abnormalCleanDone = isStatusBitSet(info, StatusWord3.A_ABNORMAL_CLEAN_DONE);
                valueOperations.set(RedisKeyConstant.statusWord3.A_ABNORMAL_CLEAN_DONE,
                        abnormalCleanDone ? "true" : "false");
                if (abnormalCleanDone) {
                    valueOperations.set(RedisKeyConstant.DRUG_ERROR_START, "false");
                }
                valueOperations.set(RedisKeyConstant.statusWord3.A_ABNORMAL_CLEAN_OUTPUT_ERROR,
                        isStatusBitSet(info, StatusWord3.A_ABNORMAL_CLEAN_OUTPUT_ERROR) ? "true" : "false");
                break;
            case STATUS_WORD_4:
                //解析状态字4：bit0=C柜_送药完成  bit1=C柜_砸门开完成  bit2=C柜_砸门关完成
                if (((info.getRawValue() >> StatusWord4.C_SEND_DRUG_DONE.getBit()) & 1) == 1) {
                    boolean firstDone = !"true".equals(valueOperations.get(
                            RedisKeyConstant.statusWord4.C_SEND_DRUG_DONE));
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SEND_DRUG_DONE, "true");
                    plcSendService.sendCCabinetSendDrugAck();
                    if (firstDone) {
                        log.info("[PLC] 状态字4: C柜_送药完成");
                        RegisterInfo sourceLayerInfo = registerMap.get(PlcRegisterTable.C_SEND_SRC_LAYER);
                        RegisterInfo sourceIndexInfo = registerMap.get(PlcRegisterTable.C_SEND_SRC_INDEX);
                        RegisterInfo targetWorkbenchInfo = registerMap.get(PlcRegisterTable.C_SEND_TARGET);
                        RegisterInfo actualWorkbenchInfo = registerMap.get(PlcRegisterTable.C_SEND_ACTUAL_TARGET);
                        int sourceLayer = sourceLayerInfo != null ? sourceLayerInfo.getRawValue() : 0;
                        int sourceIndex = sourceIndexInfo != null ? sourceIndexInfo.getRawValue() : 0;
                        int targetWorkbench = targetWorkbenchInfo != null ? targetWorkbenchInfo.getRawValue() : 0;
                        int actualWorkbench = actualWorkbenchInfo != null ? actualWorkbenchInfo.getRawValue() : 0;
                        if ("true".equals(valueOperations.get(RedisKeyConstant.DRUG_RETURN))) {
                            VacMachine data = vacMachineService.getMachineByLineNumAndPositionNum(
                                    sourceLayer, sourceIndex);
                            if (data != null && data.getProductNo() != null
                                    && data.getVaccineNum() != null && data.getVaccineNum() > 0) {
                                RedisDrugListData drugListData = new RedisDrugListData();
                                drugListData.setMachineId(data.getId());
                                drugListData.setWorkbenchNum(1);
                                drugListData.setWorkbenchNo("S01");
                                drugListData.setWorkbenchName("接种台1");
                                drugListData.setMachineNo(data.getBoxNo());
                                drugListData.setProductName(data.getProductName());
                                drugListData.setProductNo(data.getProductNo());
                                drugListData.setMachineStatus(data.getStatus());
                                dispensingFunction.dropRecordAndMachine(drugListData, 2, "疫苗退药");
                                log.info("[PLC] 退苗完成一只并记录 | 来源层={} 来源序号={} 仓位={}",
                                        sourceLayer, sourceIndex, data.getBoxNo());
                            } else {
                                log.warn("[PLC] 忽略多余退苗完成反馈，仓位已清空或无库存 | 来源层={} 来源序号={}",
                                        sourceLayer, sourceIndex);
                            }
                        } else {
                            ConfigSetting configSetting = configFunction.getSettingConfigData();
                            RedisDrugListData drugListData = findSendDrug(sourceLayer, sourceIndex);
                            if (drugListData != null) {
                                log.info("{} 出药成功信息：{}", drugListData.getWorkbenchName(), drugListData);
                                boolean workbenchMatched = targetWorkbench == actualWorkbench;
                                if (!workbenchMatched) {
                                    String errorMsg = String.format("接种台%d异常：%s 发送到实际接种台%d", targetWorkbench,
                                            drugListData.getProductName(), actualWorkbench);
                                    log.error("[PLC] {} | 来源层={} 来源序号={}", errorMsg, sourceLayer, sourceIndex);
                                    vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDDRUG.code,
                                            drugListData, errorMsg);
                                } else {
                                    log.info("发药正常：{}", JSON.toJSONString(drugListData));
                                    dispensingFunction.dropRecordAndMachine(drugListData, 1, "发药正常");
                                    valueOperations.set(RedisKeyConstant.SEND_DRUG_ERROR, "0");
                                }
                                Map<String, Object> commandData = new HashMap<>();
                                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_END.getCode());
                                commandData.put("data", drugListData);
                                websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(), commandData);
                                if ("true".equals(configSetting.getZcySend())) {
                                    zcyFunction.sendResult(drugListData, workbenchMatched ? "1" : "0");
                                }
                                popSendDrug(sourceLayer, sourceIndex);
                            }
                            if (listOps.index(RedisKeyConstant.SEND_LIST, 0) == null) {
                                valueOperations.set(RedisKeyConstant.DRUG_RUN_START, "false");
                            }
                        }
                    }
                } else {
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SEND_DRUG_DONE, "false");
                }
                if (((info.getRawValue() >> StatusWord4.C_SMASH_DOOR_OPEN_DONE.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SMASH_DOOR_OPEN_DONE, "true");
                    log.info("[PLC] 状态字4: C柜_砸门开完成");
                } else {
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SMASH_DOOR_OPEN_DONE, "false");
                }
                if (((info.getRawValue() >> StatusWord4.C_SMASH_DOOR_CLOSE_DONE.getBit()) & 1) == 1) {
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SMASH_DOOR_CLOSE_DONE, "true");
                    log.info("[PLC] 状态字4: C柜_砸门关完成");
                } else {
                    valueOperations.set(RedisKeyConstant.statusWord4.C_SMASH_DOOR_CLOSE_DONE, "false");
                }
                break;
            case A_ALARM_1: case A_ALARM_2: case A_ALARM_3: case A_ALARM_4: case A_ALARM_5:
            case B_ALARM_1: case B_ALARM_2: case B_ALARM_3: case B_ALARM_4: case B_ALARM_5:
                processAlarmWord(info);
                break;
            case C_ALARM_1: case C_ALARM_2: case C_ALARM_3: case C_ALARM_4: case C_ALARM_5:
            case C_ALARM_6: case C_ALARM_7:
                processAlarmWord(info);
                break;
            case B_JUDGE_LENGTH:
                if (!"0".equals(info.getActualValue())) {
                    valueOperations.set(RedisKeyConstant.bJudge.LENGTH, String.valueOf(Math.round(Float.parseFloat(info.getActualValue()) / 10)));
                }
                break;
            case B_JUDGE_WIDTH:
                if (!"0".equals(info.getActualValue())) {
                    valueOperations.set(RedisKeyConstant.bJudge.WIDTH, String.valueOf(Math.round(Float.parseFloat(info.getActualValue()) / 10)));
                }
                break;
            case B_JUDGE_HEIGHT:
                if (!"0".equals(info.getActualValue())) {
                    int heightInt = Math.round(Float.parseFloat(info.getActualValue()) / 10);
                    valueOperations.set(RedisKeyConstant.bJudge.HEIGHT, String.valueOf(heightInt));
                    //根据PLC状态字2判断是否有长宽厚请求，有才执行判断逻辑
                    if ("true".equals(valueOperations.get(RedisKeyConstant.statusRequest.JUDGE_REQUEST))) {
                        valueOperations.set(RedisKeyConstant.statusRequest.JUDGE_REQUEST, "false");
                        lengthStr = valueOperations.get(RedisKeyConstant.bJudge.LENGTH);
                        widthStr = valueOperations.get(RedisKeyConstant.bJudge.WIDTH);
                        if (lengthStr != null && widthStr != null) {
                            int length = Integer.parseInt(lengthStr);
                            int width = Integer.parseInt(widthStr);
                            int height = heightInt;
                            log.info("[PLC] B柜判断长宽高: 长={}, 宽={}, 厚={}", length, width, height);
                            if (length != 0 && width != 0 && height != 0) {
                                DistanceServoData data = new DistanceServoData();
                                data.setVaccineLong(length);
                                data.setVaccineWide(width);
                                data.setVaccineHigh(height);
                                if (sendDrugFunction != null) {
                                    VaccineData drugFlag = sendDrugFunction.drugIsRight(data, null);
                                    if (!drugFlag.getIsRight()) {
                                        plcSendService.sendBCabinetJudgeUnqualified();
                                        log.error("{}尺寸不合规，回滚重发。测量到的长宽高：{} {} {} 数据库的长宽高：{} {} {}",
                                                drugFlag.getProductName(), length, width, height,
                                                drugFlag.getDrugLong(), drugFlag.getDrugWide(), drugFlag.getDrugHigh());
                                    } else {
                                        plcSendService.sendBCabinetJudgeQualified();
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case B_JUDGE_BARCODE:
                String code = info.getActualValue();
                if (code == null || code.isEmpty()) {
                    break;
                }
                //根据PLC状态字2判断是否有条码请求，有才执行解析逻辑
                if (!"true".equals(valueOperations.get(RedisKeyConstant.statusRequest.BARCODE_REQUEST))) {
                    break;
                }

                valueOperations.set(RedisKeyConstant.statusRequest.BARCODE_REQUEST, "false");
                log.info("[PLC] {}: {}", info.getChineseDesc(), info.getActualValue());
                ConfigSetting configSetting = configFunction.getSettingConfigData();
                DrugRecordRequest drugRecordData = new DrugRecordRequest();
                if ("true".equals(configSetting.getZcyAuto())) {
                    drugRecordData = zcyFunction.getVaccineMsgByCode(code);
                    log.info("拿到政采云疫苗信息：{}", JSON.toJSONString(drugRecordData));
                } else {
                    drugRecordData = vacDrugService.sendDrugTest(code);
                    drugRecordData.setExpiredAt(Date.from(LocalDate.now().atStartOfDay()
                            .atZone(ZoneId.systemDefault()).toInstant()));
                    drugRecordData.setBatchNo("测试编号");
                    drugRecordData.setPrice(String.valueOf(321));
                    drugRecordData.setTag("测试标签");
                    drugRecordData.setSupervisedCode(code);
                    log.info("测试疫苗信息：{}", JSON.toJSONString(drugRecordData));
                }
                if (drugRecordData == null || drugRecordData.getIsReturn()) {
                    log.error("自动上药异常：电子监管码请求失败：{}", drugRecordData != null ? drugRecordData.getMsg() : "数据为空");
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code, code, "电子监管码请求失败");
                    plcSendService.sendBCabinetSendDrugUnqualified();
                    break;
                }
                lengthStr = valueOperations.get(RedisKeyConstant.bJudge.LENGTH);
                widthStr = valueOperations.get(RedisKeyConstant.bJudge.WIDTH);
                heightStr = valueOperations.get(RedisKeyConstant.bJudge.HEIGHT);
                if (lengthStr != null && widthStr != null && heightStr != null) {
                    int length = Integer.parseInt(lengthStr);
                    int width = Integer.parseInt(widthStr);
                    int height = Integer.parseInt(heightStr);
                    log.info("长={}, 宽={}, 厚={}", length, width, height);
                    if (length != 0 && width != 0 && height != 0) {
                        DistanceServoData data = new DistanceServoData();
                        data.setVaccineLong(length);
                        data.setVaccineWide(width);
                        data.setVaccineHigh(height);
                        VaccineData drugFlag = sendDrugFunction.drugIsRight(data, drugRecordData.getProductNo());
                        if (!drugFlag.getIsRight()) {
                            log.error("{}尺寸不合规，回滚重发。测量到的长宽高：{} {} {} 数据库的长宽高：{} {} {}",
                                    drugFlag.getProductName(), length, width, height,
                                    drugFlag.getDrugLong(), drugFlag.getDrugWide(), drugFlag.getDrugHigh());
                            plcSendService.sendBCabinetSendDrugUnqualified();
                            break;
                        }
                        //检查预留仓位中是否有同品种同批次可叠加的，有则直接复用，没有则走findBox
                        DrugRecordRequest drugRecordRequest = findReservedOrNewBox(data, drugRecordData);
                        if (drugRecordRequest == null) {
                            String msg = "自动上药异常：没有仓位可以装:" + drugRecordData.getProductName();
                            log.error(msg);
                            vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code, drugRecordData.getProductName(), msg);
                            plcSendService.sendBCabinetSendDrugUnqualified();
                            break;
                        }
                        //找到仓位后立即预留Redis（同品种药盒后续findBox可命中），防止并发抢占同一仓位
                        reserveMachine(drugRecordRequest.getMachineId(), drugRecordData);
                        plcSendService.sendBCabinetSendDrugQualified();

                        VacUntil.sleep(100);
                        BCabinetSendDrugRequest request = new BCabinetSendDrugRequest();
                        request.setIndex(drugRecordRequest.getPositionNum());
                        request.setLayer(drugRecordRequest.getLineNum());
                        listOps.rightPush(RedisKeyConstant.PLC_SEND_DRUG_MSG, JSON.toJSONString(drugRecordData));
                        plcSendService.sendBCabinetSendDrug(request);

                    }
                }
                break;
            case A_PAN_DISTANCE:
                //A柜盘苗距离结果
                valueOperations.set(RedisKeyConstant.PLC_INVENTORY_DISTANCE, info.getActualValue());
                if(!"0".equals(info.getActualValue())){
                    log.info("[PLC] A柜盘苗距离: {}", info.getActualValue());
                }

                break;
            case A_BELT_ALARM_SRC_LAYER:
                valueOperations.set(RedisKeyConstant.PLC_BELT_ALARM_SRC_LAYER, info.getActualValue());
                break;
            case A_BELT_ALARM_SRC_INDEX:

                valueOperations.set(RedisKeyConstant.PLC_BELT_ALARM_SRC_INDEX, info.getActualValue());
                //序号写入后触发异常处理业务，处理完再给PLC发应答
                handleBeltAlarmAck();
                break;
            case A_FILL_LAYER:
            case A_FILL_INDEX:
                break;
            case A_FILL_BARCODE:
                break;
            case C_SEND_SRC_LAYER:
            case C_SEND_SRC_INDEX:
            case C_SEND_TARGET:
                break;
            case A_BELT_1_ALARM: case A_BELT_2_ALARM: case A_BELT_3_ALARM:
            case A_BELT_4_ALARM: case A_BELT_5_ALARM: case A_SMALL_BELT_ALARM:
            case A_LIFT_ALARM: case A_CLAMP_STEP_ALARM: case A_CLAMP_FLAP_ALARM:
            case A_FEED_X_ALARM: case A_FEED_Z_ALARM:
            case A_DISP_X_ALARM: case A_DISP_Z_ALARM: case A_DISP_STEP_ALARM:
            case B_X_ALARM: case B_Y_ALARM: case B_Z_ALARM:
            case B_ROTATE_ALARM: case B_CONVEY_ALARM:
            case C_SLOPE_ALARM:
            case C_W1_CONVEY_ALARM: case C_W1_LIFT_ALARM: case C_W1_STEP_ALARM:
            case C_W2_CONVEY_ALARM: case C_W2_LIFT_ALARM: case C_W2_STEP_ALARM:
            case C_W3_CONVEY_ALARM: case C_W3_LIFT_ALARM: case C_W3_STEP_ALARM:
            case C_W4_CONVEY_ALARM: case C_W4_LIFT_ALARM: case C_W4_STEP_ALARM:
            case C_W5_CONVEY_ALARM: case C_W5_LIFT_ALARM: case C_W5_STEP_ALARM:
            case C_W6_CONVEY_ALARM: case C_W6_LIFT_ALARM: case C_W6_STEP_ALARM:
                break;
            default:
                break;
        }
        } catch (Exception e) {
            log.error("[PLC] 寄存器分发异常 | meta={} | address={} | value={} | {}",
                    info.getMeta(), info.getAddress(), info.getRawValue(), e.getMessage(), e);
        }
    }

    /**
     * 获取Redis中已预留的仓位ID集合（用于findBox排除，防止抢同一空仓）
     */
    private Set<Long> getReservedMachineIds() {
        Set<String> keys = hashOps.keys(RedisKeyConstant.PLC_RESERVED_MACHINES);
        if (keys == null || keys.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> ids = new HashSet<>();
        for (String s : keys) {
            ids.add(Long.parseLong(s));
        }
        return ids;
    }

    /**
     * 检查预留仓位 + findBox，优先复用同品种同批次已预留仓位
     */
    private DrugRecordRequest findReservedOrNewBox(DistanceServoData data, DrugRecordRequest drugRecordData) {
        //计算仓位最大容量
        int maxCapacity = sendDrugFunction.getDrugNum(data.getVaccineLong(), drugRecordData);
        //检查Redis预留仓位中是否有同品种同批次可叠加且未满的
        Map<String, String> allReserved = hashOps.entries(RedisKeyConstant.PLC_RESERVED_MACHINES);
        if (allReserved != null) {
            String targetProductNo = drugRecordData.getProductNo();
            String targetBatchNo = drugRecordData.getBatchNo();
            String targetExpiredAt = drugRecordData.getExpiredAt() != null ? String.valueOf(drugRecordData.getExpiredAt().getTime()) : "";
            for (Map.Entry<String, String> entry : allReserved.entrySet()) {
                Map<String, String> info = JSON.parseObject(entry.getValue(), new TypeReference<Map<String, String>>() {});
                if (info != null
                        && targetProductNo != null && targetProductNo.equals(info.get("productNo"))
                        && targetBatchNo != null && targetBatchNo.equals(info.get("batchNo"))
                        && targetExpiredAt != null && targetExpiredAt.equals(info.get("expiredAt"))) {
                    Long machineId = Long.parseLong(entry.getKey());
                    VacMachine vm = vacMachineService.getById(machineId);
                    if (vm != null) {
                        //DB已存数量 + Redis待写入数量 不能超过最大容量
                        int dbCount = vm.getVaccineNum() != null ? vm.getVaccineNum() : 0;
                        int pendingCount = Integer.parseInt(info.getOrDefault("pendingCount", "0"));
                        if (dbCount + pendingCount >= maxCapacity) {
                            log.info("[PLC] 预留仓位已满,跳过: machineId={}, dbCount={}, pendingCount={}, max={}", machineId, dbCount, pendingCount, maxCapacity);
                            continue;
                        }
                        //递增pendingCount
                        info.put("pendingCount", String.valueOf(pendingCount + 1));
                        hashOps.put(RedisKeyConstant.PLC_RESERVED_MACHINES, String.valueOf(machineId), JSON.toJSONString(info));
                        log.info("[PLC] 复用已预留仓位: machineId={}, boxNo={}, productNo={}, pendingCount={}", machineId, vm.getBoxNo(), targetProductNo, pendingCount + 1);
                        DrugRecordRequest result = new DrugRecordRequest();
                        result.setMachineId(vm.getId());
                        result.setLineNum(vm.getLineNum());
                        result.setPositionNum(vm.getPositionNum());
                        result.setLedNum(vm.getLedNum());
                        result.setProductName(vm.getProductName());
                        return result;
                    }
                }
            }
        }
        //没有匹配的预留仓位，走正常findBox（排除已预留的新空仓）
        return sendDrugFunction.findBoxExcludeMachineIds(data, drugRecordData, getReservedMachineIds());
    }

    /**
     * 预留仓位（刚findBox到的仓位，productInfo供后续同品种叠加匹配）
     */
    private void reserveMachine(Long machineId, DrugRecordRequest drugRecordData) {
        if (machineId == null) {
            return;
        }
        Map<String, String> info = new HashMap<>();
        info.put("productNo", drugRecordData.getProductNo() != null ? drugRecordData.getProductNo() : "");
        info.put("batchNo", drugRecordData.getBatchNo() != null ? drugRecordData.getBatchNo() : "");
        info.put("expiredAt", drugRecordData.getExpiredAt() != null ? String.valueOf(drugRecordData.getExpiredAt().getTime()) : "");
        info.put("productName", drugRecordData.getProductName() != null ? drugRecordData.getProductName() : "");
        info.put("pendingCount", "1");
        hashOps.put(RedisKeyConstant.PLC_RESERVED_MACHINES, String.valueOf(machineId), JSON.toJSONString(info));
        log.info("[PLC] 预留仓位: machineId={}, productNo={}, batchNo={}", machineId, drugRecordData.getProductNo(), drugRecordData.getBatchNo());
    }

    private boolean isStatusBitSet(RegisterInfo info, StatusWord3 statusWord) {
        return ((info.getRawValue() >> statusWord.getBit()) & 1) == 1;
    }

    /**
     * 释放仓位预留（上药完成写入DB后）
     */
    private void releaseMachine(Long machineId) {
        if (machineId != null) {
            hashOps.delete(RedisKeyConstant.PLC_RESERVED_MACHINES, String.valueOf(machineId));
            log.info("[PLC] 释放仓位: machineId={}", machineId);
        }
    }

    /**
     * 根据 PLC 上报的仓位坐标读取 SEND_LIST 中对应的发药任务，不改变 Redis 队列。
     */
    private RedisDrugListData findSendDrug(int sourceLayer, int sourceIndex) {
        List<String> sendList = listOps.range(RedisKeyConstant.SEND_LIST, 0, -1);
        if (sendList != null) {
            for (String item : sendList) {
                RedisDrugListData data = JSON.parseObject(item, RedisDrugListData.class);
                if (data != null && Objects.equals(data.getLineNum(), sourceLayer)
                        && Objects.equals(data.getPositionNum(), sourceIndex)) {
                    return data;
                }
            }
        }
        log.warn("[PLC] C柜送药完成未找到匹配发药任务 | 来源层={} 来源序号={}", sourceLayer, sourceIndex);
        return null;
    }

    /**
     * 在发药完成业务处理后，按 PLC 来源层和来源序号消费 SEND_LIST 中对应的任务。
     */
    private void popSendDrug(int sourceLayer, int sourceIndex) {
        List<String> sendList = listOps.range(RedisKeyConstant.SEND_LIST, 0, -1);
        if (sendList == null) {
            return;
        }
        for (String item : sendList) {
            RedisDrugListData data = JSON.parseObject(item, RedisDrugListData.class);
            if (data != null && Objects.equals(data.getLineNum(), sourceLayer)
                    && Objects.equals(data.getPositionNum(), sourceIndex)) {
                listOps.remove(RedisKeyConstant.SEND_LIST, 1, item);
                log.info("[PLC] C柜送药完成，已消费SEND_LIST任务 | 来源层={} 来源序号={}", sourceLayer, sourceIndex);
                return;
            }
        }
        log.warn("[PLC] C柜送药完成后消费SEND_LIST失败，未找到匹配任务 | 来源层={} 来源序号={}", sourceLayer, sourceIndex);
    }

    /**
     * PLC模式：A柜小皮带异常确认处理
     * <p>
     * 触发时机：A_BELT_ALARM_SRC_INDEX (277) 写入后
     * 处理流程：
     * 1. 读取 276/277 异常来源层号/序号
     * 2. 累计错误计数，连续3次以上报故障
     * 3. 禁用异常仓位
     * 4. 将该发药从 SEND_LIST 移到队尾重发
     * 5. 处理完成后向 PLC 发应答（控制字3 bit4=1，PLC收到后清零）
     */
    private void handleBeltAlarmAck() {
        try {
            // 读取异常来源层号、序号
            String layerStr = valueOperations.get(RedisKeyConstant.PLC_BELT_ALARM_SRC_LAYER);
            String indexStr = valueOperations.get(RedisKeyConstant.PLC_BELT_ALARM_SRC_INDEX);
            if ("0".equals(layerStr) || "0".equals(indexStr)) {
                return;
            }
            int alarmLayer = Integer.parseInt(layerStr);
            int alarmIndex = Integer.parseInt(indexStr);
            log.info("[PLC] 小皮带异常来源: 层={}, 序号={}", layerStr, indexStr);

            // 遍历整个SEND_LIST，找出所有匹配该层号+序号的记录
            List<String> allList = listOps.range(RedisKeyConstant.SEND_LIST, 0, -1);
            List<RedisDrugListData> matchDataList = new ArrayList<>();
            if (allList != null) {
                for (String item : allList) {
                    RedisDrugListData data = JSON.parseObject(item, RedisDrugListData.class);
                    if (data.getLineNum() == alarmLayer && data.getPositionNum() == alarmIndex) {
                        matchDataList.add(data);
                    }
                }
            }

            // SEND_LIST为空时，从PLC_CURRENT_SEND_DRUG获取当前发送中的药品信息
            if (matchDataList.isEmpty()) {
                String currentDrug = valueOperations.get(RedisKeyConstant.PLC_CURRENT_SEND_DRUG);
                if (currentDrug != null) {
                    RedisDrugListData currentData = JSON.parseObject(currentDrug, RedisDrugListData.class);
                    if (currentData.getLineNum() == alarmLayer && currentData.getPositionNum() == alarmIndex) {
                        matchDataList.add(currentData);
                    }
                }
            }

            if (matchDataList.isEmpty()) {
                log.warn("[PLC] 小皮带异常但SEND_LIST中无匹配(层={} 序号={})的发药记录，跳过重发", layerStr, indexStr);
                plcSendService.sendACabinetBeltAlarmAck();
                return;
            }

            RedisDrugListData firstMatch = matchDataList.get(0);

            // 错误计数累计
            String value = valueOperations.get(RedisKeyConstant.SEND_DRUG_ERROR);
            int errorCount = (value == null) ? 0 : Integer.parseInt(value);
            if (errorCount < 3) {
                errorCount++;
                valueOperations.set(RedisKeyConstant.SEND_DRUG_ERROR, String.valueOf(errorCount));
            } else {
                String errorMsg = "连续4次发药异常，机器停止发药，请联系售后人员";
                log.error(errorMsg);
                vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDDRUG.code, firstMatch, errorMsg);
            }

            // 禁用该仓位
            vacMachineService.vacMachineIOById(firstMatch.getMachineId(), 0);

            // 记录异常
            String errorMsg = String.format("第%d层皮带,仓位：%s 疫苗名称：%s PLC小皮带异常，未检测到药品，重新发药",
                    firstMatch.getBeltNum(), firstMatch.getBoxNo(), firstMatch.getProductName());
            log.error(errorMsg);
            vacMachineExceptionService.dropException(SettingConstants.MachineException.IO.code, firstMatch, errorMsg);
            log.info("firstMatch信息：{}",JSON.toJSONString(firstMatch));
            // 重发（addBoxDrugAgain内部自动全量扫SEND_LIST，同仓位记录一并处理入队）
            plcDispensingFunction.addBoxDrugAgain(firstMatch);

            // 处理完成后向PLC发应答
            plcSendService.sendACabinetBeltAlarmAck();
        } catch (Exception e) {
            log.error("[PLC] 小皮带异常处理失败 | {}", e.getMessage(), e);
        }
    }

    /**
     * 处理报警字各位
     * <p>
     * 遍历 AlarmBitTable 中所有匹配该地址的位定义，逐位判断并输出日志。
     * 每位只在上升沿（当前=1, 上次!=1）时输出日志，避免日志刷屏。
     */
    private void processAlarmWord(RegisterInfo info) {
        int address = info.getAddress();
        int rawValue = info.getRawValue();
        for (AlarmBitTable alarm : AlarmBitTable.values()) {
            if (alarm.getAddress() != address) {
                continue;
            }
            boolean active = (rawValue & alarm.getMask()) != 0;
            String redisKey = "plc:alarm:" + alarm.name();
            String lastVal = valueOperations.get(redisKey);
            if (active) {
                if (!"1".equals(lastVal)) {
                    valueOperations.set(redisKey, "1");
                    //记录待读取的报警编号地址（dispatch 循环结束后统一读取，带异常码上下文）
                    if (alarm.getAlarmCodeAddr() > 0) {
                        pendingAlarmCodes.put(alarm.getAlarmCodeAddr(), alarm);
                    }
                    log.error("===== [PLC] 报警触发 | {} | 地址={} bit={} | {} =====", alarm.getCabinet(), address, alarm.getBit(), alarm.getAlarmName());
                    //C柜报警触发时，从SEND_LIST弹出头部一条，报警信息合并药品名称
                    if ("C".equals(alarm.getCabinet())) {
                        String popStr = listOps.leftPop(RedisKeyConstant.SEND_LIST);
                        String drugName = "未知";
                        if (popStr != null) {
                            RedisDrugListData popData = JSON.parseObject(popStr, RedisDrugListData.class);
                            drugName = popData != null ? popData.getProductName() : "未知";
                            log.warn("[PLC] C柜报警，从SEND_LIST弹出[{}]: {}", drugName, popStr);
                        }
                        vacMachineExceptionService.sendException(
                                alarm.getExceptionCode(), drugName,
                                alarm.getAlarmName() + " | " + drugName);
                    } else {
                        //非C柜报警，直接记录异常
                        vacMachineExceptionService.sendException(alarm.getExceptionCode(), "", alarm.getAlarmName());
                    }
                }
            } else {
                if (!"0".equals(lastVal)) {
                    valueOperations.set(redisKey, "0");
                    log.info("[PLC] 报警恢复 | {} | 地址={} bit={} | {}", alarm.getCabinet(), address, alarm.getBit(), alarm.getAlarmName());
                }
            }
        }
    }

    /**
     * 报警编号按需读取并写入异常管理
     * <p>
     * dispatch 循环结束后由独立线程调用，阻塞读取单个寄存器(400-448)的伺服报警编号，
     * 将结果写入 vacMachineExceptionService.sendException。
     *
     * @param address 寄存器地址 (400-448)
     * @param alarm   触发此报警对应的 AlarmBitTable 定义
     */
    private void readAlarmCodeAndLog(int address, AlarmBitTable alarm) {
        try {
            ModbusFrame response = plcSendService.sendReadCommand(address, 1, 5000, TimeUnit.MILLISECONDS);
            if (response == null || response.isException()) {
                log.warn("[PLC] 读取报警编号超时或异常 | 地址={}", address);
                return;
            }
            byte[] data = response.getData();
            if (data == null || data.length < 3) {
                return;
            }
            int code = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            if (code > 0) {
                String msg = alarm.getAlarmName() + " | 报警编号=" + code;
                log.warn("[PLC] 报警编号 | 地址={} | 值={}", address, code);
                vacMachineExceptionService.sendException(alarm.getExceptionCode(), "", msg);
            }
        } catch (Exception e) {
            log.warn("[PLC] 读取报警编号异常 | address={} | {}", address, e.getMessage());
        }
    }

}
