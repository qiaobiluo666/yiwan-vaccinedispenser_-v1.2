package com.yiwan.vaccinedispenser.web.controller.system;


import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysMenu;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuInsertRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.MenuStructData;
import com.yiwan.vaccinedispenser.system.sys.service.sys.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/menu")
public class MenuController {


    @Autowired
    private MenuService menuService;

    /**
     * 获取用户菜单
     * @return
     */
    @GetMapping("/user-menus")
    public Result userMenus(@CurrentUser UserBean user){
        List<MenuStructData> menuStructs = new ArrayList<>();
        List<MenuStructData> menuStructList1 = new ArrayList<>(); // 一级菜单
        List<MenuStructData> menuStructList2 = new ArrayList<>(); // 二级菜单

        Long userId = user.getId();
        log.info("userId:{}",userId);
        List<MenuStructData> menuStructList = menuService.selectMenuByUserId(userId);
        menuStructList.forEach(menuStruct -> {
            if(StringUtils.isEmpty(menuStruct.getParentMenuId())){
                menuStructList1.add(menuStruct);
            } else {
                menuStructList2.add(menuStruct);
            }
        });

        // 组装两级菜单
        menuStructList1.forEach(menuStruct1 -> {
            List<MenuStructData> childMenuStructs = new ArrayList<>();
            menuStructList2.forEach(menuStruct2 -> {
                if (menuStruct1.getId().equals(menuStruct2.getParentMenuId())) {
                    childMenuStructs.add(menuStruct2);
                }
            });
            menuStruct1.setMenuStructList(childMenuStructs);
            menuStructs.add(menuStruct1);
        });

        return Result.success(menuStructs);
    }



    /**
     * 获取所有菜单并且该用户拥有权限的菜单
     * @return
     */
    @GetMapping("/role-menusandchecked")
    public Result userMenusAndChecked(Long roleId,@CurrentUser UserBean user){
        List<MenuStructData> menuStructs = menuService.roleMenusAndChecked(user, roleId);
        return Result.success(menuStructs);
    }


    /**
     * 新增
     * @param menuInsertRequest 系统的菜单
     * @return
     */
    @PostMapping("/insert")
    public Result insert(@RequestBody MenuInsertRequest menuInsertRequest, @CurrentUser UserBean userBean){
        log.info("入参-menuInsertRequest:{}",menuInsertRequest);
        menuInsertRequest.setCreateBy(userBean.getId());
        menuService.insert(menuInsertRequest);
        return Result.success();
    }

    /**
     * 更新
     * @param userBean
     * @param sysMenu
     * @return
     */
    @PostMapping("/{id}/update")
    public Result update(@Valid @RequestBody SysMenu sysMenu, @CurrentUser UserBean userBean, @PathVariable long id){
        log.info("入参SysMenu-{}",sysMenu);
        menuService.update(sysMenu,userBean );
        return Result.success();
    }


    /**
     * 删除单个菜单
     * @param id
     * @return
     */
    @PostMapping("/{id}/delete")
    public Result delete( @PathVariable long id){
        menuService.delete(id);
        return Result.success();
    }

    /**
     * 获取单个菜单
     * @param id
     * @return
     */
    @GetMapping("/{id}/menu")
    public Result findMenu( @PathVariable long id){
        MenuRequest menuParam = menuService.findMenu(id);
        return Result.success(menuParam);
    }



    /**
     * 获取所有菜单
     * @return
     */
    @GetMapping("/list")
    public Result list(){
        List<MenuStructData> menuStructs = menuService.findMenuAll();
        return Result.success(menuStructs);
    }



}