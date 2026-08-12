package com.yiwan.vaccinedispenser.web.controller.vac;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.VacMachineRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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

    @Autowired(required = false)
    private PlcSendService plcSendService;

    @Resource(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 停止自动上药
     * */
    @GetMapping("/auto-drug-stop")
    public Result autoDrugStop() throws IOException {

        if (plcSendService != null) {
            plcSendService.sendBCabinetFeedStop();
        }else {
            sendDrugThreadManager.stop();
        }
        return Result.success();
    }

    /**
     * 自动上药开始
     * */
    @GetMapping("/auto-drug-start")
    public Result autoDrugStart() throws IOException {
        //开始上药前清理上药信息队列和预留仓位
        redisTemplate.delete(RedisKeyConstant.PLC_SEND_DRUG_MSG);
        redisTemplate.delete(RedisKeyConstant.PLC_RESERVED_MACHINES);
        if (plcSendService != null) {
            plcSendService.sendBCabinetFeedStart();
        }else {
            sendDrugThreadManager.sendDrug();
            sendDrugThreadManager.goTable();
        }
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
        return vacMachineService.autoBackVaccine(request);
    }



}
