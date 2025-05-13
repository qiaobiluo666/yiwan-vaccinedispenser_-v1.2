package com.yiwan.vaccinedispenser.web.controller.vac;


import com.yiwan.vaccinedispenser.core.security.CurrentUser;
import com.yiwan.vaccinedispenser.core.security.UserBean;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.IdListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugListRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugRecordService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacDrugService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacSendDrugRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

/**
 * 入药、出药接口
 * @author slh
 * @date 2023/5/8
 * @Description
 *
 */

@RestController
@Slf4j
@RequestMapping("/record")
public class DrugRecordController {

    @Autowired
    private VacDrugRecordService vacDrugRecordService;

    @Autowired
    private VacSendDrugRecordService vacSendDrugRecordService;

    /**
     * 上药列表展示
     * */
    @PostMapping("/drug-record-list")
    public Result drugRecordList(@RequestBody @Validated DrugRecordRequest request){
        log.info("入参-DrugListRequest:{}",request);
        return Result.success(vacDrugRecordService.drugRecordList(request));
    }


    /**
     * 出药列表信息展示
     * */
    @PostMapping("/drug-send-record-list")
    public Result drugSendRecordList(@RequestBody    @Validated SendDrugRecordRequest request){
        log.info("入参-DrugListRequest:{}",request);
        return Result.success(vacSendDrugRecordService.getList(request));

    }



    /**
     * 出药统计 0 1周/1 1个月/2 一年
     */
    @GetMapping("/drug-send-record-count")
    public Result  drugSendRecordCount(@RequestParam Integer type){
        log.info("入参-type:{}",type);

        return vacSendDrugRecordService.sendDrugRecordCount(type);

    }



    /**
     * 上药统计 0 1周/1 1个月/2 一年
     */
    @GetMapping("/drug-record-count")
    public Result  drugRecordCount(@RequestParam Integer type){
        log.info("入参-type:{}",type);

        return vacDrugRecordService.drugRecordCount(type);

    }


    /**
     *
     * 每个工作台疫苗 当日疫苗数
     */
    @GetMapping("/workNum")
    public Result workNum(){


        return vacSendDrugRecordService.sendWorkNum();
    }





    /**
     * 今日上苗发苗统计
     */
    @GetMapping("/today-drug")
    public Result todayDrug(){
        return vacSendDrugRecordService.todayDrug();
    }

    /**
     * 本周与上周每日发苗次数
     */
    @GetMapping("/week-send-drug")
    public Result  weekSendDrug() throws ParseException {

        return vacSendDrugRecordService.weekSendDrug();
    }

}
