package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.until.StringUntils;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.*;
import com.yiwan.vaccinedispenser.system.sys.dao.VacBoxSpecMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.SendBtnData;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.OtherRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.DropRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author
 **/
@Service
@Slf4j
public class VacMachineServiceImpl extends ServiceImpl<VacMachineMapper, VacMachine> implements VacMachineService {

    @Autowired
    private  VacMachineMapper vacMachineMapper;

    @Autowired
    private VacDrugService vacDrugService;


    @Autowired
    private VacBoxSpecService vacBoxSpecService;


    @Autowired
    private VacDrugRecordService vacDrugRecordService;


    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Autowired
    private ZcyFunction zcyFunction;


    @Autowired
    private CabinetAService cabinetAService;

    @Value("${app.sendIsOpen}")
    private  String isSendOpen;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private VacBoxSpecMapper vacBoxSpecMapper;

    @Autowired
    private ConfigFunction configFunction;
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineDrugService vacMachineDrugService;

    @Autowired
    private VacSendDrugRecordService vacSendDrugRecordService;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Override
    public List<VacMachine> getVacMachineListByProductNo(String getProductName) {
        LambdaQueryWrapper<VacMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacMachine::getDeleted,0);
        if(getProductName!=null && !getProductName.isEmpty()){
            wrapper.eq(VacMachine::getProductName,getProductName);
        }
        wrapper.isNotNull(VacMachine::getProductName);
        return vacMachineMapper.selectList(wrapper);
    }

    @Override
    public Result  vacMachineList() {
        List<VacMachine> vacMachineList = vacMachineMapper.selectList( new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0));
        return Result.success(vacMachineList);
    }

    @Override
    public Result  vacMachineAdd(MachineListRequest request, UserBean user) {
        List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine ::getBoxNo,request.getBoxNo())
                .eq(VacMachine::getDeleted, 0)
                .or()
                .eq(VacMachine::getLineNum,request.getLineNum())
                .eq(VacMachine::getPositionNum,request.getPositionNum())
                .eq(VacMachine::getDeleted, 0) );


        if(!vacMachineList.isEmpty()){
            return Result.fail("该仓柜编号或位置已经存在");
        }


        VacMachine vacMachine = new VacMachine();
        BeanUtils.copyProperties(request, vacMachine);
        vacMachine.setCreateBy(user.getUserName());
        vacMachine.setUpdateBy(user.getUserName());
        int result = vacMachineMapper.insert(vacMachine);
        if(result>0){
            return Result.success();
        }else {
            return Result.fail("添加药仓规格异常！");
        }
    }

    @Override
    public Result  vacMachineEdit(MachineListRequest request, UserBean user) {
        log.info(JSON.toJSONString(request));
        VacMachine vacMachine = new VacMachine();
        BeanUtils.copyProperties(request, vacMachine);
        vacMachine.setUpdateBy(user.getUserName());
        vacMachine.setVaccineUseNum(request.getVaccineNum());
        vacMachine.setDeleted(0);
        int result = vacMachineMapper.updateNullById(vacMachine);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑药品异常！");
        }
    }

    @Override
    public Result  vacMachineDel(IdListRequest request, UserBean user) {

        // 查询要删除的记录
        List<VacMachine> vacMachineToDelete = vacMachineMapper.selectBatchIds(request.getIdList());

        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacMachine vacMachine : vacMachineToDelete) {

            //删除该仓位
            vacMachine.setUpdateBy(user.getUserName());
            vacMachine.setDeleted(1);
            vacMachine.setUpdateTime(LocalDateTime.now());
            result = vacMachineMapper.updateById(vacMachine);

            if(result<=0){
                flag=1;
            }
        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("删除药品异常！");
        }
    }

    @Override
    public void vacMachineIOById(Long id, Integer status) {
        VacMachine vacMachine = new VacMachine();
        vacMachine.setId(id);
        vacMachine.setStatus(status);
        vacMachineMapper.updateById(vacMachine);
    }

    @Override
    public void vacMachineIOByBoxNo(String boxNo) {
        vacMachineMapper.updateStatusByBoxNoAndDeleted(boxNo,0,1);
    }

    @Override
    public Result vacMachineBatchAdd(MachineListRequest request, UserBean user) {
        List<VacMachine>  vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getLineNum,request.getLineNum())
                .eq(VacMachine::getDeleted,0));
        if(!vacMachineList.isEmpty()){
            return  Result.fail("改成已存在仓位，请全部移除或者单个添加");
        }
        log.info(JSON.toJSONString(vacMachineList));
        for(int i=1;i<=request.getAddNum();i++){
            VacMachine vacMachine = new VacMachine();
            vacMachine.setLineNum(request.getLineNum());
            vacMachine.setPositionNum(i);
            vacMachine.setBoxNo(VacUntil.boxNoToCode(request.getLineNum(), i));
            //设置正常仓位
            vacMachine.setStatus(1);
            vacMachine.setUpdateBy(user.getUserName());
            vacMachine.setCreateBy(user.getUserName());
            vacMachineMapper.insert(vacMachine);

        }
        return Result.success();
    }

    @Override
    public DrugRecordRequest findPeople(List<Long> boxSepcIds, Integer num, DrugRecordRequest request) {
        // 多人份优先查找新仓位
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());
        List<VacMachine> vacMachineList = getNewBoxNo(boxSepcIds,request.getProductNo());
        if (!vacMachineList.isEmpty()) {
            log.info("多人份新仓位");
            VacMachine vacMachineData = vacMachineList.get(0);
            return  getDrugRecordRequestZeroNum(request,vacMachineData,vacDrug);
        }else {
            //查找是否有同个仓位
            List<VacMachine> vacMachineList1 = getOldPeopleBoxNo(boxSepcIds,num,request.getProductNo());
            if(!vacMachineList1.isEmpty()){
                log.info("查找多人份老仓位");
                VacMachine vacMachineDateOld = vacMachineList1.get(0);
                return getDrugRecordRequestExpiredAt(request,vacMachineDateOld,vacDrug);
            }else {
                log.info("没有仓位上多人份");
                return null;
            }
        }

    }

    @Override
    public DrugRecordRequest findBox(List<Long> boxSepcIds, Integer num, DrugRecordRequest request) {
        log.info("获取政采云request：{}",JSON.toJSONString(request));
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());



        //先查找是否有 有效期一致的苗仓
        List<VacMachine> vacMachineList = getExpiredAtBoxNoBatchNo(boxSepcIds,num,request.getExpiredAt(),request.getProductNo(),request.getBatchNo());

        if (!vacMachineList.isEmpty()) {

            VacMachine vacMachineData = vacMachineList.get(0);
            log.info("自动上药 同效期仓位 :{} 同批次：{} 数量：{}",vacMachineData.getBoxNo(),vacMachineData.getBatchNo(),vacMachineData.getVaccineNum());
            return getDrugRecordRequestHaveNum(request,vacMachineData,vacDrug);

        } else {
            //筛选一下
            List<VacMachine> vacMachineList5 =getBoxNoNullBatchNo(boxSepcIds, request.getProductNo(),num);
            if(!vacMachineList5.isEmpty()){
                VacMachine vacMachineData5 = vacMachineList5.get(0);
                log.info("自动上药 老仓位 没有batchNo：{}",vacMachineData5.getBoxNo());
                return getDrugRecordRequestZeroNum(request,vacMachineData5,vacDrug);
            }


            // 如果列表为空， 找一个新的药仓
            List<VacMachine> vacMachineList2 =getNewBoxNo(boxSepcIds, request.getProductNo());
            if (!vacMachineList2.isEmpty()) {
                VacMachine vacMachineData2 = vacMachineList2.get(0);
                log.info("自动上药 新仓位：{}",vacMachineData2.getBoxNo());
                return getDrugRecordRequestZeroNum(request,vacMachineData2,vacDrug);
            }else {
                //同效期不同批次
                List<VacMachine> vacMachineList3 = getOldBoxNoExpiredAt(boxSepcIds,num,request.getProductNo(),request.getExpiredAt());
                if(!vacMachineList3.isEmpty()){
                    VacMachine vacMachineData3 = vacMachineList3.get(0);
                    log.info("自动上药 同效期 不同批次 老仓位：{}，数量：{}",vacMachineData3.getBoxNo(),vacMachineData3.getVaccineNum());
                    return getDrugRecordRequestExpiredAt(request,vacMachineData3,vacDrug);

                } else {
                    //同种药
                    List<VacMachine> vacMachineList4 = getOldBoxNo(boxSepcIds,num,request.getProductNo());
                    if(!vacMachineList4.isEmpty()){
                        VacMachine vacMachineData4 = vacMachineList4.get(0);
                        log.info("自动上药 不同效期 不同批次 老仓位：{}，数量：{}",vacMachineData4.getBoxNo(),vacMachineData4.getVaccineNum());
                        return getDrugRecordRequestExpiredAt(request,vacMachineData4,vacDrug);
                    }else {

                        log.warn("没有仓位上：{},{}",vacDrug.getProductName(),request.getProductNo());
                        return null;
                    }


                }
            }
        }
    }

    @Override
    public DrugRecordRequest findBoxTest(List<Long> boxSepcIds, Integer num, DrugRecordRequest request) {
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());
        List<VacMachine> vacMachineList = getNewBoxNo(boxSepcIds,request.getProductNo());
        if (!vacMachineList.isEmpty()) {
            log.info("新仓位");
            VacMachine vacMachineData = vacMachineList.get(0);
            return getDrugRecordRequestZeroNum(request,vacMachineData,vacDrug);

        }else {
            log.info("老仓位");

            List<VacMachine> vacMachineList1 = getOldBoxNoExpiredAt(boxSepcIds,num,request.getProductNo(),request.getExpiredAt());
            if(!vacMachineList1.isEmpty()){
                VacMachine vacMachineData1 = vacMachineList1.get(0);
                return getDrugRecordRequestExpiredAt(request,vacMachineData1,vacDrug);
            }else {
                return  null;
            }
        }

    }

    @Override
    public void updateBox(DrugRecordRequest request, int status) {

        VacMachine vacMachineAdd = vacMachineMapper.selectById(request.getMachineId());
        request.setVaccineNum(vacMachineAdd.getVaccineNum());
        request.setVaccineUseNum(vacMachineAdd.getVaccineUseNum());
        Long vacId = vacMachineAdd.getId();
        log.info(JSON.toJSONString(vacMachineAdd));

        BeanUtils.copyProperties(request,vacMachineAdd);
        vacMachineAdd.setStatus(status);
        vacMachineAdd.setId(vacId);

        if(vacMachineAdd.getVaccineUseNum()!=null){
            vacMachineAdd.setVaccineUseNum(vacMachineAdd.getVaccineUseNum()+1);
        }else {
            vacMachineAdd.setVaccineUseNum(1);
        }
        if(vacMachineAdd.getVaccineNum()!=null){
            vacMachineAdd.setVaccineNum(vacMachineAdd.getVaccineNum()+1);
        }else {
            vacMachineAdd.setVaccineNum(1);
        }

        log.info(JSON.toJSONString(vacMachineAdd));
        log.info("新增上药信息：药品：{}，仓位：{}，数量：{}，有效期：{},电子监管码：{},批号：{}",vacMachineAdd.getProductName(),vacMachineAdd.getBoxNo(),vacMachineAdd.getVaccineNum(),vacMachineAdd.getExpiredAt(),request.getSupervisedCode(),request.getBatchNo());
        //更新日期
        vacMachineMapper.updateById(vacMachineAdd);

    }

    @Override
    public void decrementNumById(Long id) {
        vacMachineMapper.decrementNumById(id);
    }

    @Override
    public Page<InventoryResponse> vacMachineInventoryList(String productName, Integer page, Integer size) {

        Page<InventoryResponse> pageRequest = new Page<>(page, size);
        // 查询数据
        List<InventoryResponse> inventoryResponseList = vacMachineMapper.inventoryList(productName);
        for(InventoryResponse inventoryResponse:inventoryResponseList){
            //拿到是几人份的
            int num = Integer.parseInt(StringUntils.extractValue(inventoryResponse.getProductName()))*inventoryResponse.getTotalVaccineNum();
            inventoryResponse.setTotalVaccineNum(num);
            if(inventoryResponse.getStatus()==2){
                log.info(JSON.toJSONString(inventoryResponse));
                inventoryResponse.setProductName(inventoryResponse.getProductName()+"(散装总量)");
                inventoryResponse.setTotalVaccineNum(vacMachineDrugService.getTotalNumByVaccineId(inventoryResponse.getVaccineId()));
            }
        }

        // 手动分页
        int start = (int) ((pageRequest.getCurrent() - 1) * pageRequest.getSize());
        int end = (int) Math.min((start + pageRequest.getSize()), inventoryResponseList.size());
        List<InventoryResponse> pagedList = inventoryResponseList.subList(start, end);
        // 设置分页结果
        pageRequest.setRecords(pagedList);
        pageRequest.setTotal(inventoryResponseList.size());
        return pageRequest;

    }

    @Override
    public List<InventoryResponse> vacMachineInventoryListPdf(String productName) {
        List<InventoryResponse> inventoryResponseList = vacMachineMapper.inventoryList(productName);
        for(InventoryResponse inventoryResponse:inventoryResponseList){
            //拿到是几人份的
            int num = Integer.parseInt(StringUntils.extractValue(inventoryResponse.getProductName()))*inventoryResponse.getTotalVaccineNum();
            inventoryResponse.setTotalVaccineNum(num);
            if(inventoryResponse.getStatus()==2){
                log.info(JSON.toJSONString(inventoryResponse));
                inventoryResponse.setProductName(inventoryResponse.getProductName()+"(散装总量)");
                inventoryResponse.setTotalVaccineNum(vacMachineDrugService.getTotalNumByVaccineId(inventoryResponse.getVaccineId()));
            }
        }


        return inventoryResponseList;
    }

    @Override
    public Page<VacMachine> vacMachineInventoryDetail(String ProductNo, Integer status, Integer page, Integer size) {

        IPage<VacMachine> pageRequest= new Page<>(page,size);
        LambdaQueryWrapper<VacMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacMachine::getProductNo,ProductNo)
                .eq(VacMachine::getStatus,status)
                .eq(VacMachine::getDeleted,0);
        IPage<VacMachine> vacMachineIPage = vacMachineMapper.selectPage(pageRequest, wrapper);
        log.info(JSON.toJSONString(vacMachineIPage));
        //数据处理
        for(VacMachine vacMachine:vacMachineIPage.getRecords()){
            int num = Integer.parseInt(StringUntils.extractValue(vacMachine.getProductName()))*vacMachine.getVaccineNum();
            vacMachine.setVaccineNum(num);
            //如果是多人份散装 根据machine 求和
            if(vacMachine.getStatus()==2){
                int total = vacMachineDrugService.getTotalNumByMachineId(vacMachine.getId());
                vacMachine.setVaccineNum(total);

            }
        }

        return (Page<VacMachine>) vacMachineIPage;

    }

    /**
     * 手动上药 灯带亮灯
     *
     * @param code
     * @return
     */

    @Override
    public Result handDrugHand(String code) throws Exception {

        //人工上苗
        ConfigData configData = configFunction.getAutoDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        if("false".equals(valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_START_STATUS))){
            throw new ServiceException("扫码频繁");
        }
        try {
            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            if(code==null|| code.isEmpty()){
                log.warn("没有扫到二维码");
                return  Result.fail("没有扫到二维码");
            }


            // 读取 Redis 记录的上一个灯、当前灯
            String lastLedInfo = valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_LAST_LED_NUM_STATUS);
            int lastLineNum = -1, lastLedNum = -1;

            if (lastLedInfo != null && !lastLedInfo.isEmpty()) {
                String[] parts = lastLedInfo.split("_");
                if (parts.length == 2) {
                    lastLineNum = Integer.parseInt(parts[0]);
                    lastLedNum = Integer.parseInt(parts[1]);
                }
            }


            log.info("扫码到的二维码：{}",code);
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"false");


            //根据电子监管码查找到药品信息
            //跟政采云扫码 获得 药品信息
            DrugRecordRequest drugRecordData;
            if("true".equals(configSetting.getZcyAuto())){
                drugRecordData = zcyFunction.getVaccineMsgByCode(code);
                log.info(JSON.toJSONString(drugRecordData));
                if(drugRecordData.getIsReturn()){
                    //电子监管码请求失败
                    //TODO 没有仓位可以装这个药
                    log.error("手动上药异常：电子监管码请求失败：{}",drugRecordData.getMsg());
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),drugRecordData.getMsg());
                    //机械手回原
                    sendDrugFunction.moveHandServoInit(configData);
                    return Result.fail("政采云电子监管码请求失败!");
                }

            }else {
                //测试使用
                drugRecordData = vacDrugService.sendDrugTest(code);
                drugRecordData.setExpiredAt(new Date());
                drugRecordData.setBatchNo("测试编号");
                drugRecordData.setPrice(String.valueOf(321));
                drugRecordData.setTag("测试标签");
                drugRecordData.setSupervisedCode(code);
            }


            VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(drugRecordData.getProductNo());
            log.info("手动上苗信息：{}",JSON.toJSONString(vacDrug));
            if(vacDrug==null){
                String msg = "疫苗库里没有"+"疫苗编号："+drugRecordData.getProductNo()+"疫苗批次："+drugRecordData.getBatchNo()+"  请联系售后人员！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);

            }
            if(vacDrug.getVaccineWide()==null){
                String msg = "请先录入"+vacDrug.getProductName()+"的长宽高";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);
            }


            //计算一个仓位最多能存储多少只药品
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong());
            //确定是什么型号的仓柜
            List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(vacDrug.getVaccineWide());
            List<Long> boxSpecIds = new ArrayList<>();
            for(VacBoxSpec vacBoxSpec:vacBoxSpecList){
                boxSpecIds.add(vacBoxSpec.getId());
            }

            log.info("符合的仓位规格id:{}",JSON.toJSONString(boxSpecIds));
            //手动上药逻辑跟自动上药逻辑一致 优先将一个仓位先装满
            if (!boxSpecIds.isEmpty()) {
                //找到仓位 去发药
                DrugRecordRequest drugRecordRequest = findBox(boxSpecIds,num,drugRecordData);
                int lineNum = drugRecordRequest.getLineNum();
                int ledNum = drugRecordRequest.getLedNum();

                // 如果上次灯和新灯不同，则熄灭上次的灯
                if (lastLineNum != -1 && lastLedNum != -1 && (lastLineNum != lineNum || lastLedNum != ledNum)) {
                    sendDrugFunction.led(lastLineNum, lastLedNum, CabinetConstants.LedMode.NOT_OUTPUT);
                    log.info("熄灭上次 LED: 行 {}, 灯 {}", lastLineNum, lastLedNum);
                }


                VacUntil.sleep(200);
                sendDrugFunction.led(lineNum,ledNum,CabinetConstants.LedMode.OUTPUT);
                VacUntil.sleep(200);
                // 记录当前亮灯的位置和最后操作时间到 Redis
                long currentTime = System.currentTimeMillis();
                valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_LAST_LED_NUM_STATUS, lineNum + "_" + ledNum);
                valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_LAST_OPERATION_TIME, String.valueOf(currentTime));

                // 3 秒后允许再次扫码
                scheduler.schedule(() -> {
                    valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS, "true");
                    log.info("允许再次扫码");
                }, 3, TimeUnit.SECONDS);


                //机械手上有药，仓位药品数量+1，新增上药记录
                sendDrugFunction.addDrugRecord(drugRecordRequest,1);

                // 10 秒后检查是否需要熄灭灯
                scheduler.schedule(() -> {
                    String lastLed = valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_LAST_LED_NUM_STATUS);
                    String lastOperationTimeStr = valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_LAST_OPERATION_TIME);
                    if (lastLed != null && lastLed.equals(lineNum + "_" + ledNum) ){
                        assert lastOperationTimeStr != null;
                        long lastOperationTime = Long.parseLong(lastOperationTimeStr);
                        if (System.currentTimeMillis() - lastOperationTime >= 10000) {
                            sendDrugFunction.led(lineNum, ledNum, CabinetConstants.LedMode.NOT_OUTPUT);
                            log.info("灯光熄灭 (10s 超时): 行 {}, 灯 {}", lineNum, ledNum);
                            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_LAST_LED_NUM_STATUS, ""); // 清除记录
                        }
                    }
                }, 10, TimeUnit.SECONDS);

            }else {
                String msg = "手动上药异常：没有仓位可以装这个药";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
            }

            return Result.success();
        }finally {
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");
        }

    }


    @Override
    public Result handDrugMachine(String code) throws Exception {
        //机械手手动上苗
        ConfigData configData = configFunction.getAutoDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();

        if(Objects.equals(valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_START_STATUS), "false")){
            throw new ServiceException("扫码频繁");
        }

        try {
            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            if(code==null|| code.isEmpty()){
                log.warn("没有扫到二维码");
                return Result.fail("没有扫到二维码");
            }

            log.info("扫码到的二维码：{}",code);
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"false");

            //根据电子监管码查找到药品信息
            //跟政采云扫码 获得 药品信息
            DrugRecordRequest drugRecordData;
            if("true".equals(configSetting.getZcyAuto())){
                try {
                    drugRecordData = zcyFunction.getVaccineMsgByCode(code);
                }catch (Exception e){
                    log.error("手动上药异常：获取电子监管码接口异常");
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"","手动上药异常：获取电子监管码接口异常");
                    //机械手回原
                    sendDrugFunction.moveHandServoInit(configData);
                    return Result.fail("政采云电子监管码请求失败!");
                }

                if(drugRecordData.getIsReturn()){
                    //电子监管码请求失败
                    //TODO 没有仓位可以装这个药
                    log.error("手动上药异常：电子监管码请求失败：{}",drugRecordData.getMsg());
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),drugRecordData.getMsg());
                    //机械手回原
                    sendDrugFunction.moveHandServoInit(configData);

                    return Result.fail("政采云电子监管码请求失败!");
                }

            }else {
                //测试使用
                drugRecordData = vacDrugService.sendDrugTest(code);
                drugRecordData.setExpiredAt(new Date());
                drugRecordData.setBatchNo("测试编号");
                drugRecordData.setPrice(String.valueOf(321));
                drugRecordData.setTag("测试标签");
                drugRecordData.setSupervisedCode(code);
            }


            VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(drugRecordData.getProductNo());
            log.info("手动上苗信息：{}",JSON.toJSONString(vacDrug));
            if(vacDrug==null){
                String msg = "疫苗库里没有"+"疫苗编号："+drugRecordData.getProductNo()+"疫苗批次："+drugRecordData.getBatchNo()+"  请联系售后人员！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);

            }

            if(vacDrug.getVaccineWide()==null){
                String msg = "请先录入"+vacDrug.getProductName()+"的长宽高";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);
            }

            int wide = vacDrug.getVaccineWide()*100;

            //计算一个仓位最多能存储多少只药品
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong());
            log.info("计算仓位能装多少只：{}",num);
            //确定是什么型号的仓柜
            List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(vacDrug.getVaccineWide());
            List<Long> boxSpecIds = new ArrayList<>();
            for(VacBoxSpec vacBoxSpec:vacBoxSpecList){
                boxSpecIds.add(vacBoxSpec.getId());
            }

            //手动上药逻辑跟自动上药逻辑一致 优先将一个仓位先装满
            if (!boxSpecIds.isEmpty()) {
                //找到仓位 去发药
                DrugRecordRequest drugRecordRequest = findBox(boxSpecIds,num,drugRecordData);
                if(drugRecordRequest==null){
                    String msg = "没有仓位能上："+drugRecordData.getProductName();
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                    throw new ServiceException(msg);
                }



                sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
                sendDrugFunction.waitCabinetAStepEnd(1);

                //先去初始位置 13310,1950
                sendDrugFunction.moveHandServo(configData.getHandDrugX(),configData.getHandDrugZ());

                //提前走夹药一段距离
                int longs = vacDrug.getVaccineWide()*100;
                if((configData.getHandLen()-longs-configData.getEarly())>0){
                    int earlyDis = configData.getHandLen()-longs-configData.getEarly();
                    sendDrugFunction.cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,earlyDis);
                    sendDrugFunction.waitCabinetAStepEnd(1);
                }

                //检测传感器药品是否触发 10s
                boolean flag = false;
                String sensorIsPuts= null;
                long timeouts = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeouts) < SettingConstants.WAIT_BLOCK_TIME){
                    sendDrugFunction.intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
                    VacUntil.sleep(200);
                    //判断机械手底部传感器信号是否被触发
                    sensorIsPuts = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
                    assert sensorIsPuts != null;
                    //如果传感器触发 一直等待 不触发结束
                    if(sensorIsPuts.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                        flag =true;
                        break;
                    }
                }

                if(flag){
                    //掉药
                    int clampDis = configData.getHandLen()-wide-configData.getGap();
                    int dropDis = configData.getHandLen()-wide;
                    log.info("夹住距离:{}",clampDis);
                    boolean isSuccess = sendDrugFunction.dropDrugHandle(clampDis,dropDis,drugRecordRequest);

                    if(!isSuccess){
                        log.error("自动上药异常：药物异常报警,药没掉入药仓");

                        return Result.fail("自动上药异常：药物异常报警,药没掉入药仓");
                    }

                    //机械手上有药，仓位药品数量+1，新增上药记录
                    sendDrugFunction.addDrugRecord(drugRecordRequest,1);
                    //A柜机械手 步进电机 回原
                    sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
                    //A柜 步进电机 回原
                    sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
                }

                //机械手回原
                sendDrugFunction.moveHandServoInit(configData);

            }else {
                String msg = "手动上药异常：没有仓位可以装这个药";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
            }

            return Result.success();
        }finally {
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");
        }

    }

    @Override
    public Result handDrugPeople(String code, Integer bulkNum) throws Exception {

        //机械手手动上苗
        ConfigData configData = configFunction.getAutoDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        if("false".equals(valueOperations.get(RedisKeyConstant.handDrugStatus.HAND_START_STATUS))){
            throw new ServiceException("扫码频繁");
        }
        try {
            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            if(code==null|| code.isEmpty()){
                log.warn("没有扫到二维码");
                valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");
                return Result.fail("没有扫到二维码");
            }

            log.info("扫码到的二维码：{}",code);
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"false",60);

            //根据电子监管码查找到药品信息
            //跟政采云扫码 获得 药品信息
            DrugRecordRequest drugRecordData;
            if("true".equals(configSetting.getZcyAuto())){

                try {
                    drugRecordData = zcyFunction.getVaccineMsgByCode(code);
                }catch (Exception e){
                    log.error("手动上药异常：获取电子监管码接口异常");
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,"","手动上药异常：获取电子监管码接口异常");
                    //机械手回原
                    sendDrugFunction.moveHandServoInit(configData);
                    return Result.fail("政采云电子监管码请求失败!");
                }

                if(drugRecordData.getIsReturn()){
                    //电子监管码请求失败
                    //TODO 没有仓位可以装这个药
                    log.error("手动上药异常：电子监管码请求失败：{}",drugRecordData.getMsg());
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.SEND.code,drugRecordData.getProductName(),drugRecordData.getMsg());
                    //机械手回原
                    sendDrugFunction.moveHandServoInit(configData);

                    valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");
                    return Result.fail("政采云电子监管码请求失败!");
                }

            }else {
                //测试使用
                drugRecordData = vacDrugService.sendDrugTest(code);
                drugRecordData.setExpiredAt(new Date());
                drugRecordData.setBatchNo("测试编号");
                drugRecordData.setPrice(String.valueOf(321));
                drugRecordData.setTag("测试标签");
                drugRecordData.setSupervisedCode(code);
            }


            VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(drugRecordData.getProductNo());
            log.info("手动上苗信息：{}",JSON.toJSONString(vacDrug));

            if(vacDrug==null){
                String msg = "疫苗库里没有"+"疫苗编号："+drugRecordData.getProductNo()+"疫苗批次："+drugRecordData.getBatchNo()+"  请联系售后人员！";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);

            }
            if(vacDrug.getVaccineWide()==null){
                String msg = "请先录入"+vacDrug.getProductName()+"的长宽高";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                throw new ServiceException(msg);
            }

            int wide = vacDrug.getVaccineWide()*100;

            //计算一个仓位最多能存储多少只药品
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong());
            log.info("计算仓位能装多少只：{}",num);
            //确定是什么型号的仓柜
            List<VacBoxSpec> vacBoxSpecList = vacBoxSpecService.findVacBoxSpec(vacDrug.getVaccineWide());
            List<Long> boxSpecIds = new ArrayList<>();
            for(VacBoxSpec vacBoxSpec:vacBoxSpecList){
                boxSpecIds.add(vacBoxSpec.getId());
            }

            //手动上药逻辑跟自动上药逻辑一致 优先将一个仓位先装满
            if (!boxSpecIds.isEmpty()) {

                //找到仓位 去发药
                DrugRecordRequest drugRecordRequest = findPeople(boxSpecIds,num,drugRecordData);
                sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
                sendDrugFunction.waitCabinetAStepEnd(1);

                //先去初始位置
                sendDrugFunction.moveHandServo(configData.getHandDrugX(),configData.getHandDrugZ());

                //提前走夹药一段距离
                int longs = vacDrug.getVaccineWide()*100;
                if((configData.getHandLen()-longs-configData.getEarly())>0){
                    int earlyDis = configData.getHandLen()-longs-configData.getEarly();
                    sendDrugFunction.cabinetAStepPosition(CabinetConstants.CabinetAStepMode.CLAMP,earlyDis);
                    sendDrugFunction.waitCabinetAStepEnd(1);
                }

                //检测传感器药品是否触发 10s
                boolean flag = false;
                String sensorIsPuts= null;
                long timeouts = System.currentTimeMillis();
                while ((System.currentTimeMillis() - timeouts) < SettingConstants.WAIT_BLOCK_TIME){
                    sendDrugFunction.intPut(CabinetConstants.Cabinet.CAB_A,CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_HAND_NUM);
                    VacUntil.sleep(200);
                    //判断机械手底部传感器信号是否被触发
                    sensorIsPuts = valueOperations.get(RedisKeyConstant.sensor.HAND_SENSOR);
                    assert sensorIsPuts != null;
                    //如果传感器触发 一直等待 不触发结束
                    if(sensorIsPuts.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                        flag =true;
                        break;
                    }
                }
                if(flag){
                    //掉药
                    int clampDis = configData.getHandLen()-wide-configData.getGap();
                    int dropDis = configData.getHandLen()-wide;
                    log.info("夹住距离:{}",clampDis);
                    boolean isSuccess = sendDrugFunction.dropDrugHandle(clampDis,dropDis,drugRecordRequest);

                    if(!isSuccess){
                        log.error("自动上药异常：药物异常报警,药没掉入药仓");
                        return Result.fail("自动上药异常：药物异常报警,药没掉入药仓");
                    }

                    //机械手上有药，仓位药品数量+1，新增上药记录
                    sendDrugFunction.addDrugRecordPeople(drugRecordRequest,2,bulkNum);
                    //A柜机械手 步进电机 回原
                    sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
                    //A柜 步进电机 回原
                    sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);

                }
                //机械手回原
                sendDrugFunction.moveHandServoInit(configData);

            }else {
                String msg = "手动上药异常：没有仓位可以装这个药";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
            }
            return Result.success();
        }finally {
            valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");
        }

    }

    @Override
    public Result machineInventoryCount() throws Exception {

        List<VacMachine> vacMachineList = vacMachineMapper.selectList(
                new LambdaQueryWrapper<VacMachine>().eq(VacMachine::getDeleted,0)
                        .isNotNull(VacMachine::getCountX)
                        .isNotNull(VacMachine::getCountZ)
        );

        String msg;
        int bank = 1250;
        for(VacMachine record :vacMachineList){
            log.info("==============================================测距开始=================================================");
            //判断是不是在发药中 如果发药中停止自动盘点
            String drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
            if (drugStr!=null){
                msg = "正在发药! 取消库存盘点！";
                log.warn(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                break;
            }

            //机械手走测距位置
            sendDrugFunction.moveHandServo(record.getCountX(),record.getCountZ());

            VacUntil.sleep(1000);
            //测距
            Integer disNum =sendDrugFunction.getDistanceCount();

            if(disNum==null){

                msg = "库存盘点传感器测试异常";
                log.error(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                continue;

            }


            log.info("距离传感器显示距离：{}",disNum);
            //距离大于这个值 则为空仓
            if(disNum>bank){

                if(record.getProductNo()!=null){
                     msg = String.format("仓位：%s , 疫苗:%s ,设置为空仓",record.getBoxNo(),record.getProductName());
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                }

                //如果是空仓  仓位上还有数据 清空仓位
                record.setVaccineId(null);
                record.setVaccineNum(null);
                record.setVaccineUseNum(null);
                record.setProductName(null);
                record.setProductNo(null);
                record.setExpiredAt(null);
                log.info("仓位：{} 为空仓",record.getBoxNo());

                vacMachineMapper.updateNullById(record);
                continue;

            }

            //如果不是空仓 仓位上没数据   找到最近的那条数据 对比长度  如果长度相差不大 则 恢复原来的数据 如果长度相差过大 则 禁掉该仓位
            if(record.getProductNo()==null){
                log.info(String.valueOf(record.getId()));
                //拿到最近的一条数据
                VacDrugRecord vacDrugRecord = vacDrugRecordService.getLastByMachineId(record.getId());

                if(disNum<800){
                    log.warn("仓位号：{},空仓！传感器读取误差，读取距离为：{}",record.getBoxNo(),disNum);
                    continue;
                }

                //如果没有上药记录 则报警
                if(vacDrugRecord==null){
                    msg = String.format("仓位号：%s 异常，有药盒，没有历史上药记录",record.getBoxNo());
                    log.error(msg);
//                    vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                    continue;
                }

                //拿到他的长度
                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(vacDrugRecord.getProductNo());
                //比较长度的差距 如果长度超过药盒的1/2
                int vacLong = vacDrug.getVaccineLong();
                if((bank-disNum)>(vacLong/2) ){
                   int num = (bank-disNum)/vacLong;
                   int mode = (bank-disNum)%vacLong;
                   //计算出的余量大于药盒1/2 药盒+1
                   if(mode>(vacLong/2)){
                       num=num+1;
                   }
                   record.setVaccineId(vacDrug.getId());
                   record.setVaccineNum(num);
                   record.setVaccineUseNum(num);
                   record.setProductNo(vacDrug.getProductNo());
                   record.setProductName(vacDrug.getProductName());
                   record.setExpiredAt(vacDrugRecord.getExpiredAt());
                   if(num==1||num ==2){
                       msg = String.format("仓位：%s,系统数据已经清空，检测到还有药品，添加：疫苗名称：%s，数量：%s，效期：%s",record.getBoxNo(),record.getProductName(),num,record.getExpiredAt());
                       log.info(msg);
                       vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                       vacMachineMapper.updateNullById(record);
                   }else {
                       log.error("仓位：{},空仓！传感器数据误差太大，空仓检测超过2只",record.getBoxNo());
                   }
               }else {


                    log.info("仓位：{} 为空仓",record.getBoxNo());
                }

            }else {

                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(record.getProductNo());
                int vacLong = vacDrug.getVaccineLong();
                log.info("疫苗长度：{}",vacDrug.getVaccineLong());
                log.info("相差距离：{}",bank-disNum);

                int num = (bank-disNum)/vacLong;
                int mode = (bank-disNum)%vacLong;
                //计算出的余量大于药盒1/2 药盒+1
                if(mode>(vacLong/2)){
                    num=num+1;
                }

                //先判断库存是否相同
                if(num!=record.getVaccineNum()){

                    msg = String.format("仓位号：%s, %s  库存不对!系统库存量：%s 测量库存量：%s,传感器测量距离：%s",record.getBoxNo(),record.getProductName(),record.getVaccineNum(),num,disNum);
                    log.warn(msg);
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);

                    //如果仓位不对 更新去除电子监管码
                    record.setVaccineNum(num);
                    record.setVaccineUseNum(num);
                    //出药
//                   vacSendDrugRecordService.sendDrugRecordAdd(drugListData,status , "库存盘点清除库存");

                    //更新当前的库存
                    vacMachineMapper.updateNullById(record);
                }else {
                    record.setVaccineNum(num);
                    record.setVaccineUseNum(num);
                    //更新当前的库存
                    vacMachineMapper.updateNullById(record);
                    log.info("仓位号：{}，库存正常！,库存数量：{}",record.getBoxNo(),num);
                }

            }
            log.info("==============================================测距结束=================================================");
        }

        return Result.success();
    }

    @Override
    public void updateByIdAndNum(Long id, Integer num) {
        VacMachine vacMachine = vacMachineMapper.selectById(id);
        //疫苗的可用数量和数量
        vacMachine.setVaccineNum(num);
        vacMachineMapper.updateNullById(vacMachine);

    }

    @Override
    public Result autoBackVaccine(VacMachine vacMachine) throws ExecutionException, InterruptedException {

        while (true){
            //查找该疫苗的信息  同一个效期
            LambdaQueryWrapper<VacMachine> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(VacMachine::getDeleted,0)
                    .eq(VacMachine::getProductNo,vacMachine.getProductNo())
                    .eq(VacMachine::getExpiredAt,vacMachine.getExpiredAt())
                    .orderByAsc(VacMachine::getBoxNo);
            List<VacMachine> vacMachineList = vacMachineMapper.selectList(lambdaQueryWrapper);

            if(!vacMachineList.isEmpty()){
                VacMachine data = vacMachineList.get(0);
                //获取皮带id
                Integer beltNum =  (int) Math.ceil((double) data.getLineNum() / 2);

                //掉药
                dispensingFunction.dropDrug(data.getLineNum(),data.getPositionNum(),SettingConstants.IO_DROP_WAIT_TIME);
                //动皮带 到传感器接收到

                //抬升去当前皮带
                dispensingFunction.goToBelt(beltNum,false);

                //发送小皮带运动 直到传感器触发 再暂停 指令
                dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.BELT_STOP,50);

                VacUntil.sleep(100);
                //速度模式将药从皮带掉到光栅传感器
                dispensingFunction.speedServo(beltNum,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,500);


                String sensorIsPut;
                //等待掉药时间
                long timeout = System.currentTimeMillis();
                //判断是否掉药成功
                boolean dropFlag = false;

                while ((System.currentTimeMillis() - timeout) < SettingConstants.DRUG_BELT_WAIT_TIME){
                    dispensingFunction.intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
                    VacUntil.sleep(200);
                    //判断光栅传感器是否被触发
                    sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.BELT_SENSOR);
                    assert sensorIsPut != null;
                    if(sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                        dropFlag = true;
                        //皮带停止
                    dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,50);
                        break;
                    }
                }

                //5层皮带伺服停止
                dispensingFunction.speedServo(beltNum,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,200);
                VacUntil.sleep(200);

                //传送小皮带回原位
                dispensingFunction.goToBelt(beltNum,true);

                //运动伺服 使疫苗落到运输皮带上
                dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,150);
                //C柜伺服先运动2秒 防止药盒夹扁
                dispensingFunction.speedServoC(1,CabinetConstants.CabinetCServoCommand.SPEED,CabinetConstants.CabinetCServoStatus.COROTATION,150);
                VacUntil.sleep(2000);
                dispensingFunction.speedServoC(1,CabinetConstants.CabinetCServoCommand.PAUSE,CabinetConstants.CabinetCServoStatus.ZERO,150);

                //掉药数据 加入数据库
                RedisDrugListData drugListData = new RedisDrugListData();
                drugListData.setMachineId(data.getId());
                drugListData.setWorkbenchNum(1);
                dispensingFunction.dropRecordAndMachine(drugListData,2,"疫苗退药");
            }
            else {
                break;
            }
        }




