package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.yiwan.vaccinedispenser.system.dispensing.DispensingHandFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.*;
import com.yiwan.vaccinedispenser.system.sys.dao.VacBoxSpecMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineMapper;
import com.yiwan.vaccinedispenser.system.sys.data.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.OtherRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.*;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
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
import java.io.IOException;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private VacIoService vacIoService;

    @Autowired
    private VacBoxSpecService vacBoxSpecService;


    @Autowired
    private VacDrugRecordService vacDrugRecordService;


    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Autowired
    private DispensingHandFunction dispensingHandFunction;

    @Autowired
    private ZcyFunction zcyFunction;


    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private CabinetCService cabinetCService;

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
        List<VacMachine> vacMachineList = vacMachineMapper.selectList(wrapper);
        vacMachineList.sort(Comparator.comparing(
                VacMachine::getProductName,
                Collator.getInstance(Locale.CHINA)
        ));
        return vacMachineList;

    }

    @Override
    public Result  vacMachineList() {

        ConfigSetting configSetting = configFunction.getSettingConfigData();
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        List<VacMachine> vacMachineList = vacMachineMapper.selectList( new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0));
        List<VacMachineRequest> vacMachineRequests = vacMachineList.stream()
                .map(vacMachine -> {
                    VacMachineRequest request = new VacMachineRequest(); // using copy constructor
                    BeanUtils.copyProperties(vacMachine,request);
                    request.setIsIO(configSetting.getIsIoDrop());
                    request.setUpDistance(configSendData.getHandUpDistance());
                    return request;
                })
                .collect(Collectors.toList());
        return Result.success(vacMachineRequests);
    }

    @Override
    public Result vacMachineLockerList() {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        List<VacMachine> vacMachineList = vacMachineMapper.selectList( new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0));
        List<VacMachineRequest> vacMachineRequests = vacMachineList.stream()
                .map(vacMachine -> {
                    VacMachineRequest request = new VacMachineRequest(); // using copy constructor
                    BeanUtils.copyProperties(vacMachine,request);
                    request.setIsIO(configSetting.getIsIoDrop());
                    request.setUpDistance(configSendData.getHandUpDistance());
                    return request;
                })
                .collect(Collectors.toList());




        LockerInfoRequest  lockerInfoRequest = new LockerInfoRequest();
        lockerInfoRequest.setDataList(vacMachineRequests);
        lockerInfoRequest.setInventoryStatus(valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START));
        lockerInfoRequest.setSendStatus(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START));
        lockerInfoRequest.setErrorCleanStatus(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START));
        lockerInfoRequest.setHospitalName(configSetting.getHospitalName());

        return Result.success(lockerInfoRequest);



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

