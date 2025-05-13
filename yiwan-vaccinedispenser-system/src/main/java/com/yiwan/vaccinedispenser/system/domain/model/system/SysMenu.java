package com.yiwan.vaccinedispenser.system.domain.model.system;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 
 * 后台菜单表
 */
@Data
public class SysMenu implements Serializable {
    private Long id;

    /**
     * 父菜单id
     */
    private Long parentMenuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单URL
     */
    private String menuUrl;


    /**
     * 菜单图标
     */
    private String menuIcon;



    /**
     * 菜单级别
     */
    private Integer menuLevel;

    /**
     * 菜单排序
     */
    private Integer menuOrder;

    /**
     * 0：正常    1：已删除
     */
    private Integer status;

    private Date createTime;

    private Long createBy;



    private static final long serialVersionUID = 1L;
}