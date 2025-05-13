package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.InventoryReportData;

import java.util.List;

/**
 * @author hhd
 **/
public interface VacDrugRecordService extends IService<VacDrugRecord>{
    //上药列表
    Page<VacDrugRecord> drugRecordList(DrugRecordRequest request);

    //上药统计 0 1周/1 1个月/2 一年

    Result drugRecordCount(Integer type);


    //自动上药添加上药记录
    void addDrugRecord(DrugRecordRequest request);


    //获取最早的入药记录
    VacDrugRecord getListByMachineIdAndProductNo(Long machineId,String productNo);


    //获取最近的入药记录
    VacDrugRecord getLastByMachineId(Long machineId);


    //药发出以后更新状态
    void  updateStatusById(Long id,String status);



    //库存盘点查找电子监管码 是否发出
    void findVaccineByCode(String code);

    //机器库存上报

    List<InventoryReportData> getInventoryReport();


    //批量退药 将苗状态改为 2
    void updateStatusByProductNo(VacMachine vacMachine);








}
