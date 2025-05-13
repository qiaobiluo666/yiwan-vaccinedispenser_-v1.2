package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import lombok.Data;

import java.util.Date;

@Data
public class MenuInsertRequest {


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
     * 菜单icon
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
}
