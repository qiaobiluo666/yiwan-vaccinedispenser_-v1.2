package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineDrug;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineDrugMapper;

import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineDrugService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 78671
 */
@Service
@Slf4j
public class VacMachineDrugServiceImpl extends ServiceImpl<VacMachineDrugMapper, VacMachineDrug> implements VacMachineDrugService {

    @Autowired
    private VacMachineDrugMapper vacMachineDrugMapper;


    @Override
    public void addNum(DrugRecordRequest request, int num) {
        VacMachineDrug vacMachineDrug = new VacMachineDrug();
        vacMachineDrug.setMachineId(request.getMachineId());
        vacMachineDrug.setVaccineId(request.getVaccineId());
        vacMachineDrug.setNum(num);
        vacMachineDrugMapper.insert(vacMachineDrug);

    }

    @Override
    public void delMachineByCreatTime(long machineId) {

        vacMachineDrugMapper.updateDeletedToOneByMachineId(machineId);
    }

    @Override
    public Integer getTotalNumByMachineId(Long machineId) {
        return vacMachineDrugMapper.sumNumByMachineId(machineId);

    }


    @Override
    public Integer getTotalNumByVaccineId(Long vaccineId) {
        return vacMachineDrugMapper.sumNumByVaccineId(vaccineId);

    }
}
