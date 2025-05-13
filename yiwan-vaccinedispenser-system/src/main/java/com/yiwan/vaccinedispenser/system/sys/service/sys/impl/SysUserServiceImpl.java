package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.PasswordUtils;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.PageRequest;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUserRole;
import com.yiwan.vaccinedispenser.system.sys.dao.SysUserDao;
import com.yiwan.vaccinedispenser.system.sys.dao.SysUserRoleDao;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.UserRoleRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.SysUserDataResponse;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private SysUserRoleDao sysUserRoleDao;

    @Override
    public PageData<SysUserDataResponse> findAll(PageRequest pageRequest, String username, Integer status) {
        Integer start = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<SysUser> structList = sysUserDao.getList(username, status, start, pageRequest.getSize());
        Integer cnt = sysUserDao.getCnt(username, status);
        List<SysUserDataResponse> dataList = new ArrayList<>();

        for (SysUser sysUser : structList) {
            Long id = sysUser.getId();
            StringBuilder sb = new StringBuilder();
            // 名称
            List<String> roleNameList = sysUserRoleDao.getRoleName(id);
            roleNameList.forEach(v -> sb.append(v).append(" "));
            SysUserDataResponse sysUserDataResponse = new SysUserDataResponse(sysUser, sb.toString());
            dataList.add(sysUserDataResponse);
        }
        return new PageData<>(dataList, pageRequest.getPage(), pageRequest.getSize(), cnt);
    }

    /**
     * 查询用户的所有的role
     * @param id 用户的id
     * @return
     */
    @Override
    public List<SysUserRole> findSysUserRole(Long id) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getSysUserId,id);

        return sysUserRoleDao.selectList(wrapper);

    }

    /**
     * 修改用户的角色
     * @param userRoleRequest
     */
    @Override
    @Transactional
    public void setUserRole(UserRoleRequest userRoleRequest) {

        // 1.删除所有的用户的角色
        Long userId = userRoleRequest.getUserId();
        List<SysUserRole> sysUserRole = findSysUserRole(userId);
        if(sysUserRole != null && sysUserRole.size() > 0){
            sysUserRole.forEach(role -> {
                Long id = role.getId();
                sysUserRoleDao.deleteById(id);
            });
        }

        // 2.新增用户的角色
        List<Long> roleIds = userRoleRequest.getRoleIds();
        if(roleIds != null && roleIds.size() > 0){
            for(Long id : roleIds){
                SysUserRole userRole = new SysUserRole();
                userRole.setRoleId(id);
                userRole.setSysUserId(userId);
                sysUserRoleDao.insert(userRole);
            }
        }
    }

    // 删除用户
    @Override
    public void deleteUser(Long id) {

        // 使用mp的伪删除
        sysUserDao.deleteById(id);
    }

    /**
     * 更新用户信息
     * @param sysUser 用户信息
     * @param name      当前的用户信息
     */
    @Override
    public void updateUser(SysUser sysUser, String name) {
        Date date = new Date();
        String password = sysUser.getPassword();
        if(!StringUtils.isEmpty(password)){
            sysUser.setPassword(null);
        }
        sysUser.setUpdateTime(date);
        sysUser.setUpdateBy(name);
        sysUserDao.updateById(sysUser);
    }

    @Override
    public long addUser(UserBean user, SysUser sysUser) {
        Date date = new Date();
        String userName = user.getUserName();

        String password = sysUser.getPassword();
        if(StringUtils.isEmpty(password)){
            throw new ServiceException("密码不能为空");
        }
        sysUser.setPassword(PasswordUtils.encode(password));
//        sysUser.setStatus(Constant.SysUserStatus.SYS_USER_STATUS_NORMAL);
        sysUser.setTypes(0); // 0/对外开放   1/不对外开放
        sysUser.setCreateTime(date);

        sysUser.setCreateBy(userName);
        sysUser.setUpdateTime(date);
        sysUser.setUpdateBy(userName);
        sysUser.setDeleted(false);
        sysUserDao.insert(sysUser);

        return sysUser.getId();
    }


}