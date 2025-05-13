package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysButton;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysMenu;
import com.yiwan.vaccinedispenser.system.sys.dao.SysMenuDao;
import com.yiwan.vaccinedispenser.system.sys.dao.SysRoleMenuDao;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuInsertRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.MenuStructData;
import com.yiwan.vaccinedispenser.system.sys.service.sys.MenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private SysRoleMenuDao sysRoleMenuDao;

    @Autowired
    private SysMenuDao sysMenuDao;


    @Override
    public List<MenuStructData> selectMenuByUserId(Long userId) {
        return sysRoleMenuDao.selectMenuByUserId(userId);
    }

    @Override
    public List<MenuStructData> userMenus(UserBean user) {
        List<MenuStructData> menuStructs = new ArrayList<MenuStructData>();
        // 一级菜单
        List<MenuStructData> menuStructList1 = new ArrayList<>();
        // 二级菜单
        List<MenuStructData> menuStructList2 = new ArrayList<>();

        List<MenuStructData> menuStructList = sysRoleMenuDao.selectMenuByUserId(user.getId());
        menuStructList.forEach(menuStruct -> {
            if(ObjectUtils.isEmpty(menuStruct.getParentMenuId())){
                menuStructList1.add(menuStruct);
            } else {
                menuStructList2.add(menuStruct);
            }
        });


        // 组装两级菜单
        menuStructList1.forEach(menuStruct1 -> {
            List<MenuStructData> childMenuStructs = new ArrayList<MenuStructData>();
            menuStructList2.forEach(menuStruct2 -> {
                if (menuStruct1.getId().equals(menuStruct2.getParentMenuId())) {
                    childMenuStructs.add(menuStruct2);
                }
            });
            menuStruct1.setMenuStructList(childMenuStructs);
            menuStructs.add(menuStruct1);
        });

        return menuStructs;
    }

    @Override
    public List<MenuStructData> findMenuAll() {
        List<MenuStructData> menuStructs = new ArrayList<>();

        // 一级菜单
//        Condition condition1 = new Condition(SysMenu.class);
//        condition1.where().isNull("parentMenuId").equalTo("status",0).end().desc("menuOrder");
//        List<SysMenu> sysMenuList1 = sysMenuDao.find(condition1);

        LambdaQueryWrapper<SysMenu> wrapper1 = new LambdaQueryWrapper<SysMenu>()
                .isNull(SysMenu::getParentMenuId)
                .eq(SysMenu::getStatus, 0)
                .orderByDesc(SysMenu::getMenuOrder);
        List<SysMenu> sysMenuList1 = sysMenuDao.selectList(wrapper1);


        // 二级菜单
//        Condition condition2 = new Condition(SysMenu.class);
//        condition2.where().isNotNull("parentMenuId").equalTo("status",0).end().asc("menuOrder");
//        List<SysMenu> sysMenuList2 = sysMenuDao.find(condition2);

        LambdaQueryWrapper<SysMenu> wrapper2 =  new LambdaQueryWrapper<SysMenu>()
                .isNotNull(SysMenu::getParentMenuId)
                .eq(SysMenu::getStatus, 0)
                .orderByDesc(SysMenu::getMenuOrder);
        List<SysMenu> sysMenuList2 = sysMenuDao.selectList(wrapper2);

        // 组装两级菜单
        sysMenuList1.forEach(sysMenu -> {
            MenuStructData menuStruct = new MenuStructData(sysMenu);
            List<MenuStructData> childMenuStructs = new ArrayList<>();
            sysMenuList2.forEach(sysMenu1 -> {
                if (sysMenu.getId().equals(sysMenu1.getParentMenuId())) {
                    childMenuStructs.add(new MenuStructData(sysMenu1));
                }
            });

            menuStruct.setMenuStructList(childMenuStructs);
            menuStructs.add(menuStruct);
        });

        return menuStructs;
    }

    @Override
    public List<MenuStructData> roleMenusAndChecked(UserBean user, Long roleId) {
        List<MenuStructData> menuStructList1 = new ArrayList<>(); // 用户拥有的一级菜单
        List<MenuStructData> menuStructList2 = new ArrayList<>(); // 用户拥有的二级菜单
        List<MenuStructData> menuStructList = sysRoleMenuDao.selectMenuByRoleId(roleId);
        menuStructList.forEach(menuStruct -> {
            if(StringUtils.isEmpty(menuStruct.getParentMenuId())){
                menuStructList1.add(menuStruct);
            } else {
                menuStructList2.add(menuStruct);
            }
        });

        List<MenuStructData> menuStructs = new ArrayList<>();
        // 一级菜单
//        Condition condition1 = new Condition(SysMenu.class);
//        condition1.where().isNull("parentMenuId").equalTo("status",0).end().desc("menuOrder");
//        List<SysMenu> sysMenuList1 = sysMenuDao.find(condition1);
        LambdaQueryWrapper<SysMenu> wrapper1 = new LambdaQueryWrapper<SysMenu>()
                .isNull(SysMenu::getParentMenuId)
                .eq(SysMenu::getStatus, 0)
                .orderByDesc(SysMenu::getMenuOrder);
        List<SysMenu> sysMenuList1 = sysMenuDao.selectList(wrapper1);

        // 二级菜单
//        Condition condition2 = new Condition(SysMenu.class);
//        condition2.where().isNotNull("parentMenuId").equalTo("status",0).end().asc("menuOrder");
//        List<SysMenu> sysMenuList2 = sysMenuDao.find(condition2);
        LambdaQueryWrapper<SysMenu> wrapper2 = new LambdaQueryWrapper<SysMenu>()
                .isNotNull(SysMenu::getParentMenuId)
                .eq(SysMenu::getStatus, 0)
                .orderByAsc(SysMenu::getMenuOrder);
        List<SysMenu> sysMenuList2 = sysMenuDao.selectList(wrapper2);

        // 组装两级菜单
        sysMenuList1.forEach(sysMenu -> {
            MenuStructData menuStruct = new MenuStructData(sysMenu);

            menuStructList1.forEach(struct1 -> {
                if(struct1.getId().equals(menuStruct.getId())){
                    menuStruct.setCkecked(true);
                }
            });

            List<MenuStructData> childMenuStructs = new ArrayList<MenuStructData>();
            sysMenuList2.forEach(sysMenu2 -> {
                if (sysMenu.getId().equals(sysMenu2.getParentMenuId())) {
                    MenuStructData menuStruct2 = new MenuStructData(sysMenu2);
                    // 判断角色是否拥有此菜单权限
                    menuStructList2.forEach(struct2 -> {
                        if(struct2.getId().equals(menuStruct2.getId())){
                            menuStruct2.setCkecked(true);
                        }
                    });
                    childMenuStructs.add(menuStruct2);
                }

            });

            menuStruct.setMenuStructList(childMenuStructs);
            menuStructs.add(menuStruct);
        });

        return menuStructs;
    }

    @Override
    public void insert(MenuInsertRequest sysMenu) {
        vaildMenu(sysMenu);
        // 添加菜单
        sysMenu.setStatus(0);
        sysMenu.setCreateTime( new Date());

        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(sysMenu,menu);
        sysMenuDao.insert(menu);

    }

    private void vaildMenu(MenuInsertRequest sysMenu) {
        // 验证父菜单
        if (sysMenu.getParentMenuId() != null) {
//            SysMenu menu = sysMenuDao.findOne(sysMenu.getParentMenuId());
            Long parentMenuId = sysMenu.getParentMenuId();
            SysMenu menu =  sysMenuDao.selectById(parentMenuId);
            if (menu == null || menu.getStatus() == 1) {
                throw new ServiceException("上级菜单不存在");
            }
        }
        // 验证按钮
        /*List<SysMenuButton> sysMenuButtonList = menuParam.getSysMenuButtonList();
        if (sysMenuButtonList == null || sysMenuButtonList.size() == 0) {
            throw new ServiceException("按钮未选择");
        }*/
    }

    @Override
    public void update(SysMenu sysMenu, UserBean userBean) {

//        sysMenuDao.update(origin_data);
        sysMenuDao.updateById(sysMenu);
    }

    @Override
    public void delete(Long id) {
        // 查找下级菜单
//        Condition condition = new Condition(SysMenu.class);
//        condition.where().equalTo("parentMenuId", id).end();
//        List<SysMenu> sysMenus = sysMenuDao.find(condition);

        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentMenuId, id);
        List<SysMenu> sysMenus = sysMenuDao.selectList(wrapper);

        if (sysMenus != null && sysMenus.size() > 0) {
            throw new ServiceException("存在下级菜单，不可删除");
        }
        // 删除菜单
//        sysMenuDao.deleteOne(id);
        sysMenuDao.deleteById(id);
    }

    @Override
    public MenuRequest findMenu(Long id) {
        MenuRequest menuParam = new MenuRequest();
        SysMenu sysMenu = sysMenuDao.selectById(id);

        menuParam.setSysMenu(sysMenu);
        return menuParam;
    }

    @Override
    public List<SysButton> findButtonAll() {
        return null;
    }
}