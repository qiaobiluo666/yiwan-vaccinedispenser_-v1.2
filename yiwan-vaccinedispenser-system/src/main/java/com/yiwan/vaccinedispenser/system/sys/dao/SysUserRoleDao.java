package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUserRole;


import java.util.List;

public interface SysUserRoleDao extends BaseMapper<SysUserRole> {

    long deleteRoleByUserId(long userId);

    /**
     * 查询所有的觉得名称
     * @param userId 用户的id
     * @return
     */
    List<String> getRoleName(Long userId);
}
