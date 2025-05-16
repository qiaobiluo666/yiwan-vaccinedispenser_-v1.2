package com.yiwan.vaccinedispenser.web.controller.vac;


import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineExceptionRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 药品信息相关接口
 * @author slh
 * @date 2023/5/8
 * @Description
 *
 */
@RestController
@Slf4j
@RequestMapping("/exception")
public class ExceptionController {

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;


    /**
     * 设备异常记录列表
     * */
    @PostMapping("/machine-exception-list")
    public Result boxSpecList(@RequestBody @Validated MachineExceptionRequest request){
        log.info("入参-DrugListRequest:{}",request);
        return Result.success(vacMachineExceptionService.machineExceptionList(request));
    }


    /**
     * 清除设备异常记录
     * */
    @PostMapping("/machine-exception-del")
    public Result boxSpecAdd(@RequestBody    @Validated IdListRequest request, @CurrentUser UserBean user){
        log.info("入参-DrugListRequest:{}",request);
        return vacMachineExceptionService.machineExceptionDel(request,user);

    }



}
