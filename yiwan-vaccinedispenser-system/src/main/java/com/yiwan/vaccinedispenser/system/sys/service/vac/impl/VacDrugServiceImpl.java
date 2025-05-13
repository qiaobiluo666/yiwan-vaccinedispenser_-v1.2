package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author
 **/
@Service
@Slf4j
public class VacDrugServiceImpl extends ServiceImpl<VacDrugMapper, VacDrug> implements VacDrugService {

    @Autowired
    private  VacDrugMapper vacDrugMapper;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Override
    public Page<VacDrug> vacDrugList(DrugListRequest request) {
        IPage<VacDrug> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacDrug> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(request.getProductName())){
            wrapper.like(VacDrug::getProductName,request.getProductName());
        }
        if(StringUtils.isNotBlank(request.getVaccineMinorName())){
            wrapper.like(VacDrug::getVaccineMinorName,request.getVaccineMinorName());
        }
        wrapper.eq(VacDrug::getDeleted,0);
        IPage<VacDrug> vacDrugIPage = vacDrugMapper.selectPage(page, wrapper);

        return (Page<VacDrug>) vacDrugIPage;
    }

    @Override
    public Result vacDrugAdd(DrugListRequest request, UserBean user) {
        List<VacDrug> vacDrugs = vacDrugMapper.selectList(new LambdaQueryWrapper<VacDrug>()
                .eq(VacDrug ::getProductNo,request.getProductNo())
                .eq(VacDrug::getDeleted,0));

        if(!vacDrugs.isEmpty()){
            return Result.fail("该疫苗编号已经存在");
        }
        VacDrug vacDrug = new VacDrug();
        BeanUtils.copyProperties(request, vacDrug);
        vacDrug.setCreateBy(user.getUserName());
        vacDrug.setUpdateBy(user.getUserName());
        int result = vacDrugMapper.insert(vacDrug);
        if(result>0){
            return Result.success();
        }else {
            return Result.fail("添加疫苗异常！");
        }


    }

    @Override
    public Result vacDrugEdit(DrugListRequest request, UserBean user) {
        VacDrug vacDrug = new VacDrug();
        BeanUtils.copyProperties(request, vacDrug);
        vacDrug.setUpdateBy(user.getUserName());
        int result = vacDrugMapper.updateById(vacDrug);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑疫苗异常！");
        }
    }

    @Override
    public Result vacDrugDel(IdListRequest request, UserBean user) {

        // 查询要删除的记录
        List<VacDrug> vacDrugsToDelete = vacDrugMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacDrug vacDrug : vacDrugsToDelete) {
            vacDrug.setUpdateBy(user.getUserName());
            vacDrug.setDeleted(1);
            vacDrug.setUpdateTime(LocalDateTime.now());
            result = vacDrugMapper.updateById(vacDrug);
            if(result<=0){
                flag=1;
            }
        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("删除疫苗异常！");
        }
    }

    @Override
    public VacDrug vacDrugGetByproductNo(String productNo) {
        List<VacDrug> vacDrugList = vacDrugMapper.selectList(new LambdaQueryWrapper<VacDrug>().eq(VacDrug::getProductNo,productNo).eq(VacDrug::getDeleted,0));
        if(vacDrugList.isEmpty()){
            return null;
        }else {
            return vacDrugList.get(0);
        }
    }

    @Override
    public void vacSaveOrUpdateDrug(VacDrug vacDrug) {
        LambdaQueryWrapper<VacDrug> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacDrug::getDeleted,0)
                .eq(VacDrug::getProductNo,vacDrug.getProductNo())
                .eq(VacDrug::getProductName,vacDrug.getProductName())
                .eq(VacDrug::getVaccineTypeCode,vacDrug.getVaccineTypeCode());
        List<VacDrug> vacDrugList= vacDrugMapper.selectList(lambdaQueryWrapper);
        if(vacDrugList.isEmpty()){
            vacDrugMapper.insert(vacDrug);
        }else {
            vacDrug.setId(vacDrugList.get(0).getId());
            vacDrugMapper.updateById(vacDrug);
        }
    }

    @Override
    public DrugRecordRequest sendDrugTest(String code) {

        LambdaQueryWrapper<VacDrug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VacDrug::getDeleted,0)
                .like(VacDrug::getVaccineCode,code.substring(0, 7));

        List<VacDrug> vacDrugList = vacDrugMapper.selectList(queryWrapper);
        DrugRecordRequest request = new DrugRecordRequest();
        if(vacDrugList.isEmpty()){
            request.setIsReturn(true);
        }else {
            BeanUtils.copyProperties(vacDrugList.get(0),request);
            request.setIsReturn(false);
        }
        return  request;


    }

    @Override
    public Result drugDistance(String code) throws Exception {

        ConfigSetting configSetting = configFunction.getSettingConfigData();
        //跟政采云扫码 获得 药品信息
        DrugRecordRequest drugRecordData;

        if("true".equals(configSetting.getZcyAuto())){
//        if("true".equals(isSendOpen)){
            drugRecordData = zcyFunction.getVaccineMsgByCode(code);
            log.info(JSON.toJSONString(drugRecordData));
            if(drugRecordData.getIsReturn()){
                //电子监管码请求失败
                //TODO 没有仓位可以装这个药
                String msg = "药盒测距异常：政采云电子监管码请求失败："+drugRecordData.getProductNo();
                log.error(msg);
                return Result.fail(msg);
            }

        }else {
            //测试使用
            drugRecordData = sendDrugTest(code);
            drugRecordData.setExpiredAt(new Date());
            drugRecordData.setBatchNo("测试编号");
            drugRecordData.setPrice(String.valueOf(321));
            drugRecordData.setTag("测试标签");
            drugRecordData.setSupervisedCode(code);
            if(drugRecordData.getIsReturn()){
                String msg = "药盒测距异常：没有测试电子监管码："+code;
                log.error(msg);
                return Result.fail(msg);
            }
        }
        //拿到疫苗信息
        VacDrug vacDrug = vacDrugGetByproductNo(drugRecordData.getProductNo());
        vacDrug = sendDrugFunction.drugDistance(vacDrug);

        if(vacDrug==null){
            return Result.fail("未检测到药盒");
        }
        log.info("药盒测苗数据：{}",JSON.toJSONString(vacDrug));
        return Result.success(vacDrug);
    }

    @Override
    public List<VacDrug> getVaccinePdf() {
        LambdaQueryWrapper<VacDrug> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(VacDrug::getVaccineLong);
        wrapper.eq(VacDrug::getDeleted,"0");
        return vacDrugMapper.selectList(wrapper);
    }


}








