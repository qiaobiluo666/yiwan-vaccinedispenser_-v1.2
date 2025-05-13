package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.BoxSpecListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;

import java.util.List;

/**
 * @author slh
 **/
public interface VacBoxSpecService extends IService<VacBoxSpec>{

    /**
     * 药品列表查询
     */


    Page<VacBoxSpec> vacBoxSpecList(BoxSpecListRequest request);


    Result  vacBoxSpecAdd(BoxSpecListRequest request, UserBean user);


    Result  vacBoxSpecEdit(BoxSpecListRequest request, UserBean user);


    Result  vacBoxSpecDel(IdListRequest request, UserBean user);


    List<VacBoxSpec> findVacBoxSpec(Integer length);





}
