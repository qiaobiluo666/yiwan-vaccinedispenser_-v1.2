package com.yiwan.vaccinedispenser.system.sys.service.sys;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysButton;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysMenu;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuInsertRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.MenuRequest;
import com.yiwan.vaccinedispenser.system.sys.data.response.MenuStructData;

import java.util.List;

public interface MenuService {

    List<MenuStructData> selectMenuByUserId(Long userId);

    List<MenuStructData> userMenus(UserBean user);

    /**
     * 获取所有菜单
     * @return
     */
    List<MenuStructData> findMenuAll();

    List<MenuStructData> roleMenusAndChecked(UserBean user, Long roleId);

    /**
     * 新增
     * @param menuParam
     * @return
     */
    void insert(MenuInsertRequest menuParam);

    /**
     * 修改
     *
     * @param menuParam
     * @param userBean
     * @return
     */
    void update(SysMenu menuParam,UserBean userBean);

    /**
     * 删除
     * @param id
     * @return
     */
    void delete(Long id);

    /**
     * 查询菜单详情
     * @param id
     * @return
     */
    MenuRequest findMenu(Long id);

    /**
     * 获取所有按钮
     * @return
     */
    List<SysButton> findButtonAll();

}