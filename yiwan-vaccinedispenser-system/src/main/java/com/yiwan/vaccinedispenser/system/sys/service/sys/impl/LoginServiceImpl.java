package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.sys.dao.SysUserDao;
import com.yiwan.vaccinedispenser.system.sys.service.sys.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private SysUserDao sysUserDao;

    @Override
    public SysUser findUser(String userName){
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("user_name", userName);
        return sysUserDao.selectOne(query);
    }
}