//        Map<Integer ,List<Map<Integer, Integer>>> dropList =  new HashMap<>();
//        List<Map<Integer, Integer>> dropMaoList;
//        for(VacMachine data :vacMachineList){
//            Map<Integer ,Integer> maps = new HashMap<>();
//            dropMaoList = dropList.get(data.getLineNum());
//            if(dropMaoList==null){
//                dropMaoList = new ArrayList<>();
//            }
//            maps.put(data.getPositionNum(),data.getVaccineNum());
//            dropMaoList.add(maps);
//            dropList.put(data.getLineNum(),dropMaoList);
//        }
//
//        //创建异步任务
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        for(int i=1;i<=10;i++){
//            int finalI = i;
//            int beltNum = (int) Math.ceil((double) i /2);
//            if(dropList.get(i)!=null){
//                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                    //开启皮带
//                    dispensingFunction.speedServo(beltNum,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,200);
//                    dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,100);
//                    for (Map<Integer, Integer> item : dropList.get(finalI)) {
//                        for (Map.Entry<Integer, Integer> entry : item.entrySet()) {
//                            Integer key = entry.getKey();
//                            Integer value = entry.getValue();
//                            for(int j=0;j<value+2;j++){
//                                dispensingFunction.dropDrug(finalI,key,SettingConstants.IO_DROP_WAIT_TIME);
//                                VacUntil.sleep(1000);
//                            }
//                        }
//                    }
//                    //再走五秒 确保皮带上药品全部掉下去
//                    VacUntil.sleep(5000);
//                });
//                // 将任务添加到列表中
//                futures.add(future);
//            }
//            VacUntil.sleep(200);
//        }
//
//        // 等待所有异步任务完成
//        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//        allOf.get(); // 阻塞直到所有任务完成
//
//        //皮带和伺服停止
//        for(int x=1;x<=5;x++){
//            dispensingFunction.speedServo(x,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,300)   ;
//            VacUntil.sleep(200);
//        }
//        dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,300)   ;

//        for(VacMachine data :vacMachineList){
//            data.setVaccineNum(null);
//            data.setVaccineUseNum(null);
//            data.setExpiredAt(null);
//            data.setProductName(null);
//            data.setVaccineId(null);
//            data.setProductNo(null);
//            vacMachineMapper.updateNullById(data);
//
//            //上药那边状态更改
//            vacDrugRecordService.updateStatusByProductNo(vacMachine);
//        }


        dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,50);

        return Result.success();
    }

    @Override
    public VacMachine testDrop(Integer lineNum) {

        List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>().eq(VacMachine::getDeleted,0)
                .isNotNull(VacMachine::getVaccineId)
                        .gt(VacMachine::getVaccineUseNum,0)
                .in(VacMachine::getStatus, 1, 2)
                .eq(VacMachine::getLineNum,lineNum).orderByDesc(VacMachine::getVaccineUseNum));

        if(vacMachineList.isEmpty()){
            List<VacMachine> vacMachineLists = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>().eq(VacMachine::getDeleted,0)
                    .isNotNull(VacMachine::getVaccineId)
                    .ge(VacMachine::getVaccineUseNum,0)
                    .in(VacMachine::getStatus, 1, 2).orderByDesc(VacMachine::getVaccineUseNum));
            return vacMachineLists.get(0);
        }else {
            return vacMachineList.get(0);
        }


    }

    @Override
    public void testIOAll(int ioTime) {

        List<VacMachine> vacMachineList =vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>().eq(VacMachine::getDeleted,0).orderByDesc(VacMachine::getBoxNo));
        for(VacMachine data :vacMachineList){
            DropRequest dropRequest = new DropRequest();
            dropRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
            dropRequest.setMode(CabinetConstants.IOMode.AUTO);
            dropRequest.setIoNum(data.getPositionNum());
            dropRequest.setTimes(ioTime);
            dropRequest.setCommand(data.getLineNum());
            cabinetAService.dropCommand(dropRequest);
            VacUntil.sleep(1000);
        }

    }

    @Override
    public void testCabinet(OtherRequest request) {

        int count=1;
        while (count<=request.getCount()){
            List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                    .eq(VacMachine::getDeleted,0)
                    .eq(VacMachine::getLineNum,request.getCabinetLine())
                    .eq(VacMachine::getStatus,1)
                    .orderByAsc(VacMachine::getBoxNo));
            int startNum = request.getCabinetNumStart()-1;
            int endNum = request.getCabinetNumEnd();

            if(vacMachineList.size()<request.getCabinetNumEnd()){
                endNum = vacMachineList.size();
            }
            List<VacMachine> subList = vacMachineList.subList(startNum,endNum);
            for(VacMachine data :subList){
                DropRequest dropRequest = new DropRequest();
                dropRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
                dropRequest.setMode(CabinetConstants.IOMode.AUTO);
                dropRequest.setIoNum(data.getPositionNum());
                dropRequest.setTimes(request.getTime());
                dropRequest.setCommand(request.getCabinetLine());
                cabinetAService.dropCommand(dropRequest);
                if(request.getCabinetWaitTime()!=null){
                    VacUntil.sleep(request.getCabinetWaitTime());
                }else {
                    VacUntil.sleep(1000);
                }
            }
            count++;
        }

    }

    @Override
    public void handAutoX(OtherRequest request) {
        List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getLineNum,request.getHandLine()).orderByAsc(VacMachine::getBoxNo));
        // 初始值为传入的 autoXOne
        Integer currentAutoX = request.getAutoXOne();
        for (VacMachine vacMachine : vacMachineList) {
            // 设置当前记录的 auto_X 值
            vacMachine.setAutoX(currentAutoX);
            // 更新数据库
            vacMachineMapper.updateById(vacMachine);
            // 计算下一个记录的 auto_X 值
            // 获取当前记录的 box_spec_id 对应的 length
            VacBoxSpec vacBoxSpec = vacBoxSpecMapper.selectById(vacMachine.getBoxSpecId());

            if (vacBoxSpec != null) {
                currentAutoX += (vacBoxSpec.getLength() + 6)*10; // 更新 currentAutoX
            }
        }
    }

    @Override
    public List<SendBtnData> getSendBtnMSg() {
        List<SendBtnData> sendBtnDataList = new ArrayList<>();
        //获取所有信息
        List<InventoryResponse> inventoryResponseList = vacMachineMapper.inventoryList(null);
        for(InventoryResponse record:inventoryResponseList){
            SendBtnData sendBtnData = new SendBtnData();
            sendBtnData.setTotalNum(record.getTotalVaccineNum());
            sendBtnData.setProductName(getSendBtnProductName(record.getProductName()));
            sendBtnData.setProductNo(record.getProductNo());
            VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(record.getProductNo());
            sendBtnData.setManufacturerName(vacDrug.getManufacturerName());
            sendBtnDataList.add(sendBtnData);
        }
        return sendBtnDataList;
    }

    @Override
    public void machineSendDrugAlone(VacGetVaccine vacGetVaccine, UserBean userBean) throws Exception {
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        //请求发起人
        vacGetVaccine.setRequestNo(userBean.getUserName());

        dispensingFunction.addDrugList(vacGetVaccine);
    }


    //有效期一致的苗仓
    private  List<VacMachine> getExpiredAtBoxNoBatchNo(List<Long> boxSepcIds, Integer num, Date expiredAt, String productNo , String batchNo){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                //可用量要小于最大存储量
                .eq(VacMachine::getProductNo,productNo)
                .lt(VacMachine::getVaccineNum,num)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getExpiredAt,expiredAt)
                .eq(VacMachine::getBatchNo,batchNo)
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getStatus,1)
                //使用量、层数 升序排列
                .orderByDesc(VacMachine::getVaccineNum)
                .orderByAsc(VacMachine::getLineNum)
                //优先放最近的仓位
                .orderByAsc(VacMachine::getBoxNo);
