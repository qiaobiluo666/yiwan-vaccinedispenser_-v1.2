package com.yiwan.vaccinedispenser.system.sys.service.sys;

import com.yiwan.vaccinedispenser.system.domain.model.system.SysUserRole;

public interface SysUserRoleService {

    SysUserRole getSysUserRoleByUserId(Long userId);
}
