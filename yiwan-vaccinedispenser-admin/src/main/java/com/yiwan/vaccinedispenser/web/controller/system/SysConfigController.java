package com.yiwan.vaccinedispenser.web.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.PageRequest;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRole;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysConfigRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.SysUserDataResponse;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sys-config")
public class SysConfigController {

    @Autowired
    private  SysConfigService sysConfigService;

    /**
     * 获取系统参数列表
     */
    @GetMapping("/list")
    public Result findAll(SysConfigRequest request){
        Page<SysConfig> structPageDatas = sysConfigService.getList(request);
        return Result.success(structPageDatas);
    }


    /**
     * 新增
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody SysConfigRequest request, @CurrentUser UserBean user){
        log.info("SysConfigRequest-入参：{}",request);
        return  sysConfigService.sysConfigAdd(request, user);
    }


    /**
     * 更新系统参数
     * @return
     */
    @PostMapping("/edit")
    public Result edit( @RequestBody SysConfigRequest request, @CurrentUser UserBean user){
        log.info("SysConfigRequest-入参：{}",request);
        return  sysConfigService.sysConfigEdit(request, user);
    }




}