//        //禁止ACYW 小药盒上第10层
//        if("81576000501".equals(productNo)){
//            queryWrapper.ne(VacMachine::getLineNum,10);
//        }
        return vacMachineMapper.selectList(queryWrapper);

    }

    //找一个新的药仓
    private  List<VacMachine> getNewBoxNo(List<Long> boxSepcIds,String productNo){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .isNull(VacMachine::getProductNo)
                .and(wp->wp.isNull(VacMachine::getVaccineNum).or().eq(VacMachine::getVaccineNum, 0))
                .eq(VacMachine::getStatus,1)
                .orderByAsc(VacMachine::getBoxNo);
        //禁止ACYW 小药盒上第10层
//        if("81576000501".equals(productNo)){
//            queryWrapper.ne(VacMachine::getLineNum,10);
//        }
        return vacMachineMapper.selectList(queryWrapper);

    }
    private  List<VacMachine> getBoxNoNullBatchNo(List<Long> boxSepcIds,String productNo,Integer num){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getProductNo,productNo)
                .isNull(VacMachine::getBatchNo)
                .eq(VacMachine::getStatus,1)
                .lt(VacMachine::getVaccineNum,num)
                .orderByAsc(VacMachine::getBoxNo);
        //禁止ACYW 小药盒上第10层
