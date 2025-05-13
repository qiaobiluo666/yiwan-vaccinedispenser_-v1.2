package com.yiwan.vaccinedispenser.web.controller.system;


import com.yiwan.vaccinedispenser.core.pojo.PageData;
import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysRole;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.RoleMenuParamRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysRoleRequest;
import com.yiwan.vaccinedispenser.system.sys.service.sys.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    /**
     * 角色列表
     * @return
     */
    @PostMapping ("/roles")
    public Result roles(@RequestBody SysRoleRequest role){
        log.info("roles-入参：{}",role);
        PageData<SysRole> data = roleService.roles(role);
        return Result.success(data);
    }

    /**
     * 新增
     * @param role
     * @return
     */
    @PostMapping("/insert")
    public Result insert(@RequestBody SysRole role, @CurrentUser UserBean user){

        log.info("insert-入参：{}",role);
        long id = roleService.insert(role, user);
        return Result.success(id);
    }

    /**
     * 更新用户的角色
     * @param role 用户的角色
     * @return
     */
    @PostMapping("/update")
    public Result update( @RequestBody SysRole role){
        roleService.update(role);
        return Result.success();
    }

    /**
     * 删除一个用户的角色
     * @param id   角色id
     * @return
     */
    @PostMapping("/delete")
    public Result delete(@RequestParam Long id){
        roleService.delete(id);
        return Result.success();
    }

    /**
     * 设置角色菜单
     * @param roleMenuParam
     * @return
     */
    @PostMapping("/set-rolemenu")
    public Result setRoleMenu(@RequestBody RoleMenuParamRequest roleMenuParam, @CurrentUser UserBean userBean){

        roleService.setRoleMenu(roleMenuParam);
        return Result.success();
    }

}