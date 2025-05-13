package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineDrug;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author 78671
 */
public interface VacMachineDrugMapper extends BaseMapper<VacMachineDrug> {


    @Update("UPDATE vac_machine_drug " +
            "SET deleted = 1 " +
            "WHERE id = ( " +
            "   SELECT id FROM ( " +
            "       SELECT id FROM vac_machine_drug " +
            "       WHERE machine_id = #{machineId} " +
            "         AND deleted = 0 " +
            "       ORDER BY create_time ASC " +
            "       LIMIT 1 " +
            "   ) AS latest_record " +
            ")")
    int updateDeletedToOneByMachineId(@Param("machineId") Long machineId);


    @Select("SELECT SUM(num) AS total_num " +
            "FROM vac_machine_drug " +
            "WHERE machine_id = #{machineId} " +
            "  AND deleted = 0")
    Integer sumNumByMachineId(@Param("machineId") Long machineId);


    @Select("SELECT SUM(num) AS total_num " +
            "FROM vac_machine_drug " +
            "WHERE vaccine_id = #{vaccineId} " +
            "  AND deleted = 0")
    Integer sumNumByVaccineId(@Param("vaccineId") Long vaccineId);
}
