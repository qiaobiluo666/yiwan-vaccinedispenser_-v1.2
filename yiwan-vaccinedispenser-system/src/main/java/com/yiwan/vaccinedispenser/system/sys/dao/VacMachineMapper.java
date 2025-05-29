package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
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

    @Update("UPDATE vac_machine SET status = #{status} WHERE box_no = #{boxNo} AND deleted = #{deleted}")
    void updateStatusByBoxNoAndDeleted(@Param("boxNo") String boxNo, @Param("deleted") int deleted, @Param("status") int status);

    List<InventoryResponse> inventoryList(@Param("productName") String productName);


    //更新update null 为 null

    int updateNullById(@Param("vacMachine") VacMachine vacMachine);


    int syncUseNumWithTotal();

}
