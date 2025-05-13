package com.yiwan.vaccinedispenser.system.sys.data.response;

import com.yiwan.vaccinedispenser.system.domain.model.system.SysMenu;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 菜单结构
 * @author slh
 */
@Data
public class MenuStructData {
    //菜单id
    private Long id;
    //父菜单id
    private Long parentMenuId;
    //菜单名称
    private String menuName;
    //菜单URL
    private String menuUrl;

    //菜单Icon
    private String menuIcon;
    //菜单级别
    private Integer menuLevel;
    //菜单排序
    private Integer menuOrder;
    // 子菜单
    private List<MenuStructData> menuStructList;
    // 菜单是否被选中
    private boolean ckecked = false;

    public MenuStructData(SysMenu sysMenu) {
        this.id = sysMenu.getId();
        this.parentMenuId = sysMenu.getParentMenuId();
        this.menuName = sysMenu.getMenuName();
        this.menuUrl = sysMenu.getMenuUrl();
        this.menuIcon = sysMenu.getMenuIcon();
        this.menuLevel = sysMenu.getMenuLevel();
        this.menuOrder = sysMenu.getMenuOrder();
    }

    public MenuStructData(){}
}
