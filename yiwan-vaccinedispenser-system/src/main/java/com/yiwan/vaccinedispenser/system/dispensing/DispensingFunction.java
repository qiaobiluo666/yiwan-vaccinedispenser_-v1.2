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
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.*;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhw
 * @date 2023/11/21
 * @Description
 */
@Slf4j
@Component

public class DispensingFunction {
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

    /**
     * 条形码 扫码以后用政采云的数据 实现发药
     */
    public void addDrugList(VacGetVaccine vacGetVaccine) throws Exception {

        //最后一个发药结束10分钟后再关门
        valueOperations.set(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME, LocalDateTime.now().toString());
        ConfigSetting configSetting = configFunction.getSettingConfigData();


        //抬升装置版本的C柜 如果复位按钮没有复位则 发不出来药
        if("true".equals(configSetting.getCLifting())){
            int workNum = vacGetVaccine.getWorkbenchNum();
            findCabinetReset(workNum);
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

                // 获取五个发药队列的大小
                Map<Integer, Long> beltQueueSizeMap = new HashMap<>();
                for (int i = 1; i <= 5; i++) {
                    Long listSize = listOps.size(String.format(RedisKeyConstant.DROP_LIST, i));
                    beltQueueSizeMap.put(i, listSize);
                }

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
                        // 获取这些仓位对应的皮带层数
                        Map<Integer, List<VacMachine>> beltLineToDrugsMap = drugsWithNearestExpiry.stream()
                                .collect(Collectors.groupingBy(drug -> {
                                    // 计算皮带层数：lineNum -> beltLine
                                    return (drug.getLineNum() + 1) / 2;
                                }));

                        // 找到皮带队列最小的层数
                        int selectedBeltLine = beltLineToDrugsMap.keySet().stream()
                                .min(Comparator.comparingLong(beltLine -> beltQueueSizeMap.getOrDefault(beltLine, Long.MAX_VALUE)))
                                .orElseThrow(() -> new ServiceException("未找到合适的皮带层数！"));

                        // 获取该皮带层数对应的所有仓位
                        List<VacMachine> drugsInSelectedBeltLine = beltLineToDrugsMap.get(selectedBeltLine);

                        // 3. 如果同一皮带层有多个仓位，选择 VaccineNum 最小的仓位
                        if (drugsInSelectedBeltLine.size() > 1) {
                            vacMachine = drugsInSelectedBeltLine.stream().min(Comparator.comparing(VacMachine::getVaccineNum))
                                    .orElseThrow(() -> new ServiceException("未找到符合条件的仓位！"));
                        } else {
                            vacMachine = drugsInSelectedBeltLine.get(0);
                        }
                    } else {
                        // 如果只有一个有效期最近的仓位，直接选择
                        vacMachine = drugsWithNearestExpiry.get(0);
                    }

                }

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
                //将发药数据存入数据库
                listOps.rightPush(String.format(RedisKeyConstant.DROP_LIST,realBeltLine),JSON.toJSONString(redisDrugListData));
                listOps.rightPush(RedisKeyConstant.BELT_LIST,JSON.toJSONString(redisDrugListData));

                //发药处方加入redis
                listOps.rightPush(RedisKeyConstant.SEND_LIST,JSON.toJSONString(redisDrugListData));
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_START.getCode());
                commandData.put("data", redisDrugListData);
                websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);

                //机器配有挡片 则检查挡片是否开启
                if("true".equals(configSetting.getCBlank())){
                    //开启挡片
                    Thread thread = new Thread(() -> {
                        String isQuery = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS_QUERY);

                        if(isQuery==null) {
                            isQuery = "false";
                        }

                        if("false".equals(isQuery)){
                            valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS_QUERY,"true");
                            //查询C柜挡片状态 如果关闭则 打开C柜挡片
                            moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
                            VacUntil.sleep(200);
                            String isOpen = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS);
                            long timeouts = System.currentTimeMillis();
                            while ((System.currentTimeMillis() - timeouts) < SettingConstants.WAIT_BLOCK_TIME){
                                if("close".equals(isOpen)){
                                    moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.OPEN);
                                    break;
                                }else if("open".equals(isOpen)){
                                    break;
                                }
                                moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
                                VacUntil.sleep(200);
                            }
                            valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS_QUERY,"false");
                        }

                    });
                    thread.start();
                }
                log.info("添加发药处方：{}",JSON.toJSONStringWithDateFormat(
                        redisDrugListData,
                        "yyyy-MM-dd HH:mm:ss",
                        SerializerFeature.WriteDateUseDateFormat
                ));


            }
    }

    /**
     * 根据redis 判断是否掉药  掉药--药品停在皮带边缘
     */
    public void dropDrugs(Integer num,Integer ioTime){

        String beltHaveDrug  = valueOperations.get(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,num));
        //如果皮带上没有药
