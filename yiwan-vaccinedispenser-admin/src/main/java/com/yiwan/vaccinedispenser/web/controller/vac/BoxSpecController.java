package com.yiwan.vaccinedispenser.web.controller.vac;


import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.BoxSpecListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacBoxSpecService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓柜规格相关接口
 * @author slh
 * @date 2023/5/8
 * @Description
 *
 */
@RestController
@Slf4j
@RequestMapping("/box-spec")
public class BoxSpecController {


    @Autowired
    private VacBoxSpecService vacBoxSpecService;


    /**
     * 仓柜规格列表
     * */
    @PostMapping("/list")
    public Result boxSpecList(@RequestBody @Validated BoxSpecListRequest request){
        log.info("入参-BoxSpecListRequest:{}",request);
        return Result.success(vacBoxSpecService.vacBoxSpecList(request));
    }


    /**
     * 添加仓柜规格信息
     * */
    @PostMapping("/add")
    public Result drugAdd(@RequestBody    @Validated BoxSpecListRequest request, @CurrentUser UserBean user){
        log.info("入参-BoxSpecListRequest:{}",request);
        return vacBoxSpecService.vacBoxSpecAdd(request,user);

    }


    /**
     * 编辑仓柜规格信息
     * */
    @PostMapping("/edit")
    public Result drugEdit(@RequestBody @Validated BoxSpecListRequest request, @CurrentUser UserBean user){
        log.info("入参-BoxSpecListRequest:{}",request);
        return vacBoxSpecService.vacBoxSpecEdit(request,user);


    }


    /**
     * 删除仓柜规格信息
     * */
    @PostMapping("/del")
    public Result drugDel(@RequestBody @Validated IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-BoxSpecListRequest:{}",request);
        return vacBoxSpecService.vacBoxSpecDel(request,user );
    }




}
