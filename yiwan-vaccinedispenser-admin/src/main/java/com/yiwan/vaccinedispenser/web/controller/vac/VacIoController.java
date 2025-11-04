package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacIo;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacIoRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacIoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author 78671
 */
@RestController
@Slf4j
@RequestMapping("/ioTime")
public class VacIoController {

    @Autowired
    private VacIoService vacIoService;

    /**
     * 疫苗列表
     * */
    @PostMapping("/list")
    public Result ioTimeList(@RequestBody @Validated VacIoRequest request){
        return Result.success(vacIoService.getVacIoList(request));
    }

    /**
     * 疫苗列表
     * */
    @PostMapping("/edit")
    public Result vacIoEdit(@RequestBody VacIo request,@CurrentUser UserBean user){
        return vacIoService.vacIoEdit(request,user);
    }

    /**
     * 疫苗列表
     * */
    @PostMapping("/add")
    public Result vacIoAdd(@RequestBody VacIo request,@CurrentUser UserBean user){
        return vacIoService.vacIoAdd(request,user);
    }


    /**
     * 删除仓柜规格信息
     * */
    @PostMapping("/del")
    public Result drugDel(@RequestBody  IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-BoxSpecListRequest:{}",request);
        return vacIoService.vacIoDel(request,user);
    }


}
