package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacWorkbench;
import com.yiwan.vaccinedispenser.system.sys.dao.VacWokrbenchMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.WorkbenchRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacWorkbenchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author
 **/
@Service
@Slf4j
public class VacWorkbenchServiceImpl extends ServiceImpl<VacWokrbenchMapper, VacWorkbench> implements VacWorkbenchService {

    @Autowired
    private  VacWokrbenchMapper vacWokrbenchMapper;

    @Override
    public Page<VacWorkbench>  vacWorkbenchList(WorkbenchRequest request) {
        IPage<VacWorkbench> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacWorkbench> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacWorkbench::getDeleted,0);
        IPage<VacWorkbench> vacWorkbenchIPage = vacWokrbenchMapper.selectPage(page, wrapper);
        // 这里假设你有一个转换方法，将 VacDrug 转换为 DrugListResponse
        return (Page<VacWorkbench>) vacWorkbenchIPage;
    }

    @Override
    public Result vacWorkbenchAdd(WorkbenchRequest request, UserBean user) {
        List<VacWorkbench> vacBoxSpecList = vacWokrbenchMapper.selectList(
                new LambdaQueryWrapper<VacWorkbench>().eq(VacWorkbench ::getWorkbenchNo,request.getWorkbenchNo()).eq(VacWorkbench::getDeleted,0));
        if(!vacBoxSpecList.isEmpty()){
            return Result.fail("该工作台已经存在");
        }
        VacWorkbench vacWorkbench = new VacWorkbench();
        BeanUtils.copyProperties(request, vacWorkbench);
        vacWorkbench.setCreateBy(user.getUserName());
        vacWorkbench.setUpdateBy(user.getUserName());
        int result = vacWokrbenchMapper.insert(vacWorkbench);
        if(result>0){
            return Result.success();
        }else {
            return Result.fail("添加药仓规格异常！");
        }


    }

    @Override
    public Result vacWorkbenchEdit(WorkbenchRequest request, UserBean user) {
        VacWorkbench vacWorkbench = new VacWorkbench();
        BeanUtils.copyProperties(request, vacWorkbench);
        vacWorkbench.setUpdateBy(user.getUserName());
        int result = vacWokrbenchMapper.updateById(vacWorkbench);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑药仓规格异常！");
        }
    }

    @Override
    public Result vacWorkbenchDel(IdListRequest request, UserBean user) {

        // 查询要删除的记录
        List<VacWorkbench> vacWorkbenchList = vacWokrbenchMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacWorkbench vacWorkbench : vacWorkbenchList) {

            vacWorkbench.setUpdateBy(user.getUserName());
            vacWorkbench.setDeleted(1);
            vacWorkbench.setUpdateTime(LocalDateTime.now());

            result = vacWokrbenchMapper.updateById(vacWorkbench);
            if(result<=0){
                flag=1;
            }
        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("删除药仓规格异常！");
        }
    }

    @Override
    public VacWorkbench getByWorkbenchNum(Integer workbenchNum) {

        List<VacWorkbench> vacWorkbenchList = vacWokrbenchMapper.selectList(new LambdaQueryWrapper<VacWorkbench>()
                .eq(VacWorkbench::getDeleted,0)
                .eq(VacWorkbench::getWorkbenchNum,workbenchNum));

        if(vacWorkbenchList.isEmpty()){
            return null;
        }else {
            return vacWorkbenchList.get(0);
        }

    }

}








