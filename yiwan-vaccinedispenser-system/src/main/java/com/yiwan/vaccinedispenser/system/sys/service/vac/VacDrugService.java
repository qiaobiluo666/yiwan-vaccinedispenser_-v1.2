package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;

import java.util.List;

/**
 * @author hhd
 **/
public interface VacDrugService extends IService<VacDrug>{

    /**
     * 药品列表查询
     */


    Page<VacDrug> vacDrugList(DrugListRequest request);


    Result vacDrugAdd(DrugListRequest request, UserBean user);


    Result vacDrugEdit(DrugListRequest request, UserBean user);


    Result vacDrugDel(IdListRequest request, UserBean user);



    VacDrug vacDrugGetByproductNo(String productNo);

    void vacSaveOrUpdateDrug(VacDrug vacDrug);



    DrugRecordRequest sendDrugTest(String code);



    //药盒测距
    Result drugDistance(String code) throws Exception;


    List<VacDrug>  getVaccinePdf();


}
