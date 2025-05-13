package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;

import java.util.List;

/**
 * @author slh
 **/
public interface VacGetVaccineService extends IService<VacGetVaccine>{

        //根据taskId 查询List
        VacGetVaccine getMsgByTaskId(String taskId);

        //根据id 将待发药改为发药中
        void  updateById(Long id);


        VacGetVaccine  insertMsg(VacGetVaccine vacGetVaccine, List<String> productNoList) throws Exception;

}
