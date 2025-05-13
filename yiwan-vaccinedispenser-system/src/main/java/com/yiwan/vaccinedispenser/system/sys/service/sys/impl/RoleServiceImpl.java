package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRole;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRoleMenu;
import com.yiwan.vaccinedispenser.system.sys.dao.SysRoleDao;
import com.yiwan.vaccinedispenser.system.sys.dao.SysRoleMenuDao;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.RoleMenuParamRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysRoleRequest;
import com.yiwan.vaccinedispenser.system.sys.service.sys.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private SysRoleDao sysRoleDao;

    @Autowired
    private SysRoleMenuDao sysRoleMenuDao;

    @Override
    @Deprecated
    public PageData<SysRole> roles(SysRoleRequest role) {

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(role.getRoleName())){
            wrapper.like(SysRole::getRoleName,role.getRoleName());
        }
//        wrapper.eq(SysRole::getTypes,0)
        wrapper .eq(SysRole::getDeleted,false);

        Page<SysRole> tPage = new Page<>(role.getPage() , role.getSize() , false);
        Page<SysRole> sysRolePage = sysRoleDao.selectPage(tPage, wrapper);

        List<SysRole> records = sysRolePage.getRecords();
        long pages = sysRolePage.getPages();
        long size = sysRolePage.getSize();
        long total = sysRolePage.getTotal();
        return new PageData<SysRole>(records, (int)pages, (int)size, total);

    }

    @Override
    public long insert(SysRole role, UserBean user) {
        role.setCreateTime(new Date());
        role.setCreatedBy(user.getUserName());
        role.setTypes("0");  // 0/对外开放  1/不对外开放
        role.setDeleted(false);
        sysRoleDao.insert(role);
        return role.getId();
    }


    @Override
    public void update(SysRole role) {
        sysRoleDao.updateById(role);
    }

    @Override
    public void delete(Long id) {
        sysRoleDao.deleteById(id);
    }

    @Override
    @Transactional
    public void setRoleMenu(RoleMenuParamRequest roleMenuParam) {
        Long roleId = roleMenuParam.getRoleId();
        SysRole role = sysRoleDao.selectById(roleId);

        // 管理员是不可以进行修改的
        if(role != null && !role.getRoleName().equals("傻逼刘伟")){
            // 先删除角色所有菜单及菜单按钮
            sysRoleMenuDao.deleteByRoleId(roleId);

            // 重新写入新的
            List<Long> menuIds = roleMenuParam.getMenuIds();
            menuIds = menuIds.stream().distinct().collect(Collectors.toList());
            if(menuIds != null && !menuIds.isEmpty()){
                SysRoleMenu roleMenu = null;
//                List<SysRoleMenu> roleMenuList = new ArrayList<>();
                for(Long id : menuIds){
                    roleMenu = new SysRoleMenu();
                    roleMenu.setMenuId(id);
                    roleMenu.setRoleId(roleId);
//                    roleMenuList.add(roleMenu);
                    sysRoleMenuDao.insert(roleMenu);
                }
            }
        }else{
            throw new ServiceException("不可以修改管理员信息！！");
        }
    }
}
