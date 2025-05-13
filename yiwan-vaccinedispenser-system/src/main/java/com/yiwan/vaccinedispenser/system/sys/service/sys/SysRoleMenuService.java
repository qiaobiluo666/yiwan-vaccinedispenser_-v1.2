package com.yiwan.vaccinedispenser.system.sys.service.sys;

import com.yiwan.vaccinedispenser.system.domain.model.system.SysRoleMenuBtn;

public interface SysRoleMenuService {

    /**
     *
     * @param menuStructId 菜单结构的id
     * @param userRoleId
     * @return
     */
    SysRoleMenuBtn getMenuBtnRole(Long menuStructId, String userRoleId);
}
