package com.yiwan.vaccinedispenser.system.sys.service.sys.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiwan.vaccinedispenser.core.exception.BizException;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.redis.CashService;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysUser;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.sys.dao.SysConfigDao;
import com.yiwan.vaccinedispenser.system.sys.dao.SysUserDao;
import com.yiwan.vaccinedispenser.system.sys.data.request.sys.SysConfigRequest;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author 78671
 */
@Slf4j
@Service
public class SysConfigServiceImpl implements SysConfigService {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Override
    public Page<SysConfig> getList(SysConfigRequest request) {
        IPage<SysConfig> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>();
        wrapper.eq(SysConfig::getDeleted, 0);
        IPage<SysConfig> sysConfigIPage = sysConfigDao.selectPage(page, wrapper);
        return (Page<SysConfig>) sysConfigIPage;
    }

    @Override
    public Result sysConfigAdd(SysConfigRequest request, UserBean user) {

        List<SysConfig> sysConfigList = sysConfigDao.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCategory,request.getCategory())
                .eq(SysConfig::getConfigType, request.getConfigType())
                .eq(SysConfig::getConfigName, request.getConfigName())
                .eq(SysConfig::getDeleted, 0));

        if (!sysConfigList.isEmpty()) {
            return Result.fail("该系统类型已经存在");
        }


        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(request, sysConfig);
        sysConfig.setCreateBy(user.getUserName());
        sysConfig.setUpdateBy(user.getUserName());
        int result = sysConfigDao.insert(sysConfig);
        if (result > 0) {
            return Result.success();
        } else {
            return Result.fail("添加系统参数异常！");
        }


    }

    @Override
    public Result sysConfigEdit(SysConfigRequest request, UserBean user) {

        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(request, sysConfig);
        sysConfig.setUpdateBy(user.getUserName());
        log.info(JSON.toJSONString(sysConfig));
        int result = sysConfigDao.updateById(sysConfig);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑系统参数异常！");
        }
    }

    @Override
    public List<SysConfig> getAutoDrugConfigData() {
        return   sysConfigDao.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCategory,"AUTO_DRUG")
                .eq(SysConfig::getDeleted,0));
    }

    @Override
    public List<SysConfig> getSendDrugConfigData() {

        return sysConfigDao.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCategory,"SEND_DRUG")
                .eq(SysConfig::getDeleted,0));
    }

    @Override
    public Integer getSendDrugConfigDataIOTime() {
        List<SysConfig> sysConfigList =  sysConfigDao.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCategory,"SEND_DRUG")
                .eq(SysConfig::getConfigType,"IO_WAIT_TIME")
                .eq(SysConfig::getDeleted,0));
        if(sysConfigList.isEmpty()){
            return 20;
        }else {
            return Integer.parseInt(sysConfigList.get(0).getConfigValue());
        }
    }

    @Override
    public List<SysConfig> getSettingConfigData() {
        return   sysConfigDao.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCategory,"SETTING")
                .eq(SysConfig::getDeleted,0));
    }

}



