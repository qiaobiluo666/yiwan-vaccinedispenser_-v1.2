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



    /**
     * 通用产品编码查询：用于判断药品是否存在和读取普通资料。
     * 此方法故意不校验长宽高，未录入尺寸的药品也必须能被扫码识别。
     */
    VacDrug vacDrugGetByproductNo(String productNo);

    /** 上药、盘点专用：只返回长宽高均已录入且大于0的药品数据。 */
    VacDrug vacDrugGetValidDimensionsByProductNo(String productNo);

    void vacSaveOrUpdateDrug(VacDrug vacDrug);



    DrugRecordRequest sendDrugTest(String code);



    //药盒测距
    Result drugDistance(String code) throws Exception;

    Result drugDistanceStatus();


    Result getZycDrugDetail(String code) throws Exception;


    List<VacDrug>  getVaccinePdf();


}
