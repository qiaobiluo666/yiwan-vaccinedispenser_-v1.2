package com.yiwan.vaccinedispenser.web.controller.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineSys;
import com.yiwan.vaccinedispenser.system.sys.data.request.OtherRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.CabinetSysResponse;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetSettingService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/27 15:52
 */
@RestController
@Slf4j
@RequestMapping("/cabinet-setting")
public class CabinetSettingController {
    @Autowired
    private CabinetSettingService settingService;

    /**
     * 仓柜设置
     * */
    @PostMapping("/setting-cabinet")
    public Result setCabinet(@RequestBody @Validated WorkSettingRequest request){
        log.info("入参-WorkSettingRequest:{}",request);
        settingService.setCabinet(request);
        VacUntil.sleep(200);
        settingService.getCabinet(request);
        return Result.success();
    }

    /**
     * 设置ip指令
     * */
    @PostMapping("/setting-ip")
    public Result setIp(@RequestBody @Validated IPSettingRequest request){
        log.info("入参-IPSettingRequest:{}",request);

        settingService.setIpPort(request);
        VacUntil.sleep(200);
        settingService.getIpPort(request);
        return Result.success();
    }


    /**
     * 设置步进参数指令
     * */
    @PostMapping("/setting-step")
    public Result setStep(@RequestBody @Validated StepSettingData request){
        log.info("入参-StepSettingData:{}",request);
        settingService.setStep(request);
        VacUntil.sleep(200);
        settingService.getStep(request);
        return Result.success();
    }


    /**
     * 设置伺服参数指令
     * */
    @PostMapping("/setting-servo")
    public Result setServo(@RequestBody @Validated ServoSettingData request){
        log.info("入参-ServoSettingData:{}",request);
        settingService.setServo(request);
        VacUntil.sleep(200);
        settingService.getServo(request);
        return Result.success();
    }


    /**
     * 设置超时参数指令
     * */
    @PostMapping("/setting-over-time")
    public Result setTime(@RequestBody @Validated TimeSettingRequest request){
        log.info("入参-TimeSettingRequest:{}",request);
        settingService.setTime(request);
        VacUntil.sleep(200);
        settingService.getTime(request);
        return Result.success();
    }



    /**
     * 设置私有参数
     * */
    @PostMapping("/setting-private")
    public Result setPrivate(@RequestBody @Validated PrivateSettingData request){
        log.info("入参-TimeSettingRequest:{}",request);
        settingService.setPrivate(request);
        VacUntil.sleep(200);
        settingService.getPrivate(request);
        return Result.success();
    }










    /**
     * 获取所有系统参数指令
     * */
    @PostMapping("/setting-all-command")
    public Result getAllSys(@RequestBody OtherRequest request){
        CabinetConstants.Cabinet workMode =request.getWorkMode();
        settingService.getAllSys(workMode);
        return Result.success();
    }


    /**
     * 获取所有系统参数List
     * */
    @PostMapping("/setting-all-list")
    public Result getAllSysList(@RequestBody OtherRequest request){
        log.info("入参-OtherRequest：{}",request);
        CabinetConstants.Cabinet workMode =request.getWorkMode();
        CabinetSysResponse cabinetSysResponse = settingService.getAllSysList(workMode);
        return Result.success(cabinetSysResponse);

    }


}
