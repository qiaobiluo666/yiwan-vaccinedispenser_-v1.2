package com.yiwan.vaccinedispenser.system.dispensing;


import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachine;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zhw
 * @date 2023/10/25
 * @Description
 */
@Slf4j
@Component
public class DispensingThreadManager {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Autowired
    private DispensingFunction dispensingFunction;

    private final TaskExecutor taskExecutor;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ConfigFunction configFunction;

    @Autowired
    private VacMachineService vacMachineService;

    @Autowired
    public DispensingThreadManager(@Qualifier("DispensingThreadPool") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    private volatile boolean running = true;
    @PostConstruct
    public void init() {
        redisInit();
        drop();
        moveBelt();
        blankStatus();
    }


    //掉药线程
    public void drop() {
        taskExecutor.execute(() -> {
            //开始掉药循环
            while (running){
                //判断是否有数据进入到疫苗发药机
                boolean shouldDrop = checkDropLayersInRedis();
                if(shouldDrop){

                    Integer ioTime = sysConfigService.getSendDrugConfigDataIOTime();

                    CountDownLatch latch = new CountDownLatch(5);
                    //开始正式掉药
                    //每次遍历5大层皮带上是否有药
                    for (int i = 1; i <= 5; i++) {
                        final int num = i;
                        taskExecutor.execute(() -> {
                            try {
                                dispensingFunction.dropDrugs(num,ioTime);
                            }catch (Exception e){
                                log.error("在第 " + num + " 层掉药时发生异常", e);

                            }finally {
                                latch.countDown(); // 任务完成后递减锁存器的计数
                            }
                        });

                        VacUntil.sleep(50);
                    }


                    try {
                        // 等待所有线程完成或超时
                        boolean completed = latch.await(10, TimeUnit.SECONDS); // 例如，超时时间为10秒
                        if (!completed) {
                            // 超时后的处理
                            log.warn("超时：并非所有线程在指定时间内完成。");
                        }else {
                            VacUntil.sleep(200);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                        log.error("掉药任务被中断", e);
                        break;
                    }
                }

            }

        });
    }







    //移动皮带
    public void moveBelt() {
        taskExecutor.execute(() -> {
            while (running) {
                //判断发药队列到底有没有数据
                boolean shouldDrop = checkBeltLayersInRedis();
                if(shouldDrop){
                    CountDownLatch latch = new CountDownLatch(1);
                    //开始正式动皮带
                    taskExecutor.execute(() -> {
                        try {
                            dispensingFunction.moveBelt();
                        }catch (Exception e){
                            log.error(String.valueOf(e));
                            log.error("皮带运输模块异常");
                        }finally {
                            latch.countDown(); // 任务完成后递减锁存器的计数
                        }
                    });


                    try {
                        // 等待所有线程完成或超时
                        boolean completed = latch.await(90, TimeUnit.SECONDS); // 例如，超时时间为60秒
                        if (!completed) {
                            // 超时后的处理
                            log.warn("超时：皮带移动任务未在指定时间内完成。");
                        }else {
                            VacUntil.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                        log.error("掉药任务被中断", e);
                        break;
                    }
                }


            }
        });
    }


    //停止线程
    public void stop() {
        running = false;
    }


    //判断掉药list是否有数据
    private boolean checkDropLayersInRedis() {

        for (int i = 1; i <= 5; i++) {
            String key = String.format(RedisKeyConstant.DROP_LIST, i);
            Long  size = listOps.size(key);
            if ( size!= null && size> 0) {
                // 如果任何一层有数据，则执行掉药操作
                return true;
            }
            VacUntil.sleep(200);
        }
        // 所有层都没有数据
        return false;
    }


    private boolean checkBeltLayersInRedis() {

        Long size = listOps.size(RedisKeyConstant.BELT_LIST);
        if (size!=null&&size>0) {
            return true;
        }
        // 所有层都没有数据
        return false;
    }


    //项目启动时 redis状态初始化
    private void redisInit(){
        //删除所有发药队列信息
        ScanOptions options = ScanOptions.scanOptions().match("Dispensing:*").build();
        try (var cursors = redisTemplate.executeWithStickyConnection(redisConnection ->
                redisConnection.scan(options))) {
            assert cursors != null;
            cursors.forEachRemaining(key -> {
                redisTemplate.delete(new String(key));
            });
        }


        //手动上药状态初始化
        valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");

        //自动发药状态初始化
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"false");
        //挡片状态查询初始化
//        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS_QUERY,"false");
        for(int i=1;i<=5;i++){

//            if(valueOperations.get(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,i))==null){
                //A柜皮带上是否有药 初始化
//                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,i),"false");
//            }

//            if(valueOperations.get(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG,i))==null){
                //A柜皮带上是否有药 初始化
//                valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_STOP_DRUG,i),"false");
//            }

            //A柜皮带上是否有药 初始化
            valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,i),"false");


        }

        //疫苗退回 不在运行状态
        valueOperations.set(RedisKeyConstant.DRUG_RETURN,"false");

        valueOperations.set(RedisKeyConstant.CABINET_A_CAN_DROP_DRUG,"true");

        //A柜光栅皮带上没有药初始化
        valueOperations.set(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG,"false");

        //C柜 运输初始化
        valueOperations.set(RedisKeyConstant.CABINET_C_WORK,"true");

        //TODO 数据库的可用库存和真实库存相等
        vacMachineService.vaccineNunEqualsUserNum();





        //TODO 查看A、B、C柜子的传感器状态



    }

    //判断挡片
    public  void blankStatus(){
        CompletableFuture.runAsync(() -> {
            ConfigSetting configSetting = configFunction.getSettingConfigData();
            if ("true".equals(configSetting.getCBlank())) {
                LocalTime now = LocalTime.now();
                LocalTime startTime = LocalTime.of(7, 50);
                LocalTime endTime = LocalTime.of(11, 0);
                if (now.isAfter(startTime) && now.isBefore(endTime)) {
                    log.info("查询是否开启");
                    dispensingFunction.openBlank();
                } else if (now.isAfter(endTime)) {
                    log.info("查询是否关闭");
                    dispensingFunction.closeBlank();
                }
            }
        });
    }

}
