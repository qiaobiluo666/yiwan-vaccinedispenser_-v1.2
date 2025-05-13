package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/15 9:32
 */
@Slf4j
@Component
public class SendDrugThreadManager {


    @Autowired
    private SendDrugFunction  sendDrugFunction;



    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private WebsocketService websocketService;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    private final TaskExecutor taskExecutor;
    @Autowired
    public SendDrugThreadManager(@Qualifier("DispensingThreadPool") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    private volatile boolean running = true;


    public void goTable(){
        running=true;
        valueOperations.set(RedisKeyConstant.CABINET_B_COUNT,"1");
        taskExecutor.execute(() -> {
            ConfigData configData = configFunction.getAutoDrugConfigData();

            while (running) {
                try {
                    sendDrugFunction.goTable(configData);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    log.error("测距线程报错：",e);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                VacUntil.sleep(500);
            }
            sendDrugFunction.autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);
        });

    }

    //开始自动上药
    public void sendDrug() throws IOException {
        if("true".equals(valueOperations.get(RedisKeyConstant.autoDrug.AUTO_DRUG_START))){
            throw new ServiceException("正在自动上药");
        }

        if(!status()){
            throw new ServiceException("设备有异常，请清除异常!");
        }

        log.info("开始自动上药 ！！！");
        running=true;
        ConfigData configData = configFunction.getAutoDrugConfigData();
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        init(configData);

        //开始发药状态
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"true");
        taskExecutor.execute(() -> {
            while (running) {
                try {
                    sendDrugFunction.sendDrug(configData,configSetting);
//                    testSend.sendDrug(configData);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("上药线程报错：",e);
                    //关闭气泵
                    try {
                        sendDrugFunction.servoTableReturn(configData,"线程报错退回");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                }
                VacUntil.sleep(500);
            }

            sendDrugFunction.autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);

        });

    }


    public void  init(ConfigData configData) throws IOException {
        log.info("========================自动上药初始化=========================");
        //线程机械手运动结束
        valueOperations.set(RedisKeyConstant.CABINET_A_HANDLE_IS_MOVE_END,"true");
        valueOperations.set(RedisKeyConstant.HANDLE_IS_DROP,"true");
        //A柜步进电机回原
        sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.CLAMP);
        sendDrugFunction.cabinetAStepInit(CabinetConstants.CabinetAStepMode.BLOCK);
        //A柜伺服 回原
        sendDrugFunction.moveHandServoInit(configData);
        //B柜步进电机回原
        sendDrugFunction.cabinetBStepInit();
        //B柜伺服回原到初始上方扫码位置
        sendDrugFunction.cabinetBServoInit();

        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT_IS_END,"false");

        //判断机器是否正在发药
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"true");
        //开始上药 设置皮带没有开启
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_START,"false");
        //设置滑套传感器没触发
        valueOperations.set(RedisKeyConstant.sensor.TABLE_SENSOR,"false");
        //发药流程是否结束
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_BELT_FINISH,"false");
        //是否可以开始扫码
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_SCAN,"false");
        //是否可以开始检测有多个药
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_START,"false");
        //检测结果
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_DRUGS_RESULT,"false");
        //
        valueOperations.set(RedisKeyConstant.CABINET_B_TEST_RUN,"true");

        //伺服报警清楚
        valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_ERROR,"false");
    }


    public void stop() throws IOException {
        log.info("===============停止自动上药=================");
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"false");
        sendDrugFunction.autoDrug(CabinetConstants.CabinetBApplyCommand.AUTO, CabinetConstants.CabinetBApplyMode.STOP, CabinetConstants.CabinetBApplyStatus.ZERO);
        running = false;

        Map<String, Object> commandData = new HashMap<>();
        commandData.put("code", CommandEnums.DOSAGE_BUTTON_FINISH.getCode());
        commandData.put("data", "end");
        websocketService.sendInfo(CommandEnums.ON_CABINET_WEB.getCode(),commandData);
    }


    //查看设备是否正常 能开始上药
    public Boolean status(){
        if("true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_B))&&
            "true".equals(valueOperations.get(RedisKeyConstant.controlStatus.CABINET_A))&&
            "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.ABOVE))&&
            "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.SIDE))&&
            "true".equals(valueOperations.get(RedisKeyConstant.cameraStatus.BELOW))){
            return true;
        }else {
            return false;
        }
    }




}