//        if("81576000501".equals(productNo)){
//            queryWrapper.ne(VacMachine::getLineNum,10);
//        }
        return vacMachineMapper.selectList(queryWrapper);

    }


    //找一个多人份老仓位
    private  List<VacMachine> getOldPeopleBoxNo(List<Long> boxSepcIds, Integer num,String productNo ){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getProductNo,productNo)
                .lt(VacMachine::getVaccineNum,num)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getStatus,2)
                //使用量、层数 升序排列
                .orderByAsc(VacMachine::getExpiredAt)
                .orderByAsc(VacMachine::getVaccineNum)
                .orderByAsc(VacMachine::getLineNum)
                //优先放最近的仓位
                .orderByAsc(VacMachine::getBoxNo);
        return vacMachineMapper.selectList(queryWrapper);

    }

    //不是同效期 老仓位
    private  List<VacMachine> getOldBoxNoExpiredAt(List<Long> boxSepcIds, Integer num, String productNo ,Date expiredAt){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getProductNo,productNo)
                .lt(VacMachine::getVaccineNum,num)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getExpiredAt,expiredAt)
                .eq(VacMachine::getStatus,1)
                //使用量、层数 升序排列
                .orderByDesc(VacMachine::getVaccineNum)
                .orderByAsc(VacMachine::getLineNum)
                .orderByAsc(VacMachine::getBoxNo);
