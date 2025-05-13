package com.yiwan.vaccinedispenser.system.sys.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysConfigRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.BoxSpecListRequest;

import java.util.List;

/**
 * @author 78671
 */
public interface SysConfigService {

    //获取系统参数列表
    Page<SysConfig>  getList(SysConfigRequest request);


    Result sysConfigAdd(SysConfigRequest request, UserBean user);


    Result sysConfigEdit(SysConfigRequest request, UserBean user);


    //获取自动上药系统参数
    List<SysConfig> getAutoDrugConfigData();

    //获取抬升装置要走的距离
    List<SysConfig> getSendDrugConfigData();

    //获取IO开合的时间
    Integer getSendDrugConfigDataIOTime();


    //获取系统参数配置
    List<SysConfig> getSettingConfigData();


}