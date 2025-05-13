package com.yiwan.vaccinedispenser.system.dispensing;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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

}