//        if("false".equals(beltHaveDrug)&&"true".equals(canDropDrug)) {
        if("false".equals(beltHaveDrug)){
                //拿到发药信息
                String drugStr = listOps.index(String.format(RedisKeyConstant.DROP_LIST, num), 0);
                if (drugStr != null) {
    //                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_DRUG_LIST,num),"true");

                    RedisDrugListData drugListData = JSON.parseObject(drugStr, RedisDrugListData.class);
                    //层数 IO站号
                    Integer sensorNum = drugListData.getLineNum();
                    Integer beltNum = drugListData.getBeltNum();
                    log.info("掉药信息：仓位：{} 掉药:{}", drugListData.getBoxNo(), drugListData.getProductName());
                    dropDrug(sensorNum, drugListData.getPositionNum(), ioTime);
                    VacUntil.sleep(300);
                    //将发药信息队列 移除一条
                    listOps.leftPop(String.format(RedisKeyConstant.DROP_LIST, num));
                    //皮带上有药
                    valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG, beltNum), "true");

            }
        }

    }

    /**
     * 移动皮带
     */
    public void moveBelt() throws Exception {

        ConfigSetting configSetting = configFunction.getSettingConfigData();
        intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
        VacUntil.sleep(200);
        //判断光栅传感器是否触发
        String sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.BELT_SENSOR);
        if(sensorIsPut == null){
           //设置光栅信号 状态为不触发
            valueOperations.set(RedisKeyConstant.sensor.BELT_SENSOR,CabinetConstants.SensorStatus.RESET.code);
       }else if(sensorIsPut.equals(CabinetConstants.SensorStatus.RESET.code)){

           //如果传感器没触发 说明光栅皮带没药
            String moveMsgStr = listOps.index(RedisKeyConstant.BELT_LIST,0);
            RedisDrugListData drugListData = JSON.parseObject(moveMsgStr, RedisDrugListData.class);
            assert drugListData != null;
            Integer beltNum = drugListData.getBeltNum();


            String gsBeltHaveDrug = valueOperations.get(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG);

            //光栅皮带上没有药
//            if("true".equals(GsBeltHaveDrug)){
            if("false".equals(gsBeltHaveDrug)){
                valueOperations.set(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG,"true");
                //光栅的小皮带和皮带上的药一起掉
                log.info("============================将药送到光栅小皮带=============================");

                //传送小皮带走到皮带层
                goToBelt(beltNum,false);


                //TODO 先走速度模式  等稳定了再走位置模式
                //速度模式将药从皮带掉到光栅传感器
                speedServo(beltNum,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,500);
                VacUntil.sleep(200);
//                if(beltNum==1){
//                    speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,50);
//                }else {
//                    speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,50);
//                }

                speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.BELT_STOP,50);
                VacUntil.sleep(200);
                //等待掉药时间
                long timeout = System.currentTimeMillis();
                //判断是否掉药成功
                boolean dropFlag = false;
                while ((System.currentTimeMillis() - timeout) < SettingConstants.DRUG_BELT_WAIT_TIME){
                    intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
                    VacUntil.sleep(200);

                    //判断光栅传感器是否被触发
                    sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.BELT_SENSOR);
                    assert sensorIsPut != null;
                    if(sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                        dropFlag = true;
                        //皮带停止
//                        speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,300);
                        break;
                    }

                }

                //皮带伺服停止
                speedServo(beltNum,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,200);


                //传送小皮带回原位
                goToBelt(beltNum,true);

                if(dropFlag){
                    //将出药记录加入数据库
                    log.info("发药正常：{}",JSON.toJSONString(drugListData));
                    dropRecordAndMachine(drugListData,1,"发药正常");
                    moveBeltToC(drugListData,configSetting);

                }else {

                    //如果规定时间后，还是没有收到光栅触发的信号，电磁铁出问题 禁用该仓位
                    String errorMsg =  String.format("第%d层皮带,仓位：%s 疫苗名称：%s掉到光栅传送皮带异常，未检测到药品，重新发药", drugListData.getBeltNum(), drugListData.getBoxNo(),drugListData.getProductName());
                    log.error(errorMsg);
                    vacMachineExceptionService.dropException(SettingConstants.MachineException.IO.code,drugListData,errorMsg);
                    //将该发药队列移除
                    VacGetVaccine vacGetVaccine = new VacGetVaccine();
                    BeanUtils.copyProperties(drugListData,vacGetVaccine);

                    //禁用该仓位
                    vacMachineService.vacMachineIOById(drugListData.getMachineId(),0 );
                    valueOperations.set(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG,"false");

                    //重新发这个仓位的药
                    addBoxDrugAgain(drugListData);

                }

                //皮带可以开始掉药
                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,beltNum),"false");

            }else{
                //药还在皮带上运输 将送药信息加回皮带
                log.info("还没有处方信息");
                intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
            }
        }else {
            log.error("传送小皮带上有药 有异常");

        }

    }



    //运输小皮带运输疫苗到工作台
    private void moveBeltToC(RedisDrugListData drugListData,ConfigSetting configSetting){

        boolean flag = false;
        long timeout = System.currentTimeMillis();
        //等待C柜斜坡皮带停止
        while ((System.currentTimeMillis() - timeout) < SettingConstants.FIND_BELT_STOP_WAIT_TIME) {
            //查询皮带状态
            moveFind();
            String isBlankOpen;
            VacUntil.sleep(200);
            String isStop = valueOperations.get(RedisKeyConstant.CABINET_C_BELT_STOP);

        //C柜是否有挡片
        if("true".equals(configSetting.getCBlank())){
            //查询挡片状态
            moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
            VacUntil.sleep(50);
            isBlankOpen = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS);
            //如果挡片打开 和 皮带停止
            if("true".equals(isStop)&&"open".equals(isBlankOpen)){
                log.info("C柜皮带已经停止了！");
                log.info("C柜挡片已经打开！");
                flag = true;
                break;
            }

            if("close".equals(isBlankOpen)){
                log.info("正在打开C柜挡片！");
                moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.OPEN);
            }

            VacUntil.sleep(100);


        }else {
            if("true".equals(isStop)){
                log.info("C柜皮带已经停止了！");
                flag = true;
                break;
            }
             }
        }

        if(!flag){
            log.error("斜坡皮带一直未停止，超时发送命令！");
        }

        //运动伺服 使疫苗落到运输皮带上
        speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,150);

        //将药发送到工作台
        moveWork(drugListData.getWorkbenchNum());

        int count =0;
        timeout = System.currentTimeMillis();

        //等待药从小皮带掉到运输皮带
        while ((System.currentTimeMillis() - timeout) < SettingConstants.WORK_DRUG_BELT_WAIT_TIME){

            intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
            VacUntil.sleep(200);

            //判断光栅传感器是否被触发
            String sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.BELT_SENSOR);
            assert sensorIsPut != null;
            if(sensorIsPut.equals(CabinetConstants.SensorStatus.RESET.code)){
                if(count>=3){
                    break;
                }
                count++;
            }
        }

        VacUntil.sleep(300);
        speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,150);
        //光栅皮带不在小皮带上
        valueOperations.set(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG,"false");
        //将该发药队列移除
        listOps.leftPop(RedisKeyConstant.BELT_LIST);

        log.info("{}已经发往工作台：{}",drugListData.getProductName(),drugListData.getWorkbenchNum());
    }

    //重新发处方
    private void addBoxDrugAgain(RedisDrugListData drugDataList) throws Exception {

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

        List<String> dropDataList = listOps.range(String.format(RedisKeyConstant.DROP_LIST,drugDataList.getBeltNum()),0,-1);
        if(dropDataList != null){
            for(String dropData : dropDataList){
                RedisDrugListData drugData = JSON.parseObject(dropData, RedisDrugListData.class);
                //后续还有这个仓位的掉药记录删除
                if(Objects.equals(drugData.getPositionNum(), drugDataList.getPositionNum())&&Objects.equals(drugData.getLineNum(),drugDataList.getLineNum())){
                    listOps.remove(String.format(RedisKeyConstant.DROP_LIST,drugDataList.getBeltNum()),1,dropData);
                }
            }
        }

        //先将该仓位的掉药列表数据清楚
        //后续如果还有这个仓位发药 则重新发
        List<String> beltDataList = listOps.range(RedisKeyConstant.BELT_LIST,0,-1);

        if(beltDataList != null){
            for(String beltData : beltDataList){
                RedisDrugListData drugData = JSON.parseObject(beltData, RedisDrugListData.class);
                //后续还有这个仓位的掉药记录删除
                if(Objects.equals(drugData.getPositionNum(), drugDataList.getPositionNum())&&Objects.equals(drugData.getLineNum(),drugDataList.getLineNum())){
                    listOps.remove(RedisKeyConstant.BELT_LIST,1,beltData);
                    VacGetVaccine vacGetVaccine = new VacGetVaccine();
                    BeanUtils.copyProperties(drugData,vacGetVaccine);
                    //重新发药
                    addDrugList(vacGetVaccine);
                }
            }
        }

//        //将皮带设置回原来位置
//        valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,drugDataList.getBeltNum()),"false");
    }

    //IO 掉药指令
    public void dropDrug(Integer command,Integer ioNum ,Integer times){
        //开始掉药
        DropRequest dropRequest =new DropRequest();
        dropRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        //第几个IO板
        dropRequest.setCommand(command);
        //输出
        dropRequest.setMode(CabinetConstants.IOMode.AUTO);
        dropRequest.setIoNum(ioNum);
        dropRequest.setTimes(times);
        cabinetAService.dropCommand(dropRequest);
    }

    //速度模式伺服指令
    public void speedServo(Integer num, CabinetConstants.CabinetAServoCommand command, CabinetConstants.CabinetAServoStatus status, Integer speed){
        CabinetAServoRequest servoRequest = new CabinetAServoRequest();
        servoRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        servoRequest.setCommand(command);
        servoRequest.setMode(num);
        servoRequest.setStatus(status);
        servoRequest.setSpeed(speed);
        //发送移动皮带命令
        cabinetAService.servo(servoRequest);
    }

    //C柜速度模式伺服指令
    public void speedServoC(Integer num, CabinetConstants.CabinetCServoCommand command, CabinetConstants.CabinetCServoStatus status, Integer speed){
        CabinetCServoRequest servoRequest = new CabinetCServoRequest();
        servoRequest.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        servoRequest.setCommand(command);
        servoRequest.setMode(num);
        servoRequest.setStatus(status);
        servoRequest.setSpeed(speed);
        //发送移动皮带命令
        cabinetCService.servo(servoRequest);
    }

    //位置模式伺服指令
    public void positionServo(Integer num, Integer distance){
        valueOperations.set(String.format(RedisKeyConstant.BELT_SERVO_STATUS,num),"false");
        CabinetAServoRequest servoRequest = new CabinetAServoRequest();
        servoRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        servoRequest.setCommand(CabinetConstants.CabinetAServoCommand.POSITION);
        servoRequest.setMode(num);
        servoRequest.setStatus(CabinetConstants.CabinetAServoStatus.ZERO);
        servoRequest.setDistance(distance);
        //发送移动皮带命令
        cabinetAService.servo(servoRequest);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.SCAN_SERVO_WAIT_TIME) {
            if("true".equals(valueOperations.get(String.format(RedisKeyConstant.BELT_SERVO_STATUS,num)))){
                break;
            }
            VacUntil.sleep(400);
        }
    }

    //出药指令
    private void  moveWork(int workNum){
        valueOperations.set(RedisKeyConstant.CABINET_C_WORK,"false");
        CabinetCSendDrugRequest cabinetCSendDrugRequest = new CabinetCSendDrugRequest();
        cabinetCSendDrugRequest.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        cabinetCSendDrugRequest.setCommand(CabinetConstants.CabinetCSendDrugCommand.SEND);
        cabinetCSendDrugRequest.setWorkNum(workNum);
        cabinetCService.sendDrug(cabinetCSendDrugRequest);
    }

    //查询C柜斜坡皮带状态
    private void  moveFind(){
        CabinetCSendDrugRequest cabinetCSendDrugRequest = new CabinetCSendDrugRequest();
        cabinetCSendDrugRequest.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        cabinetCSendDrugRequest.setCommand(CabinetConstants.CabinetCSendDrugCommand.FIND);
        cabinetCService.sendDrug(cabinetCSendDrugRequest);
    }



    //挡片控制
    public void  moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus status){
        CabinetCSendDrugRequest cabinetCSendDrugRequest = new CabinetCSendDrugRequest();
        cabinetCSendDrugRequest.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        cabinetCSendDrugRequest.setCommand(CabinetConstants.CabinetCSendDrugCommand.BLOCK);
        cabinetCSendDrugRequest.setStatus(status);
        cabinetCService.sendDrug(cabinetCSendDrugRequest);
    }


    //查询光栅传感器 是否触发
    public void intPut(CabinetConstants.InPutCommand command, int mode){
        InPutRequest request = new InPutRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCabinet(CabinetConstants.Cabinet.CAB_A);
        request.setCommand( command);
        request.setMode(mode);
        cabinetAService.intPut(request);
    }

    //发药记录 以及 仓柜减少药品 -1 如果为0 为null
    public void  dropRecordAndMachine(RedisDrugListData drugListData,Integer status,String desc){
        log.info("开始减库存==================");
        //该药仓库存-1
        vacMachineService.decrementNumById(drugListData.getMachineId());
        //出药
        vacSendDrugRecordService.sendDrugRecordAdd(drugListData,status , desc);

        //多人份
        if(drugListData.getMachineStatus()==2){
            vacMachineDrugService.delMachineByCreatTime(drugListData.getMachineId());
        }
    }

    //根据层 走距离
    public void goToBelt(Integer beltNum,boolean goZero){
        Integer distance = null;
        if(goZero){
            distance=0;
        }else {


            List<SysConfig> sysConfigList = sysConfigService.getSendDrugConfigData();
            //获取需要走的distance
            for(SysConfig sysConfig: sysConfigList) {
                switch (beltNum) {
                    case 1 ->{
                        if("BELT_1".equals(sysConfig.getConfigType())){
                            distance = Integer.parseInt(sysConfig.getConfigValue());
                        }
                    }
                    case 2 ->{
                        if("BELT_2".equals(sysConfig.getConfigType())){
                            distance =Integer.parseInt(sysConfig.getConfigValue());
                        }
                    }
                    case 3 ->{
                        if("BELT_3".equals(sysConfig.getConfigType())){
                            distance =Integer.parseInt(sysConfig.getConfigValue());
                        }
                    }
                    case 4 ->{
                        if("BELT_4".equals(sysConfig.getConfigType())){
                            distance =Integer.parseInt(sysConfig.getConfigValue());
                        }
                    }
                    case 5 ->{
                        if("BELT_5".equals(sysConfig.getConfigType())){
                            distance =Integer.parseInt(sysConfig.getConfigValue());
                        }
                    }
                }
            }
            
        }
        //伺服运动
        positionServo(SettingConstants.CABINET_A_MOVE_BELT_TO_RETURN_NUM,distance);
    }

    //查询C柜复位按钮有没有按下
    public void findCabinetReset(int workNum){
        CabinetCSendDrugRequest request = new CabinetCSendDrugRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        request.setCommand(CabinetConstants.CabinetCSendDrugCommand.RESET);
        request.setMode(workNum);
        cabinetCService.sendDrug(request);
    }

    //打开挡片
    public void openBlank(){
        long timeout = System.currentTimeMillis();
        //等待C柜斜坡皮带停止
        while ((System.currentTimeMillis() - timeout) < SettingConstants.FIND_BELT_STOP_WAIT_TIME) {

            String isBlankOpen;
            //查询挡片状态
            moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
            VacUntil.sleep(200);
            isBlankOpen = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS);

            if("open".equals(isBlankOpen)){
                log.info("C柜挡片已经打开！");
                break;
            }

            //如果挡片打开 和 皮带停止
            if("close".equals(isBlankOpen)){
                log.info("正在打开C柜挡片！");
                moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.OPEN);
            }

            VacUntil.sleep(500);
        }
    }

    //关闭挡片
    public void closeBlank(){
        long timeout = System.currentTimeMillis();
        //等待C柜斜坡皮带停止
        while ((System.currentTimeMillis() - timeout) < SettingConstants.FIND_BELT_STOP_WAIT_TIME) {
            String isBlankOpen;
            //查询挡片状态
            moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);
            VacUntil.sleep(200);

            isBlankOpen = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS);

            if(("close").equals(isBlankOpen)){
                log.info("C柜挡片已经关闭！");
                break;
            }

            //如果挡片打开 和 皮带停止
            if("open".equals(isBlankOpen)){
                log.info("正在打开C柜挡片！");
                moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.CLOSE);
            }

            VacUntil.sleep(500);

        }

    }


    public void  closeBlankMinute(){
        moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.QUERY);

        VacUntil.sleep(200);

        String isBlankOpen = valueOperations.get(RedisKeyConstant.CABINET_C_BLOCK_STATUS);

        String timestampStr = valueOperations.get(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME);

        //查询挡片状态
        if("open".equals(isBlankOpen)){
            if(timestampStr == null){
                valueOperations.set(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME, LocalDateTime.now().toString());

            }else {
                LocalDateTime lastTimestamp = LocalDateTime.parse(timestampStr);
                long minutesElapsed = ChronoUnit.MINUTES.between(lastTimestamp, LocalDateTime.now());
                if (minutesElapsed >= 10) {
                    redisTemplate.delete(RedisKeyConstant.CABINET_C_BLANK_OPEN_TIME);
                    closeBlank();
                }
            }
        }
    }


}
