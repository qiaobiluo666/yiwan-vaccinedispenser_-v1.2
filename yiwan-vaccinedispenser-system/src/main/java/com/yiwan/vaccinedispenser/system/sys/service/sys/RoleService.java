package com.yiwan.vaccinedispenser.system.sys.service.sys;

import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRole;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.RoleMenuParamRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysRoleRequest;

public interface RoleService {
    /**
     * 角色列表
     * @return
     */
    PageData<SysRole> roles(SysRoleRequest role);

    /**
     * 新增一条记录
     * @param role 角色的信息
     * @param user 当前登录的用户信息
     * @return
     */
    long insert(SysRole role, UserBean user);

    /**
     * 更新一条用户的角色信息
     * @param role 用户的角色
     */
    void update(SysRole role);

    /**
     * 删除一条用户的角色信息
     * @param id 用户的角色信息
     */
    void delete(Long id);


    /**
     * 设置角色菜单
     * @param roleMenuParam
     */
    void setRoleMenu(RoleMenuParamRequest roleMenuParam);
}
