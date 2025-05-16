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
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineExceptionMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineExceptionRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
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
public class VacMachineSysServiceImpl extends ServiceImpl<VacMachineExceptionMapper, VacMachineException> implements VacMachineExceptionService {

    @Autowired
    private VacMachineExceptionMapper vacMachineExceptionMapper;


    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource
    private VacMachineService vacMachineService;


    @Override
    public Page<VacMachineException> machineExceptionList(MachineExceptionRequest request) {

        IPage<VacMachineException> page= new Page<>(request.getPage(),request.getSize());

        LambdaQueryWrapper<VacMachineException> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacMachineException::getDeleted,0)
                .orderByDesc(VacMachineException::getCreateTime);
        IPage<VacMachineException> vacMachineExceptionIPage = vacMachineExceptionMapper.selectPage(page, wrapper);

        return (Page<VacMachineException>) vacMachineExceptionIPage;

    }

    @Override
    public Result machineExceptionDel(IdListRequest request, UserBean user) {
        // 查询要删除的记录
        List<VacMachineException> vacMachineExceptionList = vacMachineExceptionMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacMachineException data : vacMachineExceptionList) {
            data.setUpdateBy(user.getUserName());
            data.setDeleted(1);
            data.setUpdateTime(LocalDateTime.now());
            result = vacMachineExceptionMapper.updateById(data);
            //清楚异常处理
            if(result<=0){
                flag=1;
            }
            if(data.getLineNum()!=null){
                if(Objects.equals(data.getCode(), SettingConstants.MachineException.BELT.code)){
                    int beltNum = (int) Math.ceil((double) data.getLineNum() /2);
                    //将redis状态修改回来
                    //TODO 仓位禁用修改回来
                    valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG,beltNum),"false");
                    valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,beltNum),"false");
                }else {
                    vacMachineService.vacMachineIOByBoxNo(data.getBoxNo());

                }

            }



        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("清楚设备异常失败！");
        }



    }

    @Override
    public void dropException(Integer code, RedisDrugListData redisDrugListData, String desc) {
        VacMachineException vacMachineException = new VacMachineException();
        if(redisDrugListData!=null){
            BeanUtils.copyProperties(redisDrugListData,vacMachineException);
        }
        vacMachineException.setCode(code);
        vacMachineException.setDescription(desc);
        vacMachineExceptionMapper.insert(vacMachineException);
    }

    @Override
    public void sendException(Integer code, String desc) {
        VacMachineException vacMachineException = new VacMachineException();
        vacMachineException.setCode(code);
        vacMachineException.setDescription(desc);
        vacMachineExceptionMapper.insert(vacMachineException);
    }

    @Override
    public void sendException(Integer code, String productName, String desc) {

        VacMachineException vacMachineException = new VacMachineException();
        vacMachineException.setDrugName(productName);
        vacMachineException.setCode(code);
        vacMachineException.setDescription(desc);
        vacMachineExceptionMapper.insert(vacMachineException);


    }
}








