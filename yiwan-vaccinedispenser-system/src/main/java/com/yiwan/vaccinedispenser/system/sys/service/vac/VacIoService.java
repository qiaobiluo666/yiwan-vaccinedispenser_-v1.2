package com.yiwan.vaccinedispenser.system.sys.service.vac;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacIo;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineDrug;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacIoRequest;

import java.util.List;

public interface VacIoService extends IService<VacIo> {

    Page<VacIo> getVacIoList(VacIoRequest request);


    Result vacIoEdit(VacIo request, UserBean user);



    Result vacIoAdd(VacIo request, UserBean user);



    Result   vacIoDel(IdListRequest request, UserBean user);


    boolean vacIoLenInterval(Integer lenMin , Integer lenMax);


    VacIo getVacIoByLen(Integer len);

}