//        //禁止ACYW 小药盒上第10层
//        if("81576000501".equals(productNo)){
//            queryWrapper.ne(VacMachine::getLineNum,10);
//        }
        return vacMachineMapper.selectList(queryWrapper);
    }


    //不是同效期 老仓位
    private  List<VacMachine> getOldBoxNo(List<Long> boxSepcIds, Integer num, String productNo){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getProductNo,productNo)
                .lt(VacMachine::getVaccineNum,num)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getStatus,1)
                //使用量、层数 升序排列
                .orderByDesc(VacMachine::getExpiredAt)
                .orderByDesc(VacMachine::getVaccineNum)
                .orderByAsc(VacMachine::getLineNum)
                .orderByAsc(VacMachine::getBoxNo);
//        //禁止ACYW 小药盒上第10层
//        if("81576000501".equals(productNo)){
//            queryWrapper.ne(VacMachine::getLineNum,10);
//        }
        return vacMachineMapper.selectList(queryWrapper);
    }


    //有效期相同 有数量
    private  DrugRecordRequest getDrugRecordRequestHaveNum(DrugRecordRequest request ,VacMachine vacMachineData,VacDrug vacDrug){
        request.setVaccineId(vacDrug.getId());
        //产品编号
        request.setProductNo(request.getProductNo());
        //产品名称
        request.setProductName(vacDrug.getProductName());
        //机器id
        request.setMachineId(vacMachineData.getId());
        //boxNo
        request.setMachineNo(vacMachineData.getBoxNo());

        //数量
        request.setVaccineUseNum(vacMachineData.getVaccineUseNum()+1);
        request.setVaccineNum(vacMachineData.getVaccineNum()+1);
        //提取出机械手X、Z位置
        request.setAutoX(vacMachineData.getAutoX());
        request.setAutoZ(vacMachineData.getAutoZ());
        request.setLedNum(vacMachineData.getLedNum());
        request.setLineNum(vacMachineData.getLineNum());

        return request;
    }

    //有效期相同 没数量
    private  DrugRecordRequest getDrugRecordRequestZeroNum(DrugRecordRequest request , VacMachine vacMachineData, VacDrug vacDrug){
        request.setVaccineId(vacDrug.getId());
        request.setExpiredAt(request.getExpiredAt());
        //产品编号
        request.setProductNo(request.getProductNo());
        //产品名称
        request.setProductName(vacDrug.getProductName());
        //机器id
        request.setMachineId(vacMachineData.getId());
        //boxNo
        request.setMachineNo(vacMachineData.getBoxNo());

        //数量
        request.setVaccineUseNum(1);
        request.setVaccineNum(1);
        //提取出机械手X、Z位置
        request.setAutoX(vacMachineData.getAutoX());
        request.setAutoZ(vacMachineData.getAutoZ());
        request.setLedNum(vacMachineData.getLedNum());
        request.setLineNum(vacMachineData.getLineNum());

        return request;
    }

    //有效期不相同 有数量
    private  DrugRecordRequest getDrugRecordRequestExpiredAt(DrugRecordRequest request ,VacMachine vacMachineData,VacDrug vacDrug){
        if(vacMachineData.getExpiredAt()!=null){
            //有效期日期
            request.setExpiredAt(vacMachineData.getExpiredAt().before(request.getExpiredAt()) ? vacMachineData.getExpiredAt() : request.getExpiredAt());
        }else {
            request.setExpiredAt(request.getExpiredAt());
        }
        //仓位信息
        request.setVaccineId(vacDrug.getId());
        //产品编号
        request.setProductNo(request.getProductNo());
        //产品名称
        request.setProductName(vacDrug.getProductName());
        //机器id
        request.setMachineId(vacMachineData.getId());
        //boxNo
        request.setMachineNo(vacMachineData.getBoxNo());



        //数量
        request.setVaccineUseNum(vacMachineData.getVaccineUseNum()+1);
        request.setVaccineNum(vacMachineData.getVaccineNum()+1);
        //提取出机械手X、Z位置
        request.setAutoX(vacMachineData.getAutoX());
        request.setAutoZ(vacMachineData.getAutoZ());
        request.setLedNum(vacMachineData.getLedNum());
        request.setLineNum(vacMachineData.getLineNum());
        return request;
    }



    private String  getSendBtnProductName(String productName){
        String[] parts = productName.split("-", 4); // 最多分割成4部分
        return parts[0] + "-" + parts[1] + "-" + parts[2];
    }

}