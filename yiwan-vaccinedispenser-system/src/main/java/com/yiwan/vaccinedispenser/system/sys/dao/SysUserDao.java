package com.yiwan.vaccinedispenser.system.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserDao  extends BaseMapper<SysUser> {

    List<SysUser> getList(@Param("username") String username, @Param("status") Integer status,
                          @Param("start") Integer start, @Param("size") Integer size);

    Integer getCnt(@Param("username") String username, @Param("status") Integer status);
}
