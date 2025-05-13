package com.yiwan.vaccinedispenser.system.sys.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 疫苗上药信息 Mapper 接口
 * </p>
 *
 * @author vicente
 * @since 2023-05-09
 */
public interface VacGetVaccineMapper extends BaseMapper<VacGetVaccine> {


    List<VacGetVaccine> findProductNo(@Param("productNo") List<String> productNo,@Param("workbenchNo") String workbenchNo);










}
