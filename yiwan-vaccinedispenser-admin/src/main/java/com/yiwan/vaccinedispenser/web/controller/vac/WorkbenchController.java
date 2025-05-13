package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.dao.VacWokrbenchMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.WorkbenchRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.WorkbenchRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacWorkbenchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author slh
 * @version 1.0
 * @desc 工作台配置
 * @date 2024/5/13 9:36
 */
@RestController
@Slf4j
@RequestMapping("/workbench")
public class WorkbenchController {


    @Autowired
    private VacWorkbenchService vacWorkbenchService;


    /**
     * 工作台配置列表
     * */
    @PostMapping("/list")
    public Result workbenchList(@RequestBody @Validated WorkbenchRequest request){
        log.info("入参-WorkbenchRequest:{}",request);
        return Result.success(vacWorkbenchService.vacWorkbenchList(request));
    }


    /**
     * 添加工作台配置信息
     * */
    @PostMapping("/add")
    public Result workbenchAdd(@RequestBody    @Validated WorkbenchRequest request, @CurrentUser UserBean user){
        log.info("入参-WorkbenchRequest:{}",request);
        return vacWorkbenchService.vacWorkbenchAdd(request,user);

    }


    /**
     * 编辑工作台配置信息
     * */
    @PostMapping("/edit")
    public Result workbenchEdit(@RequestBody @Validated WorkbenchRequest request, @CurrentUser UserBean user){
        log.info("入参-WorkbenchRequest:{}",request);
        return vacWorkbenchService.vacWorkbenchEdit(request,user);


    }


    /**
     * 删除工作台配置信息
     * */
    @PostMapping("/del")
    public Result workbenchDel(@RequestBody @Validated IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-WorkbenchRequest:{}",request);
        return vacWorkbenchService.vacWorkbenchDel(request,user );
    }








}
