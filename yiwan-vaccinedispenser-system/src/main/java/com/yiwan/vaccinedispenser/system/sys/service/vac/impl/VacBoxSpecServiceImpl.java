package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacBoxSpec;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.sys.dao.VacBoxSpecMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.BoxSpecListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacBoxSpecService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class VacBoxSpecServiceImpl extends ServiceImpl<VacBoxSpecMapper, VacBoxSpec> implements VacBoxSpecService {

    @Autowired
    private  VacBoxSpecMapper vacBoxSpecMapper;

    @Override
    public Page<VacBoxSpec>  vacBoxSpecList(BoxSpecListRequest request) {
        IPage<VacBoxSpec> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacBoxSpec> wrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(request.getName())){
            wrapper.like(VacBoxSpec::getName,request.getName());
        }
        wrapper.eq(VacBoxSpec::getDeleted,0);
        IPage<VacBoxSpec> vacBoxSpecIPage = vacBoxSpecMapper.selectPage(page, wrapper);

        // 这里假设你有一个转换方法，将 VacDrug 转换为 DrugListResponse

        return (Page<VacBoxSpec>) vacBoxSpecIPage;
    }

    @Override
    public Result vacBoxSpecAdd(BoxSpecListRequest request, UserBean user) {
        List<VacBoxSpec> vacBoxSpecList = vacBoxSpecMapper.selectList(new LambdaQueryWrapper<VacBoxSpec>().eq(VacBoxSpec ::getName,request.getName()).eq(VacBoxSpec::getDeleted,0));

        if(!vacBoxSpecList.isEmpty()){
            return Result.fail("该规格已经存在");
        }

        VacBoxSpec vacBoxSpec = new VacBoxSpec();
        BeanUtils.copyProperties(request, vacBoxSpec);
        vacBoxSpec.setCreateBy(user.getUserName());
        vacBoxSpec.setUpdateBy(user.getUserName());
        int result = vacBoxSpecMapper.insert(vacBoxSpec);
        if(result>0){
            return Result.success();
        }else {
            return Result.fail("添加药仓规格异常！");
        }


    }

    @Override
    public Result vacBoxSpecEdit(BoxSpecListRequest request, UserBean user) {
        VacBoxSpec vacBoxSpec = new VacBoxSpec();
        BeanUtils.copyProperties(request, vacBoxSpec);
        vacBoxSpec.setUpdateBy(user.getUserName());
        int result = vacBoxSpecMapper.updateById(vacBoxSpec);

        if(result>0){
            return Result.success();
        }else {
            return Result.fail("编辑药仓规格异常！");
        }
    }

    @Override
    public Result vacBoxSpecDel(IdListRequest request, UserBean user) {

        // 查询要删除的记录
        List<VacBoxSpec> vacBoxSpecToDelete = vacBoxSpecMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacBoxSpec vacBoxSpec : vacBoxSpecToDelete) {
            vacBoxSpec.setUpdateBy(user.getUserName());
            vacBoxSpec.setDeleted(1);
            vacBoxSpec.setUpdateTime(LocalDateTime.now());
            result = vacBoxSpecMapper.updateById(vacBoxSpec);
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
    public List<VacBoxSpec> findVacBoxSpec(Integer length) {
        LambdaQueryWrapper<VacBoxSpec> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .ge(VacBoxSpec::getLength,length)
                .apply("length - ranges <= {0}", length)
                .eq(VacBoxSpec::getDeleted,0);

        return vacBoxSpecMapper.selectList(queryWrapper);
    }

}








