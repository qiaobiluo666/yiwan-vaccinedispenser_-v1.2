package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author 78671
 */
@Service
@Slf4j
public class TaskService {
    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private DispensingFunction dispensingFunction;
    private final ThreadPoolTaskScheduler scheduler;
    private ScheduledFuture<?>[] scheduledTasks = new ScheduledFuture<?>[4];

    public TaskService() {
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.initialize();
    }

    //开机自动查询四个定时任务的时间

    @PostConstruct
    public void initTasks() {
        ConfigSetting configSetting = configFunction.getSettingConfigData();
        String cBlankOpenMorning = configSetting.getCBlankOpenMorning();
        String cBlankCloseMorning = configSetting.getCBlankCloseMorning();
        String cBlankOpenAfternoon = configSetting.getCBlankOpenAfternoon();
        String cBlankCloseAfternoon = configSetting.getCBlankCloseAfternoon();


        if(cBlankOpenMorning!=null && !"00:00:00".equals(cBlankOpenMorning)){
            log.info("任务1：{}",cBlankOpenMorning);
            scheduleTask(0,cBlankOpenMorning);
        }

        if(cBlankCloseMorning!=null&& !"00:00:00".equals(cBlankCloseMorning)){
            log.info("任务2：{}",cBlankCloseMorning);
            scheduleTask(1,cBlankCloseMorning);
        }

        if(cBlankOpenAfternoon!=null&& !"00:00:00".equals(cBlankOpenAfternoon)){
            log.info("任务3：{}",cBlankOpenAfternoon);
            scheduleTask(2,cBlankOpenAfternoon);
        }

        if(cBlankCloseAfternoon!=null&& !"00:00:00".equals(cBlankCloseAfternoon)){
            log.info("任务4：{}",cBlankCloseAfternoon);
            scheduleTask(3,cBlankCloseAfternoon);
        }
    }

    private void scheduleTask(int index, String timeStr) {
        LocalTime taskTime = LocalTime.parse(timeStr);
        long delay = computeInitialDelay(taskTime);
        scheduledTasks[index] = scheduler.schedule(() -> {

            if(index==0 || index==2){
                log.info("执行任务开启挡片！当前时间：" + LocalTime.now());
                ConfigSetting configSetting = configFunction.getSettingConfigData();
                //是否有挡片配置
                if("true".equals(configSetting.getCBlank())){
                    dispensingFunction.closeBlank();
                }
            }else {
                log.info("执行任务关闭挡片！当前时间：" + LocalTime.now());
                ConfigSetting configSetting = configFunction.getSettingConfigData();
                //是否有挡片配置
                if("true".equals(configSetting.getCBlank())){
                    dispensingFunction.openBlank();
                }

            }

        }, new java.util.Date(System.currentTimeMillis() + delay));
    }

    private long computeInitialDelay(LocalTime taskTime) {
        LocalTime now = LocalTime.now();
        long delay = now.until(taskTime, java.time.temporal.ChronoUnit.MILLIS);
        return delay > 0 ? delay : delay + 24 * 60 * 60 * 1000;
    }

}

