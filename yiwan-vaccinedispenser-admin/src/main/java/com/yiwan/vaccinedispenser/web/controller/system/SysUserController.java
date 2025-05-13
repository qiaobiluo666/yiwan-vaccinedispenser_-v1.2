package com.yiwan.vaccinedispenser.web.controller.system;


import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.PageRequest;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.UserRoleRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.SysUserDataResponse;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
@Slf4j
@RestController
@RequestMapping("/sys-user")
public class SysUserController {


    @Autowired
    private SysUserService sysUserService;


    /**
     * 获取用户列表
     * @param pageRequest
     * @param username
     * @param status
     * @return
     */
    @GetMapping("/list")
    public Result findAll(PageRequest pageRequest, String username, Integer status){
        PageData<SysUserDataResponse> structPageDatas = sysUserService.findAll(pageRequest, username, status);
        return Result.success(structPageDatas);
    }


    /**
     * 通过用户的id查询所有角色ID
     * @param id 用户的id
     * @return
     */
    @GetMapping("/find-sys-user-role")
    public Result findSysUserRole(Long id){
        return Result.success(sysUserService.findSysUserRole(id));
    }

    /**
     * 设置用户角色
     * @param userRoleRequest
     * @return
     */
    @PostMapping("/set-userrole")
    public Result setUserRole( @Valid @RequestBody UserRoleRequest userRoleRequest){
        log.info("入参-setUserRole:{}",userRoleRequest);
        sysUserService.setUserRole(userRoleRequest);
        return Result.success();
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    @PostMapping("/delete-user")
    public Result deleteUser(Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    /**
     * 更新用户的信息
     * @param sysUser
     * @return
     */
    @PostMapping("/update-user")
    public Result updateUser(SysUser sysUser, @CurrentUser UserBean user){
        String userName = user.getUserName();
        sysUserService.updateUser(sysUser, userName);
        return Result.success();
    }


    /**
     * 新增用户
     * @param user
     * @param sysUser
     * @return
     */
    @PostMapping("/add-user")
    public Result addUser( @RequestBody SysUser sysUser, @CurrentUser UserBean user){
        long id = sysUserService.addUser(user, sysUser);
        return Result.success(id);
    }


}