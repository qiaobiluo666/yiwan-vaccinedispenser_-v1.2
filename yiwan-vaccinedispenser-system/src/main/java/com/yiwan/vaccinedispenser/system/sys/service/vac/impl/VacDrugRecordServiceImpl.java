package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugRecordMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.InventoryReportData;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author
 **/
@Service
@Slf4j
public class VacDrugRecordServiceImpl extends ServiceImpl<VacDrugRecordMapper, VacDrugRecord> implements VacDrugRecordService {

    @Autowired
    private VacDrugRecordMapper vacDrugRecordMapper;

    @Autowired
    private VacMachineService vacMachineService;

    @Override
    public Page<VacDrugRecord> drugRecordList(DrugRecordRequest request) {

        IPage<VacDrugRecord> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacDrugRecord> wrapper = new LambdaQueryWrapper<>();

        //产品名称
        if(StringUtils.isNotBlank(request.getProductName())){
            wrapper.like(VacDrugRecord::getProductName,request.getProductName());
        }

        //机器编号
        if(StringUtils.isNotBlank(request.getMachineNo())){
            wrapper.like(VacDrugRecord::getMachineNo,request.getMachineNo());
        }

        //电子监管码
        if(StringUtils.isNotBlank(request.getSupervisedCode())){
            wrapper.like(VacDrugRecord::getSupervisedCode,request.getSupervisedCode());
        }

        //批次
        if(StringUtils.isNotBlank(request.getBatchNo())){
            wrapper.eq(VacDrugRecord::getBatchNo,request.getBatchNo());
        }


        //创建时间
        if(request.getCreateTimeStart()!=null){
            wrapper.gt(VacDrugRecord::getCreateTime,request.getCreateTimeStart());
        }

        //创建时间
        if(request.getCreateTimeEnd()!=null){
            wrapper.lt(VacDrugRecord::getCreateTime,request.getCreateTimeEnd());
        }

        wrapper.eq(VacDrugRecord::getDeleted,0);
        wrapper.orderByDesc(VacDrugRecord::getCreateTime);
        IPage<VacDrugRecord> vacBoxSpecIPage = vacDrugRecordMapper.selectPage(page, wrapper);
        return (Page<VacDrugRecord>) vacBoxSpecIPage;

    }

    @Override
    public Result drugRecordCount(Integer type) {

        List<Map<String, Object>> typeList;
        if(type==0){
            typeList = vacDrugRecordMapper.getWeeklyCountForType0();
        }else if(type==1){
            typeList = vacDrugRecordMapper.getDailyCountForType1();
        }else {
            typeList = vacDrugRecordMapper.getMonthlyCountForType2();
        }

        return Result.success(typeList);
    }

    @Override
    public void addDrugRecord(DrugRecordRequest request) {
        request.setStatus("0");
        VacDrugRecord vacDrugRecord = new VacDrugRecord();
        BeanUtils.copyProperties(request,vacDrugRecord);
        vacDrugRecord.setCreateTime(LocalDateTime.now());
        vacDrugRecord.setUpdateTime(LocalDateTime.now());
        vacDrugRecordMapper.insert(vacDrugRecord);

    }

    @Override
    public VacDrugRecord getListByMachineIdAndProductNo(Long machineId,String productNo) {

        LambdaQueryWrapper<VacDrugRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(VacDrugRecord::getDeleted,0)
                .eq(VacDrugRecord::getMachineId,machineId)
                .eq(VacDrugRecord::getProductNo,productNo)
                .eq(VacDrugRecord::getStatus,"0")
                .orderByAsc(VacDrugRecord::getCreateTime);
        List<VacDrugRecord> vacDrugRecordList = vacDrugRecordMapper.selectList(lambdaQueryWrapper);
        if(vacDrugRecordList.isEmpty()){
            return null;
        }else {
            return vacDrugRecordList.get(0);
        }
    }

    @Override
    public VacDrugRecord getLastByMachineId(Long machineId) {

        LambdaQueryWrapper<VacDrugRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(VacDrugRecord::getMachineId,machineId)
                .orderByDesc(VacDrugRecord::getCreateTime);
        List<VacDrugRecord> vacDrugRecordList = vacDrugRecordMapper.selectList(lambdaQueryWrapper);
        if(vacDrugRecordList.isEmpty()){
            return null;
        }else {
            return vacDrugRecordList.get(0);
        }
    }

    @Override
    public void updateStatusById(Long id,String status) {

        VacDrugRecord vacDrugRecord = new VacDrugRecord();
        vacDrugRecord.setId(id);
        vacDrugRecord.setStatus(status);
        vacDrugRecordMapper.updateById(vacDrugRecord);

    }

    @Override
    public void findVaccineByCode(String code) {

        LambdaQueryWrapper<VacDrugRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacDrugRecord::getDeleted,0)
                .eq(VacDrugRecord::getSupervisedCode,code)
                .orderByDesc(VacDrugRecord::getCreateTime);

        List<VacDrugRecord> vacDrugRecordList = vacDrugRecordMapper.selectList(lambdaQueryWrapper);
        if(vacDrugRecordList.isEmpty()){
            //机器上没有这个上药记录，医生手动在其他地方拿
            log.warn("机器上没有这个上药记录!");

        }else {

            for(VacDrugRecord record : vacDrugRecordList) {
                if (!"1".equals(record.getStatus())) {

                    //将药品改为发药状态
                    updateStatusById(record.getId(),"1");
                    //获取该仓位的数量
                    LambdaQueryWrapper<VacDrugRecord> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(VacDrugRecord::getMachineId, record.getMachineId())
                            .eq(VacDrugRecord::getStatus, "0")
                            .eq(VacDrugRecord::getDeleted,0);

                    int num =  vacDrugRecordMapper.selectCount(queryWrapper);

                    log.info(String.valueOf(num));
                    //根据仓位库存核对
                    vacMachineService.updateByIdAndNum(record.getMachineId(),num);

                }
            }


        }
    }

    @Override
    public List<InventoryReportData> getInventoryReport() {
        return vacDrugRecordMapper.selectGroupedRecords();
    }

    @Override
    public void updateStatusByProductNo(VacMachine vacMachine) {

            VacDrugRecord vacDrugRecord = new VacDrugRecord();
            vacDrugRecord.setStatus("2");
            vacDrugRecordMapper.update(vacDrugRecord,new UpdateWrapper<VacDrugRecord>()
                    .eq("status","0").eq("deleted","0")
                    .eq("product_no",vacMachine.getProductNo()));
    }


}








