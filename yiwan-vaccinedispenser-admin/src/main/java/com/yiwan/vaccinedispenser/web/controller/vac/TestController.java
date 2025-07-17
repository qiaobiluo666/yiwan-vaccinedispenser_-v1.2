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
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private DispensingHandFunction dispensingHandFunction;

    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private UploadController uploadController;


    @Value("${upload.hospitalName}")
    private  String hospitalName;



    @Value("${upload.databaseName}")
    private  String databaseName;

    @Value("${upload.password}")
    private  String password;


    /**
     * 疫苗列表
     * */
    @PostMapping("/add-drug")
    public Result machineList() throws Exception {

        ConfigSetting configSetting = configFunction.getSettingConfigData();


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

        vacGetVaccine.setWorkbenchNum(r.nextInt(1,7));
        if("true".equals(configSetting.getIsIoDrop())){
            dispensingFunction.addDrugList(vacGetVaccine);
        }else {
            dispensingHandFunction.addDrugList(vacGetVaccine);
        }


        VacMachine vacMachine5 = vacMachineService.testDrop(4);
        if(vacMachine5==null){
            return Result.fail("没有可发药的药仓");
        }

        BeanUtils.copyProperties(vacMachine5,vacGetVaccine);
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        vacGetVaccine.setRequestNo("requestNo");
        vacGetVaccine.setWorkbenchName("接种台5");
        vacGetVaccine.setWorkbenchNo("69");

        vacGetVaccine.setWorkbenchNum(r.nextInt(1,7));

        if("true".equals(configSetting.getIsIoDrop())){
            dispensingFunction.addDrugList(vacGetVaccine);
        }else {
            dispensingHandFunction.addDrugList(vacGetVaccine);
        }

        VacMachine vacMachine4 = vacMachineService.testDrop(3);
        if(vacMachine4==null){
            return Result.fail("没有可发药的药仓");
        }

        BeanUtils.copyProperties(vacMachine4,vacGetVaccine);
        vacGetVaccine.setTaskId(String.valueOf(UUID.randomUUID()));
        vacGetVaccine.setRequestNo("requestNo");
        vacGetVaccine.setWorkbenchName("接种台4");
        vacGetVaccine.setWorkbenchNo("69");
        vacGetVaccine.setWorkbenchNum(r.nextInt(1,7));
//        vacGetVaccine.setWorkbenchNum(3);
        if("true".equals(configSetting.getIsIoDrop())){
            dispensingFunction.addDrugList(vacGetVaccine);
        }else {
            dispensingHandFunction.addDrugList(vacGetVaccine);
        }

        return Result.success();
        
    }


    /**
     * 测试距离
     * */
    @PostMapping("/distance")
    public Result distance() throws IOException, ExecutionException, InterruptedException {
        ConfigData configData = configFunction.getAutoDrugConfigData();
        DistanceServoData data =sendDrugFunction.distanceServoAll(configData);
        return Result.success(data);
    }


    /**
     * 测试距离
     * */
    @PostMapping("/sensor")
    public Result sensor()  {
        DistanceServoData data =sendDrugFunction.getDistanceSensor();
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
     * 灯板批量测试
     */
    @PostMapping("/cabinetLed")
    public Result cabinetLed( @RequestBody OtherRequest request) throws Exception {

        List<VacMachine> vacMachineList = vacMachineService.cabinetLedTest(request.getLedLine());
        for(VacMachine vacMachine :vacMachineList){
            if(vacMachine.getLedNum()!=null){
                LedRequest ledRequest = new LedRequest();
                ledRequest.setWorkMode(CabinetConstants.Cabinet.CAB_A);
                ledRequest.setCommand(request.getLedLine());
                ledRequest.setMode(CabinetConstants.LedMode.OUTPUT);
                ledRequest.setLedNum(vacMachine.getLedNum());
                ledRequest.setStatus(CabinetConstants.LedStatus.GREEN);
                cabinetAService.ledCommand(ledRequest);
                VacUntil.sleep(request.getLedTime());
            }
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

    @GetMapping("/upload")
    public Result uploadFile() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String logFileName = LocalDateTime.now().format(formatter) + ".log";
        String path = "D:\\yiwan\\backend\\logs\\"+logFileName;
//        String path = "D:\\work\\yiwan\\疫苗发药机\\yiwan-vaccinedispenser_-v1.2\\logs\\"+logFileName;
        uploadController.exportAndUpload(hospitalName,"log", null, null, null, path, true);
        uploadController.exportAndUpload(hospitalName,"db", databaseName, "root", password, null, true);
        return Result.success();
    }



    @PostMapping("/test")
    public Result  test1() {

        vacMachineService.test2();
        return Result.success();
    }



}
