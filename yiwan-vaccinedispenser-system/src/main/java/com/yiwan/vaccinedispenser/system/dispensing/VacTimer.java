package com.yiwan.vaccinedispenser.system.dispensing;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.test.UploadController;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.http.HttpConnectTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/6/18 20:28
 */
@Component
@Slf4j
public class VacTimer {
    @Autowired
    private ZcyFunction zcyFunction;


    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UploadController uploadController;


    @Autowired
    private VacMachineService vacMachineService;

    /** 发药队列停滞跟踪 key */
    private static final String SEND_LIST_STALE_SINCE = "Machine:sendList:staleSince";
    private static final String SEND_LIST_STALE_CONTENT = "Machine:sendList:staleContent";
    private static final long STALE_TIMEOUT_MS = 5 * 60 * 1000;

    /**
     * 每分钟检查 SEND_LIST，如果只有1条且5分钟内数量、内容不变则清除
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanStaleSendList() {
        Long size = listOps.size(RedisKeyConstant.SEND_LIST);
        if (size == null || size != 1) {
            //数量不为1，清除跟踪状态
            redisTemplate.delete(SEND_LIST_STALE_SINCE);
            redisTemplate.delete(SEND_LIST_STALE_CONTENT);
            return;
        }
        //正好1条
        String currentContent = listOps.index(RedisKeyConstant.SEND_LIST, 0);
        String staleSinceStr = valueOperations.get(SEND_LIST_STALE_SINCE);
        String staleContent = valueOperations.get(SEND_LIST_STALE_CONTENT);

        if (staleSinceStr == null) {
            //首次发现1条，记录时间戳和内容
            valueOperations.set(SEND_LIST_STALE_SINCE, String.valueOf(System.currentTimeMillis()));
            valueOperations.set(SEND_LIST_STALE_CONTENT, currentContent);
            return;
        }

        //内容变了，重置跟踪
        if (!currentContent.equals(staleContent)) {
            valueOperations.set(SEND_LIST_STALE_SINCE, String.valueOf(System.currentTimeMillis()));
            valueOperations.set(SEND_LIST_STALE_CONTENT, currentContent);
            return;
        }

        //内容不变，检查是否超时
        long staleSince = Long.parseLong(staleSinceStr);
        if (System.currentTimeMillis() - staleSince >= STALE_TIMEOUT_MS) {
            log.info("[定时] 发药队列滞留超过5分钟，清除: {}", currentContent);
            listOps.leftPop(RedisKeyConstant.SEND_LIST);
            redisTemplate.delete(SEND_LIST_STALE_SINCE);
            redisTemplate.delete(SEND_LIST_STALE_CONTENT);
        }
    }

    /**
     * 一分钟轮询 挡片开启时长超过10min 自动关闭
     */

    @Scheduled(fixedDelay = 60000)
    public void closeBlankDetail(){
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        if ("true".equals(configSetting.getCBlank())) {
            LocalTime now = LocalTime.now();
            LocalTime mornCloseTime = LocalTime.parse(configSetting.getCBlankCloseMorning());
            LocalTime afterOpenTime = LocalTime.parse(configSetting.getCBlankOpenAfternoon());
            LocalTime afterCloseTime = LocalTime.parse(configSetting.getCBlankCloseAfternoon());
            if(!"00:00:00".equals(configSetting.getCBlankCloseAfternoon())&&!"00:00:00".equals(configSetting.getCBlankOpenAfternoon())){
                if (now.isAfter(afterCloseTime)) {
                    dispensingFunction.closeBlankMinute();
                }else if(now.isAfter(mornCloseTime)&&now.isBefore(afterOpenTime)){
                    dispensingFunction.closeBlankMinute();
            }
            }else {
                if (now.isAfter(mornCloseTime)) {
                    dispensingFunction.closeBlankMinute();
                }
            }
        }
    }




//    /**
//     * 每天 7:50 开启挡片
//     */
//    @Scheduled(cron = "0 50 7 * * ?")
//    public void openBlank() {
//        ConfigSetting configSetting = configFunction.getSettingConfigData();
//        //是否有挡片配置
//        if("true".equals(configSetting.getCBlank())){
//            dispensingFunction.openBlank();
//        }
//
//    }






//    /**
//     * 每天 11:00 关闭挡片
//     */
//    @Scheduled(cron = "0 0 11 * * ?")
//    public void closeBlank() {
//        ConfigSetting configSetting = configFunction.getSettingConfigData();
//        //是否有挡片配置
//        if("true".equals(configSetting.getCBlank())){
//            dispensingFunction.closeBlank();
//        }
//    }



    /**
     * 每隔一天获取疫苗列表
     * @throws Exception
     */
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    public void getVaccine() throws Exception {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        try {
            log.info(configSetting.getZcySend());
            if("true".equals(configSetting.getZcySend())){
                zcyFunction.getVaccine();
            }
        } catch (HttpHostConnectException e){
            log.error("政采云通讯异常！");
        }catch (Exception e) {
            // 捕获其它异常
            log.error("未知异常：",e);
        }


    }

    /**
     * 每隔俩秒获取发药处方
     * @throws Exception
     */
    @Scheduled(fixedDelay = 2000)
    public void getVaccineSendMsg() throws Exception {
        ConfigSetting configSetting = configFunction.getSettingConfigData();

    try {

        if("true".equals(configSetting.getZcySend())){
//            log.info("获取政采云发苗指令");
            zcyFunction.getVaccineSendMsg();
        }
    } catch (HttpHostConnectException e){
        log.error("政采云通讯异常！");
    }catch (Exception e) {
        // 捕获其它异常
       log.error("未知异常：",e);
        }
    }



    @Scheduled(fixedDelay = 2000)
    public void getVaccineMsg() throws Exception {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        if("true".equals(configSetting.getZcySend()) && "true".equals(configSetting.getIsWarn())){
            uploadController.getZycVaccineMsg();
        }
    }



    /**
     * 检测C柜底部是否有药品
     * @throws Exception
     */
    @Scheduled(fixedDelay = 5000)
    public void getSenor() throws Exception {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        //轮询传感器是否有药
        if("true".equals(configSetting.getIsCloseSensor())){
            vacMachineService.isSensorHaveDrug(configSetting);
        }
    }

}
