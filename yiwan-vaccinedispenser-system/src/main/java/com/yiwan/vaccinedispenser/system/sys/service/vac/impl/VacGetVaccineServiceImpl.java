package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.sys.dao.VacGetVaccineMapper;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacGetVaccineService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author
 **/
@Service
@Slf4j
public class VacGetVaccineServiceImpl extends ServiceImpl<VacGetVaccineMapper, VacGetVaccine> implements VacGetVaccineService {

    @Autowired
    private VacGetVaccineMapper vacGetVaccineMapper;

    @Autowired
    private ZcyFunction zcyFunction;
    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private VacDrugService vacDrugService;


    @Override
    public VacGetVaccine getMsgByTaskId(String taskId) {
        LambdaQueryWrapper<VacGetVaccine> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(VacGetVaccine::getTaskId,taskId)
                .eq(VacGetVaccine::getDeleted,0);
        List<VacGetVaccine> vacGetVaccineList = vacGetVaccineMapper.selectList(lambdaQueryWrapper);
        if(vacGetVaccineList.isEmpty()){
            return null;
        }else {
            return vacGetVaccineList.get(0);
        }

    }

    @Override
    public void updateById(Long id) {
        VacGetVaccine vacGetVaccine = new VacGetVaccine();
        vacGetVaccine.setStatus("0");
        vacGetVaccine.setId(id);
        vacGetVaccineMapper.updateById(vacGetVaccine);
    }

    @Override
    public VacGetVaccine insertMsg(VacGetVaccine vacGetVaccine, List<String> productNoList) throws Exception {
            //TODO 工作台异常

            //查找工作台编号 以及 统计药仓数量
            List<VacGetVaccine> vacGetVaccineList = vacGetVaccineMapper.findProductNo(productNoList,vacGetVaccine.getWorkbenchNo());

            if(vacGetVaccineList.isEmpty()){

                //机器没有库存 查看药品名称 厂家
                VacDrug vacDrug = vacDrugService.vacDrugGetByproductNo(vacGetVaccine.getProductNo());


                //返回没有药品
//                zcyFunction.sendResult(vacGetVaccine);
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                commandData.put("data", vacGetVaccine);
                websocketService.sendInfo(CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_START.getCode(),commandData);

                String msg;
                if(vacDrug==null){
                    msg= String.format("机器没有库存！未知药品编号：%s",vacGetVaccine.getProductNo());
                }else {
                    msg= String.format("机器没有库存：药品：%s 厂家：%s   药品编号：%s",vacDrug.getProductName(),vacDrug.getManufacturerName(),vacDrug.getProductNo());
                }

                //如果机器上没有药 直接返回机器上无药
                log.error(msg);
                vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDWARING.code,null,msg);
                zcyFunction.sendResult(vacGetVaccine,"机器没有库存");
                return null;
            }else {
                VacGetVaccine vacGetVaccineData = vacGetVaccineList.get(0);
                vacGetVaccine.setProductNo(vacGetVaccineData.getProductNo());
                vacGetVaccine.setWorkbenchName(vacGetVaccineData.getWorkbenchName());
                vacGetVaccine.setWorkbenchNo(vacGetVaccineData.getWorkbenchNo());
                vacGetVaccine.setWorkbenchNum(vacGetVaccineData.getWorkbenchNum());
                vacGetVaccine.setProductName(vacGetVaccineData.getProductName());
                //查找工作台
                vacGetVaccineMapper.insert(vacGetVaccine);
                return vacGetVaccine;
            }

    }
}








