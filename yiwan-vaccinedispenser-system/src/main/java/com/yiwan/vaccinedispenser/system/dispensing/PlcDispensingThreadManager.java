package com.yiwan.vaccinedispenser.system.dispensing;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineService;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnExpression("${plc.enable:true}")
public class PlcDispensingThreadManager {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Autowired
    private PlcDispensingFunction plcDispensingFunction;

    @Autowired
    private VacMachineService vacMachineService;

    private final TaskExecutor taskExecutor;

    private volatile boolean running = true;

    @Autowired
    public PlcDispensingThreadManager(@Qualifier("DispensingThreadPool") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void init() {
        redisInit();
        taskExecutor.execute(() -> {
            log.info("[PLC] 发药线程启动");
            while (running) {
                boolean shouldDrop = checkPlcSendList();
                if (shouldDrop) {
                    try {
                        plcDispensingFunction.plcSendDrug();
                    } catch (Exception e) {
                        log.error("[PLC] 发药异常", e);
                    }
                }
                VacUntil.sleep(100);
            }
            log.info("[PLC] 发药线程已停止");
        });
    }

    public void stop() {
        running = false;
    }

    private boolean checkPlcSendList() {
        Long size = listOps.size(RedisKeyConstant.PLC_SEND_LIST);
        return size != null && size > 0;
    }

    private void redisInit() {
        //清除PLC发药队列
        try {
            String key = RedisKeyConstant.PLC_SEND_LIST;
            redisTemplate.delete(key);
            key = RedisKeyConstant.BELT_LIST;
            redisTemplate.delete(key);
            key = RedisKeyConstant.SEND_LIST;
            redisTemplate.delete(key);
            key = RedisKeyConstant.PLC_SEND_DRUG_MSG;
            redisTemplate.delete(key);
            key = RedisKeyConstant.PLC_RESERVED_MACHINES;
            redisTemplate.delete(key);

            log.info("[PLC] 清理PLC发药队列");
        } catch (Exception e) {
            log.warn("[PLC] 启动时清理PLC发药队列失败: {}", e.getMessage(), e);
        }

        //删除所有发药队列信息
        ScanOptions options = ScanOptions.scanOptions().match("Dispensing:*").build();
        try (var cursors = redisTemplate.executeWithStickyConnection(redisConnection ->
                redisConnection.scan(options))) {
            assert cursors != null;
            cursors.forEachRemaining(key -> {
                redisTemplate.delete(new String(key));
            });
        }

        try {
            Set<String> keys = redisTemplate.keys("Machine:cameraNum*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[PLC] 清理 Redis Key：{}", keys);
            } else {
                log.info("[PLC] Redis 中没有历史摄像头计数 Key");
            }
        } catch (Exception e) {
            log.warn("[PLC] 启动时清理 Redis Key 失败: {}", e.getMessage());
        }

        //手动上药状态初始化
        valueOperations.set(RedisKeyConstant.handDrugStatus.HAND_START_STATUS,"true");

        //自动发药状态初始化
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START,"false");
        for(int i=1;i<=5;i++){
            //A柜皮带上是否有药 初始化
            valueOperations.set(String.format(RedisKeyConstant.CABINET_A_BELT_HAVE_DRUG,i),"false");
        }
        //库存盘点初始化
        valueOperations.set(RedisKeyConstant.DRUG_INVENTORY_START,"false");
        //不在发苗种
        valueOperations.set(RedisKeyConstant.DRUG_RUN_START,"false");

        //疫苗退回 不在运行状态
        valueOperations.set(RedisKeyConstant.DRUG_RETURN,"false");

        //异常清理 不在运行状态
        valueOperations.set(RedisKeyConstant.DRUG_ERROR_START,"false");

        valueOperations.set(RedisKeyConstant.CABINET_A_CAN_DROP_DRUG,"true");

        //A柜光栅皮带上没有药初始化
        valueOperations.set(RedisKeyConstant.CABINET_A_GS_BELT_HAVE_DRUG,"false");

        //C柜 运输初始化
        valueOperations.set(RedisKeyConstant.CABINET_C_WORK,"true");

        //TODO 数据库的可用库存和真实库存相等
        vacMachineService.vaccineNunEqualsUserNum();

        //机械手是否可以开始
        valueOperations.set(RedisKeyConstant.handMachine.HAND_DROP_START,"true");

        //B伺服报警清除
        valueOperations.set(RedisKeyConstant.CABINET_B_SERVO_ERROR,"false");

        //自动盘点初始化
        valueOperations.set(RedisKeyConstant.AUTO_IS_START,"false");

        log.info("[PLC] 发药线程初始化完成");
    }
}
