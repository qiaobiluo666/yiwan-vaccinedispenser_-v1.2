package com.yiwan.vaccinedispenser.web.controller.vac;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.camera.CameraSendMsg;
import com.yiwan.vaccinedispenser.system.com.ComPortConfig;
import com.yiwan.vaccinedispenser.system.com.ComService;
import com.yiwan.vaccinedispenser.system.dispensing.*;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacGetVaccine;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import com.yiwan.vaccinedispenser.system.sys.data.request.OtherRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.DropRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.LedRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.test.UploadController;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/20 9:09
 */
@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {

    @Autowired
    private DispensingFunction dispensingFunction;

    @Autowired
    private CameraSendMsg cameraSendMsg;

    @Autowired
    private CabinetAService cabinetAService;

    @Autowired
    private SendDrugFunction sendDrugFunction;


    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private VacMachineService vacMachineService;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private UploadController uploadController;


    /**
     * 疫苗列表
     * */
    @PostMapping("/add-drug")
    public Result machineList() throws Exception {


//        uploadController.exportAndUpload("log", null, null, null, "D:\\work\\yiwan\\疫苗发药机\\yiwan-vaccinedispenser_-v1.0\\logs\\20250422.log", false);
//        uploadController.exportAndUpload("db", "vaccine_dispenser_gongchenqiao", "root", "root", null, false);

        Random r = new Random();
        VacGetVaccine vacGetVaccine =new VacGetVaccine();
        VacMachine vacMachine6 = vacMachineService.testDrop(5);
        if(vacMachine6==null){
            return Result.fail("没有可发药的药仓");
        }

        BeanUtils.copyProperties(vacMachine6,vacGetVaccine);
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        vacGetVaccine.setRequestNo("requestNo");
        vacGetVaccine.setWorkbenchName("接种台6");
        vacGetVaccine.setWorkbenchNo("69");
//        vacGetVaccine.setWorkbenchNum(1);

        vacGetVaccine.setWorkbenchNum(r.nextInt(1,3));

        dispensingFunction.addDrugList(vacGetVaccine);

        VacMachine vacMachine5 = vacMachineService.testDrop(4);
        if(vacMachine5==null){
            return Result.fail("没有可发药的药仓");
        }

        BeanUtils.copyProperties(vacMachine5,vacGetVaccine);
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        vacGetVaccine.setRequestNo("requestNo");
        vacGetVaccine.setWorkbenchName("接种台5");
        vacGetVaccine.setWorkbenchNo("69");
//        vacGetVaccine.setWorkbenchNum(2);
        vacGetVaccine.setWorkbenchNum(r.nextInt(1,3));

        dispensingFunction.addDrugList(vacGetVaccine);

        VacMachine vacMachine4 = vacMachineService.testDrop(3);
        if(vacMachine4==null){
            return Result.fail("没有可发药的药仓");
        }

        BeanUtils.copyProperties(vacMachine4,vacGetVaccine);
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        vacGetVaccine.setRequestNo("requestNo");
        vacGetVaccine.setWorkbenchName("接种台4");
        vacGetVaccine.setWorkbenchNo("69");
        vacGetVaccine.setWorkbenchNum(r.nextInt(1,3));
//        vacGetVaccine.setWorkbenchNum(3);
        dispensingFunction.addDrugList(vacGetVaccine);

        return Result.success();
        
    }


    /**
     * 测试距离
     * */
    @PostMapping("/distance")
    public Result distance() throws ExecutionException, InterruptedException, IOException {
        ConfigData configData = configFunction.getAutoDrugConfigData();
        DistanceServoData data =sendDrugFunction.distanceServoAll(configData);
        return Result.success(data);
    }

    /**
     * 测试距离
     * */
    @PostMapping("/distanceXY")
    public Result distanceXY() throws ExecutionException, InterruptedException, IOException {
        ConfigData configData = configFunction.getAutoDrugConfigData();
        DistanceServoData data =sendDrugFunction.DistanceSerVoGetXY(configData);
        return Result.success(data);
    }


    /**
     * 扫码
     */
    @PostMapping("/scan")
    public Result aboveScan( @RequestBody OtherRequest request){

//       int type = request.getType();
//        String result = null;
//        if(type==1){
//            cameraSendMsg.sendCommandToAboveCamera();
//            result = CameraRunStatusVariable.getAboveCameraResult();
//            if(result.split(";").length>1){
//               result="俩个药";
//            }
//        }else if(type==2){
//            cameraSendMsg.sendCommandToBelowCamera();
//            result = CameraRunStatusVariable.getBelowCameraResult();
//        }else {
//            cameraSendMsg.sendCommandToSideCamera();
//            result = CameraRunStatusVariable.getSideCameraResult();
//        }


        int count=0;
        while (count<50){
            cameraSendMsg.sendCommandToSideCamera();
            VacUntil.sleep(100);
            count++;
        }


        log.info(valueOperations.get(RedisKeyConstant.scanCode.SIDE));
        return Result.success();
    }


    /**
     * 灯板批量测试
     */
    @PostMapping("/led")
    public Result led( @RequestBody OtherRequest request) throws Exception {
//        DrugRecordRequest drugRecordData = zcyFunction.getVaccineMsgByCode("81901010039410717437");
//        log.info(JSON.toJSONString(drugRecordData));

        for(int i=1;i<=request.getLedNum();i++){
            LedRequest ledRequest = new LedRequest();
            ledRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
            ledRequest.setCommand(request.getLedLine());
            ledRequest.setMode(CabinetConstants.LedMode.OUTPUT);
            ledRequest.setLedNum(i);
            ledRequest.setStatus(CabinetConstants.LedStatus.GREEN);
            cabinetAService.ledCommand(ledRequest);
            VacUntil.sleep(1000);
            ledRequest.setMode(CabinetConstants.LedMode.NOT_OUTPUT);
            cabinetAService.ledCommand(ledRequest);
            VacUntil.sleep(1000);
        }

        return Result.success();
    }


    /**
     * 仓位批量测试
     */
    @PostMapping("/cabinet")
    public Result cabinet( @RequestBody OtherRequest request) throws InterruptedException {
        vacMachineService.testCabinet(request);
        return Result.success();
    }


    /**
     * io批量测试
     */
    @PostMapping("/io")
    public Result io( @RequestBody OtherRequest request) throws InterruptedException {

    int count=1;
    while (count<=request.getCount()) {
        DropRequest dropRequest = new DropRequest();
        for(int i=request.getIoNumStart();i<=request.getIoNumEnd();i++){

            dropRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
            dropRequest.setMode(CabinetConstants.IOMode.AUTO);
            dropRequest.setCommand(request.getIoLine());
            dropRequest.setIoNum(i);
            dropRequest.setTimes(request.getTime());
            cabinetAService.dropCommand(dropRequest);
            VacUntil.sleep(request.getIoWaitTime());
        }

        count++;

    }

        return Result.success();
    }

    /**
     * 机械手对仓位、 x根据仓位自动增加
     */
    @PostMapping("/handAutoX")
    public Result handAutoX( @RequestBody OtherRequest request) throws InterruptedException {

        vacMachineService.handAutoX(request);
        return Result.success();

    }



}
