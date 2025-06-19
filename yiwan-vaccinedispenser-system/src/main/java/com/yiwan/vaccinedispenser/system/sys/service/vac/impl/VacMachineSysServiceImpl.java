package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineSys;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineSysMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineExceptionRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineSysService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author
 **/
@Service
@Slf4j
public class VacMachineSysServiceImpl extends ServiceImpl<VacMachineSysMapper, VacMachineSys> implements VacMachineSysService {


}








