package com.yiwan.vaccinedispenser.system.sys.service.vac;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineDrug;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;

/**
 * @author 78671
 */
public interface VacMachineDrugService extends IService<VacMachineDrug> {


    //增加多人份散装数量明细
    void addNum(DrugRecordRequest request ,int num);

    //删除最近的多人份散装数量记录
    void delMachineByCreatTime(long machineId);

    //获取多人份散装总数
    Integer getTotalNumByMachineId(Long machineId);


    Integer getTotalNumByVaccineId(Long vaccineId);


}
