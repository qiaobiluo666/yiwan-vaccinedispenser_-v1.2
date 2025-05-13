package com.yiwan.vaccinedispenser.system.sys.service.sys;


import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;

public interface LoginService {


    /**
     * 通过用户名进行查询
     * @param userName
     * @return
     */
    SysUser findUser(String userName);
}
