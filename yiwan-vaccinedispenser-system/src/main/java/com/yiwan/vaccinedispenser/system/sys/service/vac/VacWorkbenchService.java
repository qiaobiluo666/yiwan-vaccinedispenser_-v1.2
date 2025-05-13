package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacWorkbench;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.WorkbenchRequest;

/**
 * @author slh
 **/
public interface VacWorkbenchService extends IService<VacWorkbench>{

    /**
     * 药品列表查询
     */


    Page<VacWorkbench> vacWorkbenchList(WorkbenchRequest request);


    Result  vacWorkbenchAdd(WorkbenchRequest request, UserBean user);


    Result  vacWorkbenchEdit(WorkbenchRequest request, UserBean user);


    Result  vacWorkbenchDel(IdListRequest request, UserBean user);



    VacWorkbench getByWorkbenchNum(Integer workbenchNum);
}
