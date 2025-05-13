package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUserRole;
import com.yiwan.vaccinedispenser.system.sys.dao.SysUserRoleDao;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserRoleServiceImpl implements SysUserRoleService {

    @Autowired
    private SysUserRoleDao sysUserRoleDao;


    @Override
    public SysUserRole getSysUserRoleByUserId(Long userId) {

        return  sysUserRoleDao.selectOne(new QueryWrapper<SysUserRole>().eq("sys_user_id", userId));
    }
}
