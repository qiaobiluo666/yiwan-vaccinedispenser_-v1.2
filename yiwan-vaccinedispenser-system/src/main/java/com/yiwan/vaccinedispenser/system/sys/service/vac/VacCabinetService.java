package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacCabinet;

/**
 * @author slh
 **/
public interface VacCabinetService extends IService<VacCabinet>{

    /**
     * 查询某个机柜的状态
     * @param name  机柜的名称
     * @param status 机柜的状态 状态 1/正常 0/不启用
     * @return
     */
    VacCabinet getOneVacCabinet(String name, int status);


    void updateVacCabinet(Integer type,String ip);



}
