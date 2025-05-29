package com.yiwan.vaccinedispenser.web.controller.vac;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;

import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacMachineRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/15 14:18
 */
@RestController
@Slf4j
@RequestMapping("/dispenser")
public class DispensingController {
    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;

    @Autowired
    private VacMachineService vacMachineService;

    /**
     * 停止自动上药
     * */
    @GetMapping("/auto-drug-stop")
    public Result autoDrugStop() throws IOException {
        sendDrugThreadManager.stop();
        return Result.success();
    }

    /**
     * 自动上药开始
     * */
    @GetMapping("/auto-drug-start")
    public Result autoDrugStart() throws IOException {
        sendDrugThreadManager.sendDrug();
        sendDrugThreadManager.goTable();
        return Result.success();
    }


    /**
     * 手动上药  人工上药
     */
    @GetMapping("/hand-drug/hand")
    public Result handDrugHand(String code) throws Exception {
        return   vacMachineService.handDrugHand(code);
    }

    /**
     * 手动上药  机械手上药
     */
    @GetMapping("/hand-drug/machine")
    public Result handDrugMachine(String code) throws Exception {
        return   vacMachineService.handDrugMachine(code);
    }


    /**
     * 手动上药  机械手上药 多人份
     */
    @GetMapping("/hand-drug/people")
    public Result handDrugPeople(String code,Integer bulkNum) throws Exception {
        return  vacMachineService.handDrugPeople(code ,bulkNum);
    }



    /**
     * 疫苗退回
     */
    @PostMapping("/auto-back-drug")
    public Result autoBackDrug(@RequestBody VacMachineRequest request) throws ExecutionException, InterruptedException {
        log.info(JSON.toJSONString(request));
        vacMachineService.autoBackVaccine(request);
        return Result.success();
    }



}
