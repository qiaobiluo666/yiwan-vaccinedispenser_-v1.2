package com.yiwan.vaccinedispenser.system.domain.model.system;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 
 * 用户角色表
 */
@Data
public class SysUserRole implements Serializable {
    private Long id;

    private Long sysUserId;

    private Long roleId;

    private static final long serialVersionUID = 1L;
}