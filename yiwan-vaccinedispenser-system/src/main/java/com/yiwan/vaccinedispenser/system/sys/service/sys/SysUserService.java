package com.yiwan.vaccinedispenser.system.sys.service.sys;

import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.PageRequest;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUserRole;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.UserRoleRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.SysUserDataResponse;

import java.util.List;


public interface SysUserService {

    /**
     * 分页查询所有的
     * @param pageRequest  分页
     * @param username  用户名
     * @param status 状态
     * @return
     */
    PageData<SysUserDataResponse> findAll(PageRequest pageRequest, String username, Integer status);

    /**
     * 查询用户所有角色的id
     * @param id
     * @return
     */
    List<SysUserRole> findSysUserRole(Long id);

    /**
     * 修改用户的角色
     * @param userRoleRequest
     */
    void setUserRole(UserRoleRequest userRoleRequest);


    /**
     * 删除用户
     * @param id
     */
    void deleteUser(Long id);

    /**
     * 更新用户的信息
     *
     * @param sysUser 用户信息
     * @param name      当前的用户信息
     */
    void updateUser(SysUser sysUser, String name);

    long addUser(UserBean user, SysUser sysUser);
}

