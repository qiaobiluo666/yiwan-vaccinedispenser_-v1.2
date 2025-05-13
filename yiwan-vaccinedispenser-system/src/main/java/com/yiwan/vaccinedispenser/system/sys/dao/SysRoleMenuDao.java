package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRoleMenu;
import com.yiwan.vaccinedispenser.system.sys.data.response.MenuStructData;


import java.util.List;

public interface SysRoleMenuDao  extends BaseMapper<SysRoleMenu> {

    // You can type your methods here
    long deleteByRoleId(Long roleId);

    List<MenuStructData> selectMenuByUserId(Long userId);

    List<MenuStructData> selectMenuByRoleId(Long roleId);
}
