package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacHandle;
import com.yiwan.vaccinedispenser.system.sys.dao.VacHandleMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacStepRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacHandleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class VacHandleServiceImpl extends ServiceImpl<VacHandleMapper, VacHandle> implements VacHandleService {

    @Override
    public VacHandle getVacHandleByLen(Integer len) {
        if (len == null) {
            return null;
        }
        LambdaQueryWrapper<VacHandle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(VacHandle::getLenMin, len)
                .ge(VacHandle::getLenMax, len)
                .eq(VacHandle::getDeleted, 0);
        List<VacHandle> list = baseMapper.selectList(queryWrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Page<VacHandle> getVacStepList(VacStepRequest request) {
        IPage<VacHandle> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<VacHandle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacHandle::getDeleted, 0);
        IPage<VacHandle> resultPage = baseMapper.selectPage(page, wrapper);
        return (Page<VacHandle>) resultPage;
    }

    @Override
    public Result vacStepEdit(VacHandle request, UserBean user) {
        log.info(JSON.toJSONString(request));
        if (request.getLenMin() > request.getLenMax()) {
            return Result.fail("最短长度不能大于最长长度！");
        }
        // 判断长度区间是否有冲突
        if (vacStepLenInterval(request.getLenMin(), request.getLenMax())) {
            return Result.fail("长度区间冲突！");
        }
        request.setUpdateBy(user.getUserName());
        baseMapper.updateById(request);
        return Result.success("编辑成功");
    }

    @Override
    public Result vacStepAdd(VacHandle request, UserBean user) {
        request.setCreateBy(user.getUserName());
        request.setUpdateBy(user.getUserName());
        // 判断长度区间是否有冲突
        if (vacStepLenInterval(request.getLenMin(), request.getLenMax())) {
            return Result.fail("长度区间冲突！");
        }
        baseMapper.insert(request);
        return Result.success("添加成功");
    }

    @Override
    public Result vacStepDel(IdListRequest request, UserBean user) {
        List<VacHandle> list = baseMapper.selectBatchIds(request.getIdList());
        int flag = 0;
        for (VacHandle item : list) {
            item.setUpdateBy(user.getUserName());
            item.setDeleted(1);
            item.setUpdateTime(LocalDateTime.now());
            if (baseMapper.updateById(item) <= 0) {
                flag = 1;
            }
        }
        return flag == 0 ? Result.success() : Result.fail("删除步进配置异常！");
    }

    @Override
    public boolean vacStepLenInterval(Integer lenMin, Integer lenMax) {
        LambdaQueryWrapper<VacHandle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(VacHandle::getLenMin, lenMax)
                .gt(VacHandle::getLenMax, lenMin)
                .eq(VacHandle::getDeleted, 0);
        return baseMapper.selectCount(queryWrapper) > 0;
    }
}
