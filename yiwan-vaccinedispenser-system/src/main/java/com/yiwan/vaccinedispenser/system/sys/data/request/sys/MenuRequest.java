package com.yiwan.vaccinedispenser.system.sys.data.request.sys;


import com.yiwan.vaccinedispenser.system.domain.model.system.SysMenu;
import lombok.Data;

import java.util.Date;

@Data
public class MenuRequest {

    private SysMenu sysMenu; // 菜单信息

}