//    @Override
//    public DrugRecordRequest findBox(List<Long> boxSepcIds, Integer num, DrugRecordRequest request) {
//
//        log.info("获取政采云request：{}",JSON.toJSONString(request));
//        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());
//
//        //先查找是否有 有效期、批次一致的苗仓
//        List<VacMachine> vacMachineList = getExpiredAtBoxNoBatchNo(boxSepcIds,num,request.getExpiredAt(),request.getProductNo(),request.getBatchNo());
//        if (!vacMachineList.isEmpty()) {
//            VacMachine vacMachineData = vacMachineList.get(0);
//            log.info("自动上药 同效期仓位 :{} 同批次：{} 数量：{}",vacMachineData.getBoxNo(),vacMachineData.getBatchNo(),vacMachineData.getVaccineNum());
//            return getDrugRecordRequestHaveNum(request,vacMachineData,vacDrug);
//        }
//
//
//        //自动上药 老仓位 同效期   没有批次
//        List<VacMachine> vacMachineList1 =getBoxNoNullBatchNo(boxSepcIds, request.getProductNo(),request.getExpiredAt(),num);
//        if(!vacMachineList1.isEmpty()){
//            VacMachine vacMachineData1 = vacMachineList1.get(0);
//            log.info("自动上药 老仓位 同效期  没有batchNo：{}",vacMachineData1.getBoxNo());
//            return getDrugRecordRequestZeroNum(request,vacMachineData1,vacDrug);
//        }
//
//        // 如果列表为空， 找一个新的药仓
//        List<VacMachine> vacMachineList2 =getNewBoxNo(boxSepcIds, request.getProductNo());
//
//        if (!vacMachineList2.isEmpty()) {
//            VacMachine vacMachineData2 = vacMachineList2.get(0);
//            log.info("自动上药 新仓位：{}",vacMachineData2.getBoxNo());
//            return getDrugRecordRequestZeroNum(request,vacMachineData2,vacDrug);
//        }
//
//
//        //同效期不同批次
//        List<VacMachine> vacMachineList3 = getOldBoxNoExpiredAt(boxSepcIds,num,request.getProductNo(),request.getExpiredAt());
//        if(!vacMachineList3.isEmpty()){
//            VacMachine vacMachineData3 = vacMachineList3.get(0);
//            log.info("自动上药 同效期 不同批次 老仓位：{}，数量：{}",vacMachineData3.getBoxNo(),vacMachineData3.getVaccineNum());
//            return getDrugRecordRequestExpiredAt(request,vacMachineData3,vacDrug);
//        }
//
//
//        //同种药
//        List<VacMachine> vacMachineList4 = getOldBoxNo(boxSepcIds,num,request.getProductNo());
//        if(!vacMachineList4.isEmpty()){
//            VacMachine vacMachineData4 = vacMachineList4.get(0);
//            log.info("自动上药 不同效期 不同批次 老仓位：{}，数量：{}",vacMachineData4.getBoxNo(),vacMachineData4.getVaccineNum());
//            return getDrugRecordRequestExpiredAt(request,vacMachineData4,vacDrug);
//        }
//
//        log.warn("没有仓位上：{},{}",vacDrug.getProductName(),request.getProductNo());
//        return null;
//
//    }

    @Override
    public DrugRecordRequest findBox(List<Long> boxSepcIds, Integer num, DrugRecordRequest request) {
        log.info("获取政采云request：{}",JSON.toJSONString(request));
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(request.getProductNo());

        //先查找是否有 有效期、批次一致的苗仓
        List<VacMachine> vacMachineList = getExpiredAtBoxNoBatchNo(boxSepcIds,num,request.getExpiredAt(),request.getProductNo(),request.getBatchNo());
        if (!vacMachineList.isEmpty()) {
            VacMachine vacMachineData = vacMachineList.get(0);
            log.info("自动上药 同效期仓位 :{} 同批次：{} 数量：{}",vacMachineData.getBoxNo(),vacMachineData.getBatchNo(),vacMachineData.getVaccineNum());
            return getDrugRecordRequestHaveNum(request,vacMachineData,vacDrug);
        }
        log.info("没找到有效期、批次一致的苗仓");

        //自动上药 老仓位 同效期   没有批次
        List<VacMachine> vacMachineList1 =getBoxNoNullBatchNo(boxSepcIds, request.getProductNo(),request.getExpiredAt(),num);
        if(!vacMachineList1.isEmpty()){
            VacMachine vacMachineData1 = vacMachineList1.get(0);
            log.info("自动上药 老仓位 同效期  没有batchNo：{}",vacMachineData1.getBoxNo());
            return getDrugRecordRequestZeroNum(request,vacMachineData1,vacDrug);
        }
        log.info("没找到老仓位 同效期   没有批次");

        // 如果列表为空， 找一个新的药仓
        List<VacMachine> vacMachineList2 =getNewBoxNo(boxSepcIds, request.getProductNo());
        if (!vacMachineList2.isEmpty()) {
            VacMachine vacMachineData2 = vacMachineList2.get(0);
            log.info("自动上药 新仓位：{}",vacMachineData2.getBoxNo());
            return getDrugRecordRequestZeroNum(request,vacMachineData2,vacDrug);
        }


//        //同效期不同批次
//        List<VacMachine> vacMachineList3 = getOldBoxNoExpiredAt(boxSepcIds,num,request.getProductNo(),request.getExpiredAt());
//        if(!vacMachineList3.isEmpty()){
//
//            VacMachine vacMachineData3 = vacMachineList3.get(0);
//            log.info("自动上药 同效期 不同批次 老仓位：{}，数量：{}",vacMachineData3.getBoxNo(),vacMachineData3.getVaccineNum());
//            return getDrugRecordRequestExpiredAt(request,vacMachineData3,vacDrug);
//
//        }


//        //同种药
//        List<VacMachine> vacMachineList4 = getOldBoxNo(boxSepcIds,num,request.getProductNo());
//        if(!vacMachineList4.isEmpty()){
//            VacMachine vacMachineData4 = vacMachineList4.get(0);
//            log.info("自动上药 不同效期 不同批次 老仓位：{}，数量：{}",vacMachineData4.getBoxNo(),vacMachineData4.getVaccineNum());
//            return getDrugRecordRequestExpiredAt(request,vacMachineData4,vacDrug);
//        }




        log.warn("没有仓位上：{},{},{}",vacDrug.getProductName(),request.getProductNo(),request.getBatchNo());
        return null;

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
        if(productName!=null&& !productName.isEmpty()){
            for(InventoryResponse inventoryResponse:inventoryResponseList){
                //添加疫苗种类、厂家
                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(inventoryResponse.getProductNo());
                log.info(String.valueOf(inventoryResponse.getVaccineId()));
                inventoryResponse.setManufacturerName(vacDrug.getManufacturerName());
                inventoryResponse.setVaccineMinorName(vacDrug.getVaccineMinorName());
                //添加今日上苗 今日送苗
                //发苗
                SendDrugRecordRequest sendDrugRecordRequest = vacSendDrugRecordService.countTodayGroupedByProductId(inventoryResponse.getProductNo());
                Integer useDrugNum = sendDrugRecordRequest != null ? sendDrugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setUseDrugNum(useDrugNum);
                //今日上苗
                DrugRecordRequest drugRecordRequest = vacDrugRecordService.countTodayGroupedByProductId(inventoryResponse.getProductNo());
                Integer sendDrugNum = drugRecordRequest != null ? drugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setSendDrugNum(sendDrugNum);
                //拿到是几人份的
                int num = Integer.parseInt(StringUntils.extractValue(inventoryResponse.getProductName()))*inventoryResponse.getTotalVaccineNum();
                inventoryResponse.setTotalVaccineNum(num);
                if(inventoryResponse.getStatus()==2){
                    log.info(JSON.toJSONString(inventoryResponse));
                    inventoryResponse.setProductName(inventoryResponse.getProductName()+"(散装总量)");
                    inventoryResponse.setTotalVaccineNum(vacMachineDrugService.getTotalNumByVaccineId(inventoryResponse.getVaccineId()));
                }

            }
        }else {
            //拿到今日发苗的所有数据
            List<SendDrugRecordRequest> sendDrugRecordRequestList  =vacSendDrugRecordService.countToday();
            //拿到今日上苗的所有数据
            List<DrugRecordRequest> drugRecordRequestList = vacDrugRecordService.countToday();
            // 获取库存中已有的productNo
            Set<String> existingProductNos = inventoryResponseList.stream()
                    .map(InventoryResponse::getProductNo)
                    .filter(Objects::nonNull)
                    .filter(no -> !no.isEmpty())
                    .collect(Collectors.toSet());
            // 找出在发苗/上苗中但不在库存中的productNo
            List<String> missingProductNos = Stream.concat(
                            sendDrugRecordRequestList.stream().map(SendDrugRecordRequest::getProductNo),
                            drugRecordRequestList.stream().map(DrugRecordRequest::getProductNo)
                    )
                    .filter(Objects::nonNull)
                    .filter(no -> !no.isEmpty())
                    .filter(no -> !existingProductNos.contains(no))  // 关键：过滤掉库存中已有的
                    .distinct()
                    .toList();

            for(InventoryResponse inventoryResponse:inventoryResponseList){
                //添加疫苗种类、厂家
                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(inventoryResponse.getProductNo());
                inventoryResponse.setManufacturerName(vacDrug.getManufacturerName());
                inventoryResponse.setVaccineMinorName(vacDrug.getVaccineMinorName());

                //添加今日上苗 今日送苗
                //发苗
                SendDrugRecordRequest sendDrugRecordRequest = vacSendDrugRecordService.countTodayGroupedByProductId(inventoryResponse.getProductNo());
                Integer useDrugNum = sendDrugRecordRequest != null ? sendDrugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setUseDrugNum(useDrugNum);
                //今日上苗
                DrugRecordRequest drugRecordRequest = vacDrugRecordService.countTodayGroupedByProductId(inventoryResponse.getProductNo());
                Integer sendDrugNum = drugRecordRequest != null ? drugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setSendDrugNum(sendDrugNum);
                //拿到是几人份的
                int num = Integer.parseInt(StringUntils.extractValue(inventoryResponse.getProductName()))*inventoryResponse.getTotalVaccineNum();
                inventoryResponse.setTotalVaccineNum(num);
                if(inventoryResponse.getStatus()==2){
                    log.info(JSON.toJSONString(inventoryResponse));
                    inventoryResponse.setProductName(inventoryResponse.getProductName()+"(散装总量)");
                    inventoryResponse.setTotalVaccineNum(vacMachineDrugService.getTotalNumByVaccineId(inventoryResponse.getVaccineId()));
                }

            }


            //单独添加
            for (String missingProductNo : missingProductNos) {

                InventoryResponse inventoryResponse = new InventoryResponse();
                //添加疫苗种类、厂家
                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(missingProductNo);
                inventoryResponse.setProductName(vacDrug.getProductName());
                inventoryResponse.setProductNo(vacDrug.getProductNo());
                inventoryResponse.setManufacturerName(vacDrug.getManufacturerName());
                inventoryResponse.setVaccineMinorName(vacDrug.getVaccineMinorName());
                inventoryResponse.setTotalVaccineNum(0);
                //添加今日上苗 今日送苗
                //发苗
                SendDrugRecordRequest sendDrugRecordRequest = vacSendDrugRecordService.countTodayGroupedByProductId(missingProductNo);
                Integer useDrugNum = sendDrugRecordRequest != null ? sendDrugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setUseDrugNum(useDrugNum);

                //今日上苗
                DrugRecordRequest drugRecordRequest = vacDrugRecordService.countTodayGroupedByProductId(missingProductNo);
                Integer sendDrugNum = drugRecordRequest != null ? drugRecordRequest.getTotalNum() : 0;
                inventoryResponse.setSendDrugNum(sendDrugNum);
                inventoryResponseList.add(inventoryResponse);
            }

        }

        inventoryResponseList.sort(Comparator.comparing(
                InventoryResponse::getProductName,
                Collator.getInstance(Locale.CHINA)
        ));

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

            if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
                throw new ServiceException("正在自动对仓！");
            }


            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            String isInventoryStart = valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START);
            if("true".equals(isInventoryStart)){
                String msg = "正在库存盘点！";
                log.warn(msg);
                throw new ServiceException(msg);
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
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong(),drugRecordData);


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
                if(drugRecordRequest==null){
                    String msg = "手动上药异常：没有仓位可以装这个药";
                    log.error(msg);
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.HAND.code,drugRecordData.getProductName(),msg);
                }
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

            if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
                throw new ServiceException("正在自动对仓！");
            }


            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            String isInventoryStart = valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START);
            if("true".equals(isInventoryStart)){
                String msg = "正在库存盘点！";
                log.warn(msg);
                throw new ServiceException(msg);
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
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong(),drugRecordData);
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
                    String msg = "手动上药异常 :没有仓位能上："+drugRecordData.getProductName();
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
                        String msg = String.format("自动上药异常：药物异常报警,%s 药没掉入药仓",drugRecordRequest.getProductName());
                        log.error(msg);
                        return Result.fail(msg);
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
    public void vaccineNunEqualsUserNum() {
        vacMachineMapper.syncUseNumWithTotal();

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
            if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
                throw new ServiceException("正在自动对仓！");
            }


            if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
                throw new ServiceException("正在自动上药");
            }

            String isInventoryStart = valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START);
            if("true".equals(isInventoryStart)){
                String msg = "正在库存盘点！";
                log.warn(msg);
                throw new ServiceException(msg);
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
            int num = sendDrugFunction.getDrugNum(vacDrug.getVaccineLong(),drugRecordData);
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
    public Result machineStop(Integer type) {
        switch (type){
            //停止库存盘点
            case 1-> {
                valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"false");
                return Result.success("正在停止库存盘点");
            }

            //停止异常清理
            case 2 -> {
                valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"false");
                return Result.success("正在停止异常清理");
            }
        }
        return Result.success();

    }

    @Override
    public Result machineInventoryCount(MachineInventoryRequest request) throws Exception {

        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .isNotNull(VacMachine::getCountX)
                .isNotNull(VacMachine::getCountZ);
        if(request!=null){
            if(request.getLineList()!=null&&!request.getBoxNoList().isEmpty()){
                queryWrapper.in(VacMachine::getLineNum,request.getLineList());
            }

            if(request.getBoxNoList()!=null&&!request.getBoxNoList().isEmpty()){
                queryWrapper.in(VacMachine::getBoxNo,request.getBoxNoList());
            }else {
                return Result.success();
            }
        }


        List<VacMachine> vacMachineList = vacMachineMapper.selectList(
               queryWrapper
        );
        log.info(JSON.toJSONString(vacMachineList));
        String msg;
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        boolean isUpdate  = configSetting.getInventoryUpdate();
        boolean isStart  = configSetting.getInventoryStart();
        int bank = configSetting.getInventoryCountLen();

        if(!isStart){
            msg = "库存盘点系统参数未开启";
            log.warn(msg);
            throw new ServiceException(msg);
        }
        String isInventoryStart = valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START);
        if("true".equals(isInventoryStart)){
            msg = "正在库存盘点！";
            log.warn(msg);
            throw new ServiceException(msg);
        }

        if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
            throw new ServiceException("正在自动对仓！");
        }


        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            throw new ServiceException("正在自动上药");
        }

        valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"true");


        for(VacMachine record :vacMachineList){

            if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START))){
                log.info("库存盘点已停止");
                ConfigData configData = configFunction.getAutoDrugConfigData();
                sendDrugFunction.moveHandServoInit(configData);
                return Result.success("库存盘点已停止");
            }

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
                valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"false");
                throw new ServiceException(msg);

            }

            String autoSend = valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START);
            if("true".equals(autoSend)){
                msg = "正在自动上药! 取消库存盘点！";
                log.warn(msg);
                vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", msg);
                websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
                valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"false");
                throw new ServiceException(msg);
            }

            //测距
            Integer disNum = moveInventoryDis(record.getCountX(),record.getCountZ(),configSetting);

            if(disNum==null){
                continue;
            }

            int add = 3;
            int less =3;
            log.info("less:{}   add:{}",less,add);
            List<Integer> disNumList = new ArrayList<>();
//            //距离大于板长+100 则为空仓
//            if(disNum>(bank-150)){
//
//                //距离大于这个值  而且机器数据为1只药 设为空仓
//                if(record.getProductNo()!=null && record.getVaccineNum()<2){
//                    Integer num =  getInventoryNum(disNum,bank,record.getProductNo());
//                    if(num!=1){
//                        int count =50*less;
//                        //移动一下重新测距
//                        for(int i=1;i<less;i++){
//                            count =count-50;
//                            if(record.getCountX()-count<0){
//                                continue;
//                            }
//                            disNum = moveInventoryDis(record.getCountX()-count,record.getCountZ(),configSetting);
//                            if(disNum==null){
//                                continue;
//                            }
//                            if(bank>disNum){
//                                disNumList.add(disNum);
//                            }
//                        }
//                        count=0;
//                        //移动一下重新测距
//                        for(int i=1;i<add;i++){
//                            count =count+50;
//                            disNum = moveInventoryDis(record.getCountX()+count,record.getCountZ(),configSetting);
//                            if(disNum==null){
//                                continue;
//                            }
//                            if(bank>disNum){
//                                disNumList.add(disNum);
//                            }
//                        }
//
//                        boolean flag= false;
//                        if(!disNumList.isEmpty()){
//                            log.info("disNumList:{}",disNumList);
//                            for (Integer item: disNumList){
//
//                                 num =  getInventoryNum(item,bank,record.getProductNo());
//                                if(num==1){
//                                    flag =true;
//                                    break;
//                                }
//
//                            }
//                        }
//
//                        if(!flag){
//                            msg = String.format("仓位：%s , 疫苗:%s ,原来数量：%s 设置为空仓 测试距离：%s ",record.getBoxNo(),record.getProductName(),record.getVaccineNum(),disNum);
//                            vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
//                            //如果是空仓  仓位上还有数据 清空仓位
//                            record.setVaccineId(null);
//                            record.setVaccineNum(null);
//                            record.setVaccineUseNum(null);
//                            record.setProductName(null);
//                            record.setProductNo(null);
//                            record.setExpiredAt(null);
//                            log.info("仓位：{} 为空仓",record.getBoxNo());
//                            if(isUpdate){
//                                vacMachineMapper.updateNullById(record);
//                            }
//                        }
//                    }
//                    }
//
//                continue;
//            }

            //如果空仓 仓位上没数据   找到最近的那条数据 对比长度  如果长度相差不大 则 恢复原来的数据 如果长度相差过大 则 禁掉该仓位
            if(record.getProductNo()==null){

                log.info(String.valueOf(record.getId()));
                //拿到最近的一条数据
                VacDrugRecord vacDrugRecord = vacDrugRecordService.getLastByMachineId(record.getId());

                if(disNum>(bank-18)){
                    log.info("空仓！");
                    continue;
                }

                //如果没有上药记录 则报警
                if(vacDrugRecord==null){
                    msg = String.format("仓位号：%s 异常，有药盒，没有历史上药记录   读取距离：%s",record.getBoxNo(),disNum);
                    log.error(msg);
                    vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                    continue;
                }

                int num =  getInventoryNum(disNum,bank,vacDrugRecord.getProductNo());
                if(num==1||num ==2){
                   msg = String.format("仓位：%s,系统数据已经清空，检测到还有药品，添加：疫苗名称：%s，数量：%s，效期：%s",record.getBoxNo(),record.getProductName(),num,record.getExpiredAt());
                   log.info(msg);
                   vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                   if(isUpdate){
                       vacMachineMapper.updateNullById(record);
                   }
                }else if(num>2) {
                       msg =  String.format("仓位：%s,空仓！传感器数据误差太大，空仓检测超过2只",record.getBoxNo());
                       log.error(msg);
                       vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                }else {
                    log.info("仓位：{} 为空仓",record.getBoxNo());
                }

            }else {

                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(record.getProductNo());
                int vacLong = vacDrug.getVaccineLong();
                int num =  getInventoryNum(disNum,bank,record.getProductNo());
                List<Integer> numList = new ArrayList<>();
                //先判断库存是否相同
                if(num!=record.getVaccineNum()){
                    numList.add(num);
                    boolean flag= false;
                    //移动一下重新测距
                    int count =50*less;
                    for(int i=1;i<less;i++){
                        if(record.getCountX()-count<0){
                            continue;
                        }
                        count =count-50;
                        disNum = moveInventoryDis(record.getCountX()-count,record.getCountZ(),configSetting);

                        if(disNum==null){
                            continue;
                        }
                        num =  getInventoryNum(disNum,bank,record.getProductNo());
                        if(num==record.getVaccineNum()){
                            flag =true;
                            break;
                        }
                        numList.add(num);
                    }

                    if(!flag){
                        count=0;
                        //移动一下重新测距
                        for(int i=1;i<add;i++){
                            count =count+50;
                            disNum = moveInventoryDis(record.getCountX()+count,record.getCountZ(),configSetting);
                            if(disNum==null){
                                continue;
                            }
                            num =  getInventoryNum(disNum,bank,record.getProductNo());
                            if(num==record.getVaccineNum()){
                                flag =true;
                                break;
                            }
                            numList.add(num);
                        }
                    }



                    if(!flag){
                        //找到出现数量最多的num 写入
                        Optional<Integer> mostFrequentOpt = numList.stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                                .entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey);

                        num = mostFrequentOpt.orElse(0);
                        if(num<=0){
                            num=0;
                        }
                        msg = String.format("仓位号：%s, %s  库存不对!系统库存量：%s 测量库存量：%s,传感器测量距离：%s 疫苗长度：%s",record.getBoxNo(),record.getProductName(),record.getVaccineNum(),num,disNum,vacLong);
                        log.warn(msg);
                        vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
                        //如果仓位不对 更新去除电子监管码
                        if(num==0){
                            record.setVaccineId(null);
                            record.setVaccineNum(null);
                            record.setVaccineUseNum(null);
                            record.setProductName(null);
                            record.setProductNo(null);
                            record.setExpiredAt(null);
                            log.info("仓位：{} 为空仓",record.getBoxNo());
                        }else {
                            record.setVaccineNum(num);
                            record.setVaccineUseNum(num);
                        }

                        //出药
//                   vacSendDrugRecordService.sendDrugRecordAdd(drugListData,status , "库存盘点清除库存");

                        //更新当前的库存
                        if(isUpdate){
                            vacMachineMapper.updateNullById(record);
                        }
                    }

                }else {
                    record.setVaccineNum(num);
                    record.setVaccineUseNum(num);
                    //更新当前的库存
                    if(isUpdate){
                        vacMachineMapper.updateNullById(record);
                    }
                    log.info("仓位号：{}，库存正常！,库存数量：{}",record.getBoxNo(),num);
                }

            }
            log.info("==============================================测距结束=================================================");
        }
        //回到原位置
        ConfigData configData = configFunction.getAutoDrugConfigData();
        sendDrugFunction.moveHandServoInit(configData);
        valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"false");
        return Result.success();
    }

    @Override
    public Result machineInventoryError() throws Exception {
        log.info("=====================异常清理盘苗================================");
        //拿到异常处理的数据
        List<String> boxNoList = vacMachineExceptionService.getExceptionBoxNoListByError();
        MachineInventoryRequest request = new MachineInventoryRequest();
        request.setBoxNoList(boxNoList);
        machineInventoryCount(request);
        return Result.success(request);
    }


    private Integer moveInventoryDis(Integer x, Integer z, ConfigSetting configSetting) throws IOException {
        //机械手走测距位置
        sendDrugFunction.moveHandServo(x,z);
        VacUntil.sleep(1000);
        Integer disNum =sendDrugFunction.getDistanceCount();

        if(disNum==null){
            String msg = "库存盘点传感器测试异常";
            log.error(msg);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.COUNTWARING.code,"",msg);
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
            commandData.put("data", msg);
            websocketService.sendInfo(CommandEnums.MACHINE_STATUS_COMMAND.getCode(),commandData);
            return null;
        }

        log.info("距离传感器显示距离：{},传感器误差值：{}",disNum,configSetting.getInventoryCountValue());
        disNum = disNum-configSetting.getInventoryCountValue();
        return disNum;

    }


    private Integer getInventoryNum(Integer disNum ,Integer bank,String productNO){
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(productNO);
        int vacLong = vacDrug.getVaccineLong();
        log.info("疫苗长度：{}",vacDrug.getVaccineLong());
        log.info("相差距离：{}",bank-disNum);
        double len = bank - disNum;
        double raw = len / vacLong;

        int base = (int) Math.floor(raw);
//        // 只考虑药盒 ±1mm 的累计误差
//        double errBoxes = (base + 1.0)* 2.15 / vacLong ;
        return (raw - base  >= 0.6) ? base + 1 : base;


    }

    @Override
    public void updateByIdAndNum(Long id, Integer num) {
        VacMachine vacMachine = vacMachineMapper.selectById(id);
        //疫苗的可用数量和数量
        vacMachine.setVaccineNum(num);
        vacMachineMapper.updateNullById(vacMachine);

    }

    @Override
    public Result autoBackVaccine(VacMachineRequest request) throws ExecutionException, InterruptedException {

        String isDrp = valueOperations.get(RedisKeyConstant.DRUG_RUN_START);
        String isErrorClean = valueOperations.get(RedisKeyConstant.DRUG_ERROR_START);
        if("true".equals(isDrp)){
            return Result.fail("正在发苗！不能退苗");
        }
        if("true".equals(isErrorClean)){
            return Result.fail("正在异常清理！不能退苗");
        }

        valueOperations.set(RedisKeyConstant.DRUG_RETURN,"true");
        while (true){
            //查找该疫苗的信息  同一个效期
            LambdaQueryWrapper<VacMachine> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(VacMachine::getDeleted,0)
                    .eq(VacMachine::getProductNo, request.getProductNo())
                    .eq(VacMachine::getExpiredAt, request.getExpiredAt())

                    .orderByAsc(VacMachine::getBoxNo);

            if(!request.getBackAll()){
                lambdaQueryWrapper .eq(VacMachine::getBoxNo, request.getBoxNo());
                log.info("单仓退苗");
            }
            List<VacMachine> vacMachineList = vacMachineMapper.selectList(lambdaQueryWrapper);

            if(!vacMachineList.isEmpty()){

                VacMachine data = vacMachineList.get(0);
                //获取皮带id
                Integer beltNum =  (int) Math.ceil((double) data.getLineNum() / 2);

                //掉药
                dispensingFunction.dropDrug(data.getLineNum(),data.getPositionNum(),SettingConstants.IO_DROP_WAIT_TIME,data.getProductNo());
                //动皮带 到传感器接收到

                //抬升去当前皮带 工作台默认为1
                dispensingFunction.returnDrugGoToBelt(beltNum,false);

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
                valueOperations.set(RedisKeyConstant.sensor.BELT_SENSOR,CabinetConstants.SensorStatus.RESET.code);
                while ((System.currentTimeMillis() - timeout) < SettingConstants.DRUG_BELT_WAIT_TIME){

                    dispensingFunction.intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
                    VacUntil.sleep(500);
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
                dispensingFunction.returnDrugGoToBelt(beltNum,true);

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
                drugListData.setMachineNo(data.getBoxNo());
                drugListData.setProductName(data.getProductName());

                drugListData.setProductNo(data.getProductNo());
                drugListData.setMachineStatus(data.getStatus());
                dispensingFunction.dropRecordAndMachine(drugListData,2,"疫苗退药");
            }
            else {
                break;
            }
        }
        //传送小皮带暂停
        dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,50);
        valueOperations.set(RedisKeyConstant.DRUG_RETURN,"false");
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
    public List<VacMachine> cabinetLedTest(Integer lineNum) {
        return vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getLineNum,lineNum));
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
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        if("true".equals(configSetting.getIsIoDrop())){
            dispensingFunction.addDrugList(vacGetVaccine);
        }else {
            dispensingHandFunction.dropHandDrugs();
        }

    }

    @Override
    public void test1() {

        CabinetCServoRequest request = new CabinetCServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        request.setCommand(CabinetConstants.CabinetCServoCommand.POSITION);
        request.setStatus(CabinetConstants.CabinetCServoStatus.ZERO);
        while (true){
            //C柜伺服控制

            request.setDistance(0);
            request.setMode(5);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            request.setMode(6);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            request.setMode(7);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            VacUntil.sleep(4000);

            request.setMode(5);
            request.setDistance(2512);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            request.setMode(6);
            request.setDistance(2480);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            request.setMode(7);
            request.setDistance(12000);
            cabinetCService.servo(request);
            VacUntil.sleep(100);

            VacUntil.sleep(4000);
        }





    }


    @Override
    public void test2() {
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        CabinetAServoRequest request = new CabinetAServoRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAServoCommand.POSITION);
        request.setStatus(CabinetConstants.CabinetAServoStatus.ZERO);
        request.setMode(9);

        while (true){
            request.setDistance(configSendData.getBelt1());
            cabinetAService.servo(request);
            VacUntil.sleep(5000);
            request.setDistance(configSendData.getBelt5());
            cabinetAService.servo(request);
            VacUntil.sleep(5000);
        }
    }

    @Override
    public void test3() {
        DropRequest dropRequest = new DropRequest();
        dropRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        dropRequest.setMode(CabinetConstants.IOMode.AUTO);
        dropRequest.setCommand(1);
        dropRequest.setTimes(100);
        String count = valueOperations.get(RedisKeyConstant.TEST_IO_COUNT);
        while (true){
            dropRequest.setIoNum(1);
            cabinetAService.dropCommand(dropRequest);
            dropRequest.setIoNum(2);
            cabinetAService.dropCommand(dropRequest);
            VacUntil.sleep(5000);
            if(count==null){
                count="1";
            }else {
                count = Integer.toString(Integer.parseInt(count)+1);
            }
            log.info("电磁铁运行次数：{}",count);
            valueOperations.set(RedisKeyConstant.TEST_IO_COUNT,count);
        }
    }

    @Override
    public Result machineClean(IdListRequest request) {

        //查看疫苗退回状态
        String isReturn =  valueOperations.get(RedisKeyConstant.DRUG_RETURN);
        //查看是否再发苗中
        String isDrop  = valueOperations.get(RedisKeyConstant.DRUG_RUN_START);

        if("true".equals(isReturn)){
            return Result.fail("疫苗退回中，不能异常清除");
        }

        if("true".equals(isDrop)){
            return Result.fail("正在发苗！不能异常清除");
        }

        valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"true");
        List<Integer> errorList = request.getErrorList();
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        int workNum = configSendData.getReturnWorkNum();
        RedisDrugListData drugListData = new RedisDrugListData();
        drugListData.setWorkbenchNum(workNum);
        drugListData.setProductName("清苗疫苗");
        boolean isStop = false;
        if((errorList.size()==1&&errorList.contains(7))||(errorList.size()==1&&errorList.contains(101))){
            dispensingFunction.openBlank();
            //单独清空轨道
            dispensingFunction.moveWork(drugListData.getWorkbenchNum());

        }else if(errorList.size()==1&&errorList.contains(102) ){
            dispensingFunction.openBlank();
            //第二条轨道
            dispensingFunction.moveWork(configSendData.getReturnWorkNumTwo());

        } else if((errorList.size()==1&&errorList.contains(8) )|| (errorList.size()==1&&errorList.contains(103) )){
            //一条轨道的疫苗退回 版本
            returnDrugWarn(configSendData.getReturnWorkNum());
            log.info("疫苗清理异常结束");
            valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"false");
            return Result.success();

            }else if(errorList.size()==1&&errorList.contains(104)){
            //第二条轨道退苗
            returnDrugWarn(configSendData.getReturnWorkNumTwo());
            log.info("疫苗清理异常结束");
            valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"false");
            return Result.success();

        }

        else{
            List<Integer>   workList;
            if(errorList.contains(6)){
                workList = Arrays.asList(1, 2, 3, 4, 5);;
            }else {
                //过滤掉其他的选项 就清楚选中的轨道
                errorList.removeAll(Arrays.asList(6, 7,8,101,102,103,104));
                workList = errorList;
            }

            log.info(workList.toString());
            Map<Integer,Boolean> errorMap = new HashMap<>();
            for(Integer x :workList){
                errorMap.put(x,false);
            }
            while (!isStop){
                int count =0;
                //判断这层是不是true 如果是true 跳过
                for (Integer i : errorMap.keySet()) {

                    if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                        log.info("异常清除已经停止");
                        return Result.success("异常清除已经停止");
                    }

                    Boolean value = errorMap.get(i);
                    if(value){
                        count++;
                        if(count==errorMap.size()){
                            log.info("异常清除结束");
                            isStop = true;
                        }
                    }else {

                        //传送小皮带走到皮带层
                        dispensingFunction.goToBelt(i,workNum,false);

                        if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                            log.info("异常清除已经停止");
                            return Result.success("异常清除已经停止");
                        }

                        //速度模式将药从皮带掉到光栅传感器
                        dispensingFunction.speedServo(i,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,500);
                        VacUntil.sleep(200);

                        //如果点击停止 则光栅小皮带停止运行
                        if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                            dispensingFunction.speedServo(i,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,500);
                            log.info("异常清除已经停止");
                            return Result.success("异常清除已经停止");
                        }


                        dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.BELT_STOP,50);
                        VacUntil.sleep(200);


                        //检测到药时间
                        long timeout = System.currentTimeMillis();
                        String sensorIsPut;
                        //判断是否掉药成功
                        boolean dropFlag = false;
                        while ((System.currentTimeMillis() - timeout) < SettingConstants.DRUG_BELT_WAIT_TIME){

                            if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                                dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,50);
                                VacUntil.sleep(200);
                                //皮带伺服停止
                                dispensingFunction.speedServo(i,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,200);
                                log.info("异常清除已经停止");
                                return Result.success("异常清除已经停止");
                            }


                            dispensingFunction.intPut(CabinetConstants.InPutCommand.QUERY,SettingConstants.SENSOR_CABINET_A_MOVE_BELT_NUM);
                            VacUntil.sleep(200);
                            //判断光栅传感器是否被触发
                            sensorIsPut = valueOperations.get(RedisKeyConstant.sensor.BELT_SENSOR);
                            assert sensorIsPut != null;
                            if(sensorIsPut.equals(CabinetConstants.SensorStatus.NORMAL.code)){
                                dropFlag = true;
                                break;
                            }
                        }

                        //皮带伺服停止
                        dispensingFunction.speedServo(i,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,200);


                        if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                            dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,50);
                            VacUntil.sleep(200);
                            //皮带伺服停止
                            log.info("异常清除已经停止");
                            return Result.success("异常清除已经停止");
                        }

                        if(dropFlag){
                            //传送小皮带回原位
                            dispensingFunction.goToBelt(i,workNum,true);
                            if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                                VacUntil.sleep(200);
                                //皮带伺服停止
                                log.info("异常清除已经停止");
                                return Result.success("异常清除已经停止");
                            }
                            dispensingFunction.moveBeltToC(drugListData,configSetting,configSendData);
                        }else {
                            dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.ZERO,150);
                            //如果没有检测到传感器有药 皮带清空
                            errorMap.replace(i,true);
                        }
                    }
                }
            }

            if("false".equals(valueOperations.get(RedisKeyConstant.DRUG_ERROR_START))){
                //皮带伺服停止
                log.info("异常清除已经停止");
                return Result.success("异常清除已经停止");
            }

            //传送小皮带回原位
            dispensingFunction.goToBelt(1,workNum,true);
            dispensingFunction.moveWork(drugListData.getWorkbenchNum());

            //浦沿第二条轨道
            if(SettingConstants.PU_YAN_HOSPITAL_NAME.equals(configSetting.getHospitalName())) {
                dispensingFunction.moveWork(configSendData.getReturnWorkNumTwo());
            }


        }

        log.info("疫苗清理异常结束");
        valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"false");

        return Result.success();
    }

    @Override
    public void isSensorHaveDrug(ConfigSetting configSetting) {

        //浦沿街道
        if(configSetting.getHospitalName().equals(SettingConstants.PU_YAN_HOSPITAL_NAME)){
            puYanSenorReturn();
        }else {
            sensorReturn();
        }
    }

    @Override
    public void returnDrugWarn(Integer workNum) {

        CabinetCSendDrugRequest request = new CabinetCSendDrugRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        request.setMode(workNum);
        request.setCommand(CabinetConstants.CabinetCSendDrugCommand.RETURN);
        cabinetCService.sendDrug(request);

    }

    @Override
    public Integer getVacIoTimeByProduct(String productNo, Integer times) {
        //根据产品编码获取疫苗长度
        VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(productNo);
        //找不到这个药品则直接默认时间发药
        if(vacDrug==null || vacDrug.getVaccineLong() == null){
            return  times;
        }
        VacIo vacIo = vacIoService.getVacIoByLen(vacDrug.getVaccineLong());
        if(vacIo == null){
            return times;
        }else {
            return vacIo.getIoTime();
        }
    }

    public static boolean isValidBoxSequence(String startBoxNo, String endBoxNo) {
        // 检查是否为同一层（前3个字符相同：A01）
        boolean isSameLevel = startBoxNo.substring(0, 3).equals(endBoxNo.substring(0, 3));

        // 检查startBoxNo是否小于endBoxNo
        boolean isStartLessThanEnd = startBoxNo.compareTo(endBoxNo) < 0;

        return isSameLevel && isStartLessThanEnd;
    }





    @Override
    public Result AutoProofread(AutoData data) {

        if("true".equals(valueOperations.get(RedisKeyConstant.DRUG_INVENTORY_START))){
            throw new ServiceException("正在库存盘点！");
        }

        if("true".equals(valueOperations.get(RedisKeyConstant.AUTO_IS_START))){
            throw new ServiceException("正在自动对仓！");
        }

        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            throw new ServiceException("正在自动上药");
        }

        //判断层数和仓位是否符合
        String startBoxNo = data.getStartBoxNo();
        String endBoxNo  = data.getEndBoxNo();
        if(!isValidBoxSequence(startBoxNo,endBoxNo)){
            Result.fail("起始仓位跟结束仓位不在同一层或者起始仓位大");
        }

        //根据层数和起始仓位获取药仓数据
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getLineNum,data.getLineNum())
                    .eq(VacMachine::getDeleted,0)
                .ge(VacMachine::getBoxNo,startBoxNo)
                .le(VacMachine::getBoxNo,endBoxNo)
                .orderByAsc(VacMachine::getBoxNo);

        List<VacMachine> vacMachineList = vacMachineMapper.selectList(queryWrapper);
        log.info(JSON.toJSONString(vacMachineList));
        if(vacMachineList.isEmpty()){
            log.warn("没有符合的仓位");
            return Result.fail("没有符合的仓位");
        }
        Map<String,Object> map = new HashMap<>();
        map.put("autoX",-1);
        map.put("error","false");
        valueOperations.set(RedisKeyConstant.AUTO_IS_START,"true");

        for(VacMachine vacMachine :vacMachineList){
            int autoX = (int) map.get("autoX");
            String error = (String) map.get("error");
            if(autoX==-1 && "false".equals(error)){
                if(vacMachine.getAutoX()==null){
                    log.warn("第一个仓位autoX 不能为空");
                    return Result.fail("第一个仓位autoX 不能为空");
                }
                log.info("第一个仓位 直接开始程序");
                autoX = vacMachine.getAutoX();
                map.put("autoX",autoX);

            }else {
                //如果下位机获取servoX 成功 则继续操作
                if("true".equals(error)){
                    log.error("距离传感器没有获取到仓位：{} 数据 请手动校对",vacMachine.getBoxNo());
                }else {
                    log.info("仓位：{} 仓位原始数据：{}  计算的新数据：{}  误差：{}   X的值：{} 上个仓位规格：{}",vacMachine.getBoxNo(),vacMachine.getAutoX(),autoX,vacMachine.getAutoX()-autoX,Integer.parseInt(Objects.requireNonNull(valueOperations.get(RedisKeyConstant.distance.AUTO))),vacMachine.getBoxSpecName());
                    //更新数据库
                    vacMachine.setAutoX(autoX);
                    vacMachineMapper.updateById(vacMachine);
                }

            }

            ConfigData configData = configFunction.getAutoDrugConfigData();
            //如果 仓位跟X轴方向一致
            if("true".equals(configData.getDropXAdd())){
                getAuto(autoX,vacMachine.getBoxSpecId(),data,map);
            }else {
                getAutoReturn(autoX,vacMachine.getBoxSpecId(),data,map);
            }

        }


        valueOperations.set(RedisKeyConstant.AUTO_IS_START,"false");
        return Result.success();

    }

    @Override
    public void blankController(OtherRequest request) {
        if(request.getType()==1){
            dispensingFunction.openBlank();
        }else {
            dispensingFunction.closeBlank();
        }
    }

    @Override
    public void autoChangeAutoZ(OtherRequest request) {
        List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted,0)
                .eq(VacMachine::getLineNum,request.getLine()));
        for(VacMachine record:vacMachineList){

            record.setAutoZ(request.getAutoZ());
            vacMachineMapper.updateById(record);
        }


    }

    @Override
    public void test4() {
        ConfigSendData configSendData = configFunction.getSendDrugConfigData();

        while (true){
            //输出
            OutPutRequest outPutRequest = new OutPutRequest();
            outPutRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
            outPutRequest.setCommand(CabinetConstants.OutPutCommand.OUTPUT);

            //关闭
            outPutRequest.setMode(16);
            outPutRequest.setCabinet(CabinetConstants.Cabinet.CAB_A);
            cabinetAService.outPut(outPutRequest);

            VacUntil.sleep(2000);
            outPutRequest.setCommand(CabinetConstants.OutPutCommand.NOT_OUTPUT);
            cabinetAService.outPut(outPutRequest);
            //关闭挡片





            dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.BELT_STOP,configSendData.getSmallBeltStopSpeed());
            int countSend = 0;
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
                    countSend++;
                    if(countSend>=3){
                        dropFlag = true;
                        break;
                    }

                }

            }

            if(!dropFlag){
                //如果规定时间后，还是没有收到光栅触发的信号，电磁铁出问题 禁用该仓位
                String errorMsg =  String.format("没有检测到药品");
                log.error(errorMsg);
                vacMachineExceptionService.dropException(SettingConstants.MachineException.IO.code,null,errorMsg);
            }

            outPutRequest.setMode(15);
            outPutRequest.setCommand(CabinetConstants.OutPutCommand.OUTPUT);
            cabinetAService.outPut(outPutRequest);

            dispensingFunction.
            //运动伺服 使疫苗落到运输皮带上
            speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.SPEED,CabinetConstants.CabinetAServoStatus.COROTATION,configSendData.getSmallBeltGoCSpeed());

            VacUntil.sleep(2000);
            outPutRequest.setCommand(CabinetConstants.OutPutCommand.NOT_OUTPUT);
            cabinetAService.outPut(outPutRequest);
            VacUntil.sleep(200);
            dispensingFunction.speedServo(SettingConstants.CABINET_A_MOVE_BELT_TO_C_NUM,CabinetConstants.CabinetAServoCommand.PAUSE,CabinetConstants.CabinetAServoStatus.COROTATION,configSendData.getSmallBeltGoCSpeed());
        }











    }


    private int getAuto( Integer autoX, Long boxSpecId,AutoData data,Map<String,Object> maps){

        CabinetAGetDistanceRequest request = new CabinetAGetDistanceRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAGetDistanceCommand.AUTO);
        request.setMode(1);
        //伺服id
        request.setStatus(7);
        //拿到第一个仓位的数据
        VacBoxSpec vacBoxSpec = vacBoxSpecService.getById(boxSpecId);
        //30(3 30) 规格是30mm  伺服走到距离 10 为1mm
        Integer  boxDis = Integer.parseInt(vacBoxSpec.getName().replaceAll(".*\\s(\\d+)\\).*", "$1"))*10;
        log.info("boXDis:{}",boxDis);
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        Integer py = configSetting.getInventoryPyValue();
        Integer startDis = autoX+boxDis-100-py;
        Integer endDis = autoX+boxDis+100-py;
        //正负1cm范围
        request.setStartDis(startDis);
        request.setEndDis(endDis);
        log.info("{}+{}-100-{}={}",autoX,boxDis,py,startDis);
        log.info("伺服走的范围：{}-{}",startDis,endDis);

        request.setSensorDis(data.getSensorDis());
        request.setSpeed(data.getSpeed());
        request.setThreshold(data.getThreshold());

        valueOperations.set(RedisKeyConstant.distanceStart.AUTO,"false");
        valueOperations.set(RedisKeyConstant.distance.AUTO,"-1");

        log.info("发送参数：{}",JSON.toJSONString(request));
        cabinetAService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_AUTO_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.AUTO)) ) {
                break;
            }
            VacUntil.sleep(50);
        }

        String autoSensorDis = valueOperations.get(RedisKeyConstant.distance.AUTO);
        int servoXDis;
        if("ERROR".equals(autoSensorDis)||"-1".equals(autoSensorDis)){
            //测试逻辑先
            servoXDis = autoX+boxDis+data.getOffsetDis();
            maps.put("error","true");
        }else {
            //拿到的值+偏移量
            assert autoSensorDis != null;
            servoXDis = Integer.parseInt(autoSensorDis)+data.getOffsetDis()+py;
            maps.put("error","false");
        }

        maps.put("autoX",servoXDis);
        return  servoXDis;

    }


    private int getAutoReturn( Integer autoX, Long boxSpecId,AutoData data,Map<String,Object> maps){

        CabinetAGetDistanceRequest request = new CabinetAGetDistanceRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.CabinetAGetDistanceCommand.AUTO);
        request.setMode(1);
        //伺服id
        request.setStatus(7);
        //拿到第一个仓位的数据
        VacBoxSpec vacBoxSpec = vacBoxSpecService.getById(boxSpecId);
        //30(3 30) 规格是30mm  伺服走到距离 10 为1mm
        Integer  boxDis = Integer.parseInt(vacBoxSpec.getName().replaceAll(".*\\s(\\d+)\\).*", "$1"))*10;
        log.info("boXDis:{}",boxDis);
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        //激光跟右边侧板的偏移量
        Integer py = configSetting.getInventoryPyValue();

        //正负1cm范围
        request.setStartDis(autoX-boxDis-100+py);

        request.setEndDis(autoX-boxDis+100+py);

        request.setSensorDis(data.getSensorDis());
        request.setSpeed(data.getSpeed());
        request.setThreshold(data.getThreshold());

        valueOperations.set(RedisKeyConstant.distanceStart.AUTO,"false");
        valueOperations.set(RedisKeyConstant.distance.AUTO,"-1");

        log.info("发送参数：{}",JSON.toJSONString(request));
        cabinetAService.getDistance(request);
        long timeout = System.currentTimeMillis();
        while ((System.currentTimeMillis() - timeout) < SettingConstants.GET_DISTANCE_AUTO_WAIT_TIME) {
            if ("true".equals(valueOperations.get(RedisKeyConstant.distanceStart.AUTO)) ) {
                break;
            }
            VacUntil.sleep(50);
        }

        String autoSensorDis = valueOperations.get(RedisKeyConstant.distance.AUTO);
        int servoXDis;
        if("ERROR".equals(autoSensorDis)||"-1".equals(autoSensorDis)){
            //测试逻辑先
            servoXDis = autoX-boxDis-data.getOffsetDis();
            maps.put("error","true");
        }else {
            //拿到的值-偏移量
            assert autoSensorDis != null;
            servoXDis = Integer.parseInt(autoSensorDis)-data.getOffsetDis()-py;
            maps.put("error","false");
        }

        maps.put("autoX",servoXDis);
        return  servoXDis;
    }


    //常规检测传感器状态以及处理
    private void sensorReturn(){
        //查询传感器状态
        InPutRequest request = new InPutRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        request.setCabinet(CabinetConstants.Cabinet.CAB_C);
        request.setCommand(CabinetConstants.InPutCommand.QUERY);
        request.setMode(SettingConstants.IS_SENSOR_INPUT_WARING_NUM);
        cabinetAService.intPut(request);
        VacUntil.sleep(500);

        //底部蜂鸣器输出
        OutPutRequest outPutRequest = new OutPutRequest();
        outPutRequest.setWorkMode(CabinetConstants.Cabinet.CAB_C);
        outPutRequest.setCabinet(CabinetConstants.Cabinet.CAB_C);
        outPutRequest.setCommand( CabinetConstants.OutPutCommand.OUTPUT);
        outPutRequest.setMode(SettingConstants.IS_SENSOR_OUTPUT_WARING_NUM);
        //如果传感器触发 蜂鸣器告警
        if("true".equals(valueOperations.get(String.format(RedisKeyConstant.sensor.SENSOR_CABINET_C_NUM,SettingConstants.IS_SENSOR_INPUT_WARING_NUM)))){
            //蜂鸣器告警
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SENDWARING.code,"传送轨道底部发药告警");
        }else {
            //清除告警
            outPutRequest.setCommand( CabinetConstants.OutPutCommand.NOT_OUTPUT);
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.delExceptionByCodeAndDesc(SettingConstants.MachineException.SENDWARING.code,"传送轨道底部发药告警");
        }
    }


    //浦沿检测传感器状态以及处理
    private void puYanSenorReturn(){
        //查询传感器状态 27 28
        InPutRequest request = new InPutRequest();
        request.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        request.setCabinet(CabinetConstants.Cabinet.CAB_A);
        request.setCommand(CabinetConstants.InPutCommand.QUERY);
        request.setMode(SettingConstants.IS_SENSOR_INPUT_WARING_NUM);
        cabinetAService.intPut(request);
        request.setMode(SettingConstants.IS_SENSOR_INPUT_WARING_TWO_NUM);
        cabinetAService.intPut(request);

        VacUntil.sleep(1000);
        //底部蜂鸣器输出
        OutPutRequest outPutRequest = new OutPutRequest();
        outPutRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
        outPutRequest.setCabinet(CabinetConstants.Cabinet.CAB_A);


        //如果传感器触发 蜂鸣器告警 27 输出 14 1-4工作台
        if(CabinetConstants.SensorStatus.NORMAL.code.equals(valueOperations.get(String.format(RedisKeyConstant.sensor.SENSOR_CABINET_A_NUM,SettingConstants.IS_SENSOR_INPUT_WARING_TWO_NUM)))){
            outPutRequest.setCommand( CabinetConstants.OutPutCommand.OUTPUT);
            outPutRequest.setMode(SettingConstants.IS_SENSOR_OUTPUT_WARING_NUM);
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SENDWARING.code,"1-4传送轨道底部发药告警");
        }else {
            //清除告警
            outPutRequest.setMode(SettingConstants.IS_SENSOR_OUTPUT_WARING_NUM);
            outPutRequest.setCommand( CabinetConstants.OutPutCommand.NOT_OUTPUT);
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.delExceptionByCodeAndDesc(SettingConstants.MachineException.SENDWARING.code,"1-4传送轨道底部发药告警");
        }

        VacUntil.sleep(500);

        //如果传感器触发 蜂鸣器告警 28 输出 15 5-6工作台
        if(CabinetConstants.SensorStatus.NORMAL.code.equals(valueOperations.get(String.format(RedisKeyConstant.sensor.SENSOR_CABINET_A_NUM,SettingConstants.IS_SENSOR_INPUT_WARING_NUM)))){
            outPutRequest.setCommand( CabinetConstants.OutPutCommand.OUTPUT);
            outPutRequest.setMode(SettingConstants.IS_SENSOR_OUTPUT_WARING_TWO_NUM);
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.sendException(SettingConstants.MachineException.SENDWARING.code,"5-6传送轨道底部发药告警");
        }else {
            //清除告警
            outPutRequest.setCommand( CabinetConstants.OutPutCommand.NOT_OUTPUT);
            outPutRequest.setMode(SettingConstants.IS_SENSOR_OUTPUT_WARING_TWO_NUM);
            cabinetAService.outPut(outPutRequest);
            vacMachineExceptionService.delExceptionByCodeAndDesc(SettingConstants.MachineException.SENDWARING.code,"5-6传送轨道底部发药告警");

        }

    }



    //有效期一致的苗仓
    private  List<VacMachine> getExpiredAtBoxNoBatchNo(List<Long> boxSepcIds, Integer num, Date expiredAt, String productNo , String batchNo){
        log.info("{},{},{},{},{}",boxSepcIds,num,expiredAt,productNo,batchNo);
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
                .orderByDesc(VacMachine::getVaccineNum);
        getACYW(queryWrapper,productNo,boxSepcIds);
        queryWrapper.orderByAsc(VacMachine::getBoxNo);
        return vacMachineMapper.selectList(queryWrapper);

    }


    //找一个新的药仓
    private  List<VacMachine> getNewBoxNo(List<Long> boxSepcIds,String productNo){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .isNull(VacMachine::getProductNo)
                .and(wp->wp.isNull(VacMachine::getVaccineNum).or().eq(VacMachine::getVaccineNum, 0))
                .eq(VacMachine::getStatus,1);

        getACYW(queryWrapper,productNo,boxSepcIds);
        queryWrapper.orderByAsc(VacMachine::getBoxSpecName);
        queryWrapper.orderByAsc(VacMachine::getBoxNo);

        return vacMachineMapper.selectList(queryWrapper);

    }

    private  List<VacMachine> getBoxNoNullBatchNo(List<Long> boxSepcIds,String productNo,Date expiredAt, Integer num){
        LambdaQueryWrapper<VacMachine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacMachine::getDeleted,0)
                .in(VacMachine::getBoxSpecId,boxSepcIds)
                .eq(VacMachine::getProductNo,productNo)
                .eq(VacMachine::getExpiredAt,expiredAt)
                .isNull(VacMachine::getBatchNo)
                .eq(VacMachine::getStatus,1)
                .lt(VacMachine::getVaccineNum,num);
        getACYW(queryWrapper,productNo,boxSepcIds);
        queryWrapper.orderByAsc(VacMachine::getBoxNo);
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
                .orderByAsc(VacMachine::getVaccineNum);

        getACYW(queryWrapper,productNo,boxSepcIds);
        //优先放最近的仓位
        queryWrapper.orderByAsc(VacMachine::getBoxNo);
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
                .orderByDesc(VacMachine::getVaccineNum);

        getACYW(queryWrapper,productNo,boxSepcIds);
        queryWrapper.orderByAsc(VacMachine::getBoxNo);
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
                .orderByDesc(VacMachine::getVaccineNum);

        getACYW(queryWrapper,productNo,boxSepcIds);
        queryWrapper.orderByAsc(VacMachine::getBoxNo);

        return vacMachineMapper.selectList(queryWrapper);
    }

    private void  getACYW(LambdaQueryWrapper<VacMachine> queryWrapper,String productNo,List<Long> boxSepcIds){

        //禁止ACYW 小药盒上第10层
        if("81576000501".equals(productNo)){
            queryWrapper.in(VacMachine::getLineNum, List.of(10));
            queryWrapper.in(VacMachine::getBoxNo, Arrays.asList("A1001", "A1005", "A1010"));
            queryWrapper.orderByDesc(VacMachine::getLineNum);

        }else if("82926000101".equals(productNo)){
            //鼻喷上偶数仓
            queryWrapper.orderByAsc(VacMachine::getLineNum);

            if (boxSepcIds != null && boxSepcIds.contains(14L)) {
                queryWrapper.notIn(VacMachine::getLineNum, List.of(10));
                queryWrapper.notIn(VacMachine::getBoxNo, Arrays.asList("A1001", "A1005", "A1010"));
            }
            queryWrapper .apply("line_num % 2 = 0");
        }
        else {
            queryWrapper.orderByAsc(VacMachine::getLineNum);
            if (boxSepcIds != null && boxSepcIds.contains(14L)) {
                queryWrapper.notIn(VacMachine::getLineNum, Arrays.asList(8, 10));
            }
        }
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
        if(vacMachineData.getBatchNo()!=null){
            request.setBatchNo(vacMachineData.getBatchNo());
        }

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

    @Override
    public List<String> getBatchListNum(List<String> batchNoList) {
        // 创建返回结果列表
        List<String> result = new ArrayList<>();
        
        if (batchNoList == null || batchNoList.isEmpty()) {
            return result;
        }
        
        // 查询所有指定batchNo的vacMachine记录
        List<VacMachine> vacMachineList = vacMachineMapper.selectList(new LambdaQueryWrapper<VacMachine>()
                .eq(VacMachine::getDeleted, 0)
                .in(VacMachine::getBatchNo, batchNoList)
                .isNotNull(VacMachine::getVaccineUseNum));
        
        if (vacMachineList.isEmpty()) {
            return result;
        }
        
        // 根据batchNo分组，求和vaccineUseNum
        Map<String, Integer> batchSumMap = vacMachineList.stream()
                .collect(Collectors.groupingBy(
                        VacMachine::getBatchNo,
                        Collectors.summingInt(VacMachine::getVaccineUseNum)
                ));
        
        // 找到最小的总和值
        Optional<Integer> minSumOpt = batchSumMap.values().stream().min(Integer::compareTo);
        if (minSumOpt.isPresent()) {
            int minSum = minSumOpt.get();
            
            // 将所有总和等于最小值的batchNo加入结果列表
            for (Map.Entry<String, Integer> entry : batchSumMap.entrySet()) {
                if (entry.getValue() == minSum) {
                    result.add(entry.getKey());
                }
            }
        }
        
        return result;
    }

}