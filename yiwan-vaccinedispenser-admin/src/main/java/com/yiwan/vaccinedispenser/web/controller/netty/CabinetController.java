package com.yiwan.vaccinedispenser.web.controller.netty;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetBService;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * AB柜相关接口
 * @author slh
 * @date 2023/5/8
 * @Description
 *
 */
@RestController
@Slf4j
@RequestMapping("/cabinet-debug")
public class CabinetController {
    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private CabinetBService cabinetBService;


    @Autowired
    private CabinetCService cabinetCService;

    @Autowired
    private SendDrugFunction sendDrugFunction;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;


    /**
     * 掉药控制指令
     * */
    @PostMapping("/A-drop")
    public Result dropA(@RequestBody @Validated DropRequest request){
        log.info("入参-DropRequest:{}",request);
        cabinetAService.dropCommand(request);
        return Result.success();
    }


    /**
     * 掉药控制指令
     * */
    @PostMapping("/A-led")
    public Result LedA(@RequestBody @Validated LedRequest request){
        log.info("入参-LedRequest:{}",request);
        log.info(String.valueOf(request.getStatus().num));
        cabinetAService.ledCommand(request);
        return Result.success();
    }


    /**
     * A柜伺服
     * */
    @PostMapping("/A-servo")
    public Result servoA(@RequestBody     @Validated CabinetAServoRequest request){
        log.info("入参-CabinetAServoRequest:{}",request);
        cabinetAService.servo(request);
        if(request.getStatus().num==1){
            VacUntil.sleep(1000);
            String data =valueOperations.get(RedisKeyConstant.servoGetDistance.CABINET_A);
            return Result.success(data);
        }else {
            return Result.success();
        }

    }


    /**
     * A柜步进电机
     * */
    @PostMapping("/A-step")
    public Result stepA(@RequestBody @Validated CabinetAStepRequest request){
        log.info("入参-CabinetAStepRequest:{}",request);
        cabinetAService.step(request);
        return Result.success();
    }


    /**
     * B柜步进电机
     * */
    @PostMapping("/A-move")
    public Result moveA(@RequestParam Integer X,@RequestParam Integer Z){
        log.info("入参-X:{},Z:{}",X,Z);
        sendDrugFunction.moveHandServo(X,Z);
        return Result.success();
    }


    /**
     * B柜上药
     * */
    @PostMapping("/B-send-drug")
    public Result applyB(@RequestBody @Validated CabinetBApplyRequest request){
        log.info("入参-CabinetBApplyRequest:{}",request);
        cabinetBService.apply(request);
        return Result.success();
    }


    /**
     * B柜伺服
     * */
    @PostMapping("/B-servo")
    public Result scanServoB(@RequestBody @Validated CabinetBServoRequest request){
        log.info("入参-CabinetBServoRequest:{}",request);
        cabinetBService.servo(request);
        if(request.getStatus().num==1){
            VacUntil.sleep(1000);
            String data =valueOperations.get(RedisKeyConstant.servoGetDistance.CABINET_B);
            return Result.success(data);
        }else {
            return Result.success();
        }

    }


    /**
     * B柜步进电机
     * */
    @PostMapping("/B-step")
    public Result stepB(@RequestBody @Validated CabinetBStepRequest request){
        log.info("入参-CabinetBStepRequest:{}",request);
        cabinetBService.step(request);
        return Result.success();
    }

    /**
     * C柜出药指令
     * */
    @PostMapping("/C-send-drug")
    public Result sendDrugC(@RequestBody @Validated CabinetCSendDrugRequest request){
        log.info("入参-CabinetCSendDrugRequest:{}",request);
        request.setCommand(CabinetConstants.CabinetCSendDrugCommand.SEND);
        cabinetCService.sendDrug(request);
        return Result.success();
    }


    /**
     * C柜皮带拨片伺服电机
     * */
    @PostMapping("/C-servo")
    public Result servoC(@RequestBody @Validated CabinetCServoRequest request){
        log.info("入参-CabinetCServoRequest:{}",request);
        cabinetCService.servo(request);
        return Result.success();
    }


    /**
     * C柜步进电机
     * */
    @PostMapping("/C-step")
    public Result stepC(@RequestBody @Validated CabinetCStepRequest request){
        log.info("入参-CabinetCServoRequest:{}",request);
        cabinetCService.step(request);
        return Result.success();
    }

    /**
     * AB控制板输出
     * */
    @PostMapping("/outPut")
    public Result outPut(@RequestBody  @Validated OutPutRequest request){
        log.info("入参-OutPutRequest:{}",request);
        cabinetAService.outPut(request);
        return Result.success();
    }

    /**
     * AB控制板输入 /A/B/C/D/E 4类传感器
     * */
    @PostMapping("/intPut")
    public Result intPut(@RequestBody @Validated InPutRequest request){
        log.info("入参-InPutRequest:{}",request);
        cabinetAService.intPut(request);
        return Result.success();
    }

}
