package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.InventoryResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


import java.util.List;

/**
 * <p>
 * 药仓规格表 Mapper 接口
 * </p>
 *
 * @author vicente
 * @since 2023-05-09
 */
public interface VacMachineMapper extends BaseMapper<VacMachine> {

    /**
     *
     * @param id
     * 根据id 药仓库存-1
     */
    void  decrementNumById(Long id);

    /**
     * 自动上药最终入库：仓位未达到最大容量时，原子增加真实库存和可用库存。
     *
     * @return 1 表示更新成功，0 表示仓位不存在、已删除或已经达到最大容量
     */
    int incrementInventoryIfBelowMax(
            @Param("request") DrugRecordRequest request,
            @Param("status") int status,
            @Param("maxCapacity") int maxCapacity);

    /**
     * 发药预占：只原子减少可用库存，不回写真实库存及仓位其他字段。
     *
     * @return 1 表示更新成功，0 表示仓位不存在、已删除或可用库存不足
     */
    int decrementVaccineUseNumById(@Param("machineId") Long machineId);

    @Update("UPDATE vac_machine SET status = #{status} WHERE box_no = #{boxNo} AND deleted = #{deleted}")
    void updateStatusByBoxNoAndDeleted(@Param("boxNo") String boxNo, @Param("deleted") int deleted, @Param("status") int status);

    List<InventoryResponse> inventoryList(@Param("productName") String productName);


    //更新update null 为 null
    int updateNullById(@Param("vacMachine") VacMachine vacMachine);


    int syncUseNumWithTotal();



    List<VacMachine> updateBatchNo();

}
