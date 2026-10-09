package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.InventoryReportData;

import java.util.Date;
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

    /**
     * 自动上药确认落仓后的事务提交：原子增加仓位库存并写入上药记录。
     *
     * @return 1 表示仓位库存和上药记录均已提交
     */
    int addAutoDrugRecordAndInventory(DrugRecordRequest request, int status, int maxCapacity);


    //获取最早的入药记录
    VacDrugRecord getListByMachineIdAndProductNo(Long machineId,String productNo);

    VacDrugRecord getListByMachineIdAndProductNoAndExpiredAt(Long machineId,String productNo,Date expiredAt);
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



    /**
     * 在同一有效期的候选批次中，返回第一次上药时间最早的批号。
     */
    String getBatchNoEarly(List<String> batchNoList, List<String> productNoList, Date expiredAt);


    DrugRecordRequest countTodayGroupedByProductId(String productNo);

    List<DrugRecordRequest> countToday();


    void updateBatchNo();

    List<VacDrugRecord> drugRecordListByCreateTime(Date createTimeStart, Date createTimeEnd);

    List<DrugRecordRequest> drugRecordTotalListByCreateTime(Date createTimeStart, Date createTimeEnd);


}
