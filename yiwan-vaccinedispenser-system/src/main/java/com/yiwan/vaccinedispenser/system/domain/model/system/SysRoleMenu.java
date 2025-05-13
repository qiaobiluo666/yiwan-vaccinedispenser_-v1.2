package com.yiwan.vaccinedispenser.system.domain.model.system;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 
 * 角色拥有菜单关系表
 */
@Data
public class SysRoleMenu implements Serializable {
    private Long id;

    private Long menuId;

    private Long roleId;

    private static final long serialVersionUID = 1L;
}