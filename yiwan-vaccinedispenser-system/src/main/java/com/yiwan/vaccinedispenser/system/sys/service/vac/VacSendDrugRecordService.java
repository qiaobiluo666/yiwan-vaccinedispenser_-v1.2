package com.yiwan.vaccinedispenser.system.sys.service.vac;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * @author hhd
 **/
public interface VacSendDrugRecordService extends IService<VacSendDrugRecord>{

    Page<VacSendDrugRecord> getList(SendDrugRecordRequest request);


    //上药统计 0 1周/1 1个月/2 一年
    Result sendDrugRecordCount(Integer type);



    /**
     *
     */
    void  sendDrugRecordAdd(RedisDrugListData drugListData,Integer status,String desc);





    Result  sendWorkNum();



    Result  todayDrug();


    Result weekSendDrug() throws ParseException;


    //发药详情
    List<VacSendDrugRecord> sendDrugRecordListByCreateTime(Date createTimeStart, Date createTimeEndate,String workbenchName );


    //发药 药品发了几只
    List<SendDrugRecordRequest> sendDrugRecordTotalListByCreateTime(Date createTimeStart, Date createTimeEndate,String workbenchName );





}
