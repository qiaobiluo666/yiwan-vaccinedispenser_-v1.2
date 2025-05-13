package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineExceptionRequest;

/**
 * @author slh
 **/
public interface VacMachineExceptionService extends IService<VacMachineException>{

    //设备异常列表
    Page<VacMachineException> machineExceptionList(MachineExceptionRequest request);


    Result machineExceptionDel(IdListRequest request, UserBean user);



    //掉药异常
    void  dropException(Integer code, RedisDrugListData redisDrugListData, String desc);


    void sendException(Integer code,String desc);

    void sendException(Integer code,String productName,String desc);

}
