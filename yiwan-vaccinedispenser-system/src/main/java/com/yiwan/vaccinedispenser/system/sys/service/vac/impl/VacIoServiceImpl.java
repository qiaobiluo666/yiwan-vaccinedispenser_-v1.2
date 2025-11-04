package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrug;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacIo;
import com.yiwan.vaccinedispenser.system.sys.dao.VacIoMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacIoRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacIoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class VacIoServiceImpl extends ServiceImpl<VacIoMapper, VacIo> implements VacIoService {
    @Autowired
    private VacIoMapper vacIoMapper;




    @Override
    public Page<VacIo> getVacIoList(VacIoRequest request) {
        IPage<VacIo> page =  new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacIo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VacIo::getDeleted,0);
        IPage<VacIo> vacIoIPage = vacIoMapper.selectPage(page, wrapper);
        return (Page<VacIo>) vacIoIPage;
    }

    @Override
    public Result vacIoEdit(VacIo request, UserBean user) {
        log.info(JSON.toJSONString(request));
        if(request.getLenMin()>request.getLenMax()){
            return Result.fail("最短长度不能大于最长长度！");
        }

        //判断长度区间是否有冲突
        if(vacIoLenInterval(request.getLenMin(),request.getLenMax())){
            return Result.fail("长度区间冲突！");
        }
        request.setUpdateBy(user.getUserName());
        vacIoMapper.updateById(request);
        return Result.success("编辑成功");
    }

    @Override
    public Result vacIoAdd(VacIo request, UserBean user) {
        request.setCreateBy(user.getUserName());
        request.setUpdateBy(user.getUserName());
        //判断长度区间是否有冲突
        if(vacIoLenInterval(request.getLenMin(),request.getLenMax())){
            return Result.fail("长度区间冲突！");
        }
        vacIoMapper.insert(request);
        return Result.success("添加成功");
    }

    @Override
    public Result vacIoDel(IdListRequest request, UserBean user) {
        // 查询要删除的记录
        List<VacIo> vacBoxSpecToDelete = vacIoMapper.selectBatchIds(request.getIdList());
        int flag=0;
        int result;
        // 手动设置更新字段值
        for (VacIo vacIo : vacBoxSpecToDelete) {
            vacIo.setUpdateBy(user.getUserName());
            vacIo.setDeleted(1);
            vacIo.setUpdateTime(LocalDateTime.now());
            result = vacIoMapper.updateById(vacIo);
            if(result<=0){
                flag=1;
            }
        }

        if(flag==0){
            return Result.success();
        }else {
            return Result.fail("删除药盒长度配置异常！");
        }
    }

    @Override
    public boolean vacIoLenInterval(Integer lenMin, Integer lenMax) {
        LambdaQueryWrapper<VacIo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(VacIo::getLenMin, lenMax)
                .gt(VacIo::getLenMax, lenMin)
              .eq(VacIo::getDeleted, 0);
        return vacIoMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public VacIo getVacIoByLen(Integer len) {

        LambdaQueryWrapper<VacIo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(VacIo::getLenMin, len)
                .gt(VacIo::getLenMax, len)
                .eq(VacIo::getDeleted, 0);

        List<VacIo> vacIoList = vacIoMapper.selectList(queryWrapper);

        if(vacIoList.isEmpty()){
            return null;
        }else {
            return  vacIoList.get(0);
        }

    }


}
