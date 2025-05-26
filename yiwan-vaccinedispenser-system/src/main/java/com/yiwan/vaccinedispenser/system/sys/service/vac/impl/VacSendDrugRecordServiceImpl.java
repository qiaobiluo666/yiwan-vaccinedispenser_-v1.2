package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacWorkbench;
import com.yiwan.vaccinedispenser.system.sys.dao.VacDrugRecordMapper;
import com.yiwan.vaccinedispenser.system.sys.dao.VacSendDrugRecordMapper;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacSendDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacWorkbenchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @author
 **/
@Service
@Slf4j
public class VacSendDrugRecordServiceImpl extends ServiceImpl<VacSendDrugRecordMapper, VacSendDrugRecord> implements VacSendDrugRecordService {

    @Autowired
    private VacSendDrugRecordMapper vacSendDrugRecordMapper;


    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    @Autowired
    private VacWorkbenchService vacWorkbenchService;

    @Autowired
    private VacDrugRecordMapper vacDrugRecordMapper;

    @Override
    public Page<VacSendDrugRecord> getList(SendDrugRecordRequest request) {


        IPage<VacSendDrugRecord> page= new Page<>(request.getPage(),request.getSize());
        LambdaQueryWrapper<VacSendDrugRecord> wrapper = new LambdaQueryWrapper<>();

        //产品名称
        if(StringUtils.isNotBlank(request.getProductName())){
            wrapper.like(VacSendDrugRecord::getProductName,request.getProductName());
        }


        //接种台
        if(StringUtils.isNotBlank(request.getWorkbenchName())){
            wrapper.eq(VacSendDrugRecord::getWorkbenchName,request.getWorkbenchName());
        }

//        //机器编号
//        if(request.getMachineNo()!=null){
//            wrapper.like(VacSendDrugRecord::getMachineNo,request.getMachineNo());
//        }

        //电子监管码
        if(StringUtils.isNotBlank(request.getSupervisedCode())){
            wrapper.like(VacSendDrugRecord::getSupervisedCode,request.getSupervisedCode());
        }

//        //批次
//        if(StringUtils.isNotBlank(request.getBatchNo())){
//            wrapper.eq(VacSendDrugRecord::getBatchNo,request.getBatchNo());
//        }


        //创建时间
        if(request.getCreateTimeStart()!=null){
            wrapper.gt(VacSendDrugRecord::getCreateTime,request.getCreateTimeStart());
        }
        //创建时间
        if(request.getCreateTimeEnd()!=null){
            wrapper.lt(VacSendDrugRecord::getCreateTime,request.getCreateTimeEnd());
        }

        //工作台名称
        if(StringUtils.isNotBlank(request.getBatchNo())){
            wrapper.eq(VacSendDrugRecord::getWorkbenchName,request.getWorkbenchName());
        }

        wrapper.eq(VacSendDrugRecord::getDeleted,0);
        wrapper.orderByDesc(VacSendDrugRecord::getCreateTime);
        IPage<VacSendDrugRecord> vacSendDrugRecordIPage = vacSendDrugRecordMapper.selectPage(page, wrapper);

        // 这里假设你有一个转换方法，将 VacDrug 转换为 DrugListResponse

        return (Page<VacSendDrugRecord>) vacSendDrugRecordIPage;

    }


    @Override
    public Result sendDrugRecordCount(Integer type) {

        List<Map<String, Object>> typeList;
        if(type==0){
            typeList = vacSendDrugRecordMapper.getWeeklyCountForType0();
        }else if(type==1){
            typeList = vacSendDrugRecordMapper.getDailyCountForType1();
        }else {
            typeList = vacSendDrugRecordMapper.getMonthlyCountForType2();
        }

        return Result.success(typeList);

    }

    @Override
    public void sendDrugRecordAdd(RedisDrugListData drugListData, Integer status, String desc) {

        //获取最早的入药记录
        VacDrugRecord vacDrugRecord = vacDrugRecordService.getListByMachineIdAndProductNo(drugListData.getMachineId(), drugListData.getProductNo());

        VacWorkbench vacWorkbench =vacWorkbenchService.getByWorkbenchNum(1);

        if(vacDrugRecord!=null&&vacWorkbench!=null){
            log.info("记录疫苗退回数据");
            //上药记录 药品的状态变成不在药仓里
            vacDrugRecordService.updateStatusById(vacDrugRecord.getId(),"1");

            VacSendDrugRecord vacSendDrugRecord = new VacSendDrugRecord();
            BeanUtils.copyProperties(vacDrugRecord,vacSendDrugRecord);
            vacSendDrugRecord.setWorkbenchName(vacWorkbench.getWorkbenchName());
            vacSendDrugRecord.setWorkbenchNo(vacWorkbench.getWorkbenchNo());

            //发药记录
            vacSendDrugRecord.setStatus(status);
            vacSendDrugRecord.setDescription(desc);
            vacSendDrugRecord.setCreateTime(LocalDateTime.now());
            vacSendDrugRecord.setUpdateTime(LocalDateTime.now());
            vacSendDrugRecordMapper.insert(vacSendDrugRecord);

        }


    }

    @Override
    public Result sendWorkNum() {
        return Result.success(vacSendDrugRecordMapper.getSendWorkNum());
    }

    @Override
    public Result todayDrug() {
        List<Map<String, Object>> todayDrug = vacDrugRecordMapper.todayDrug();

        List<Map<String, Object>> todaySendDrug = vacSendDrugRecordMapper.todaySendDrug();

        Map<String, Object> data = new HashMap<>();
        data.put("todayDrug",todayDrug.get(0).get("total_records"));
        data.put("todaySendDrug",todaySendDrug.get(0).get("total_records"));



        return Result.success(data);
    }

    @Override
    public Result weekSendDrug() throws ParseException {
        List<Map<String, Object>> records = vacSendDrugRecordMapper.weekSendDrug();

        // 初始化数据
        Map<String, Object> data = new HashMap<>();
        data.put("LastMonday", 0);
        data.put("LastTuesday", 0);
        data.put("LastWednesday", 0);
        data.put("LastThursday", 0);
        data.put("LastFriday", 0);
        data.put("LastSaturday", 0);
        data.put("LastSunday", 0);

        data.put("Monday", 0);
        data.put("Tuesday", 0);
        data.put("Wednesday", 0);
        data.put("Thursday", 0);
        data.put("Friday", 0);
        data.put("Saturday", 0);
        data.put("Sunday", 0);

        // 获取当前日期和星期几
        LocalDate today = LocalDate.now();
        LocalDate thisSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        DayOfWeek dayOfWeek ;
        // 1=Monday, 7=Sunday
        // 遍历 records 数据
        if (records != null) {
            for (Map<String, Object> record : records) {
                // 获取日期对象
                java.sql.Date sqlDate = (java.sql.Date) record.get("date");

                // 转换为 LocalDate
                LocalDate recordDate = sqlDate.toLocalDate();

                // 计算日期差
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(recordDate, thisSunday));

                // 判断是上周还是本周
                dayOfWeek = recordDate.getDayOfWeek();
                String key;
                if (daysDiff <7) {
                    // 本周
                    key = getWeekdayKey(dayOfWeek.getValue());
                } else {
                    // 上周
                    key = "Last" + getWeekdayKey(dayOfWeek.getValue());
                }


                data.put(key, record.getOrDefault("count",0));
            }
        }



        return Result.success(data);
    }

    @Override
    public List<VacSendDrugRecord> sendDrugRecordListByCreateTime(Date createTimeStart, Date createTimeEndate, String workbenchName) {
        LambdaQueryWrapper<VacSendDrugRecord> wrapper = new LambdaQueryWrapper<>();
        //创建时间
        wrapper.gt(VacSendDrugRecord::getCreateTime,createTimeStart);
        wrapper.lt(VacSendDrugRecord::getCreateTime,createTimeEndate);
        if(workbenchName!=null && !workbenchName.isEmpty()){
            wrapper.gt(VacSendDrugRecord::getWorkbenchName,workbenchName);
        }
        wrapper.orderByDesc(VacSendDrugRecord::getCreateTime);
        wrapper.eq(VacSendDrugRecord::getDeleted,0);
        return vacSendDrugRecordMapper.selectList(wrapper);
    }

    @Override
    public List<SendDrugRecordRequest> sendDrugRecordTotalListByCreateTime(Date createTimeStart, Date createTimeEndate,String workbenchName) {
        return vacSendDrugRecordMapper.countGroupedByProductName(createTimeStart,createTimeEndate,workbenchName);
    }


    // 计算两个 LocalDate 之间的天数差距
    private static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return Math.abs(startDate.toEpochDay() - endDate.toEpochDay());
    }



    // 根据 DayOfWeek 枚举值返回对应的键名
    private static String getWeekdayKey(int dayOfWeekValue) {
        switch (dayOfWeekValue) {
            case 1:
                return "Monday";
            case 2:
                return "Tuesday";
            case 3:
                return "Wednesday";
            case 4:
                return "Thursday";
            case 5:
                return "Friday";
            case 6:
                return "Saturday";
            case 7:
                return "Sunday";
            default:
                return "";
        }
    }
}









