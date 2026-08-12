package com.yiwan.vaccinedispenser.system.plc.manager;

import com.yiwan.vaccinedispenser.system.plc.config.PlcConfig;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 报警编号轮询管理器 (地址400~448)
 * <p>
 * 功能说明：通过 PlcSendService 周期性读取地址400~448共49个报警编号寄存器。
 * 使用 ReentrantLock.tryLock(1s) 加锁机制防止并发执行。
 * 采用 scheduleWithFixedDelay 确保每轮完成后间隔1s再发起下一轮。
 * <p>
 * 加锁说明：
 *   - tryLock(1, SECONDS)：获取不到锁等待1秒，超时则跳过本轮并打印警告
 *   - unlock() 在 finally 中释放，确保不会死锁
 *
 * @author yiwan
 */
@Slf4j
public class AlarmPollManager {

    /** 读取起始地址 400 */
    private static final int START_ADDRESS = 400;
    /** 读取寄存器数量 (448-400+1 = 49) */
    private static final int REGISTER_COUNT = 49;
    /** 轮询间隔 2000ms */
    private static final int POLL_INTERVAL_MS = 2000;
    /** 每次请求超时 3000ms */
    private static final int REQUEST_TIMEOUT_MS = 3000;
    /** 锁超时时间 1000ms */
    private static final int LOCK_TIMEOUT_MS = 1000;

    private final PlcSendService sendService;
    private final PlcConfig plcConfig;
    private final ScheduledExecutorService pollScheduler;
    private final ReentrantLock pollLock = new ReentrantLock();

    private volatile ScheduledFuture<?> pollTask;
    private volatile boolean running = false;

    public AlarmPollManager(PlcSendService sendService, PlcConfig plcConfig) {
        this.sendService = sendService;
        this.plcConfig = plcConfig;
        this.pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "t_plc_alarm_poll");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (!plcConfig.isAlarmPollEnabled()) {
            log.info("[PLC] 报警轮询已通过配置文件关闭(plc.alarm-poll-enabled=false)");
            return;
        }
        if (running) {
            log.warn("[PLC] 报警轮询已在运行中");
            return;
        }
        running = true;
        pollTask = pollScheduler.scheduleWithFixedDelay(
                this::doPoll, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.info("[PLC] 报警轮询启动 | 间隔={}ms | 地址={}~{}",
                POLL_INTERVAL_MS, START_ADDRESS, START_ADDRESS + REGISTER_COUNT - 1);
    }

    public synchronized void stop() {
        running = false;
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
        log.info("[PLC] 报警轮询已停止");
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 单次报警轮询执行体
     * <p>
     * 加锁机制：
     *   1. tryLock(1s) 尝试获取锁，超时则跳过本轮
     *   2. 获取锁后发送读取指令并等待响应
     *   3. finally 中 unlock() 释放锁
     */
    private void doPoll() {
        try {
            if (!pollLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("[PLC] 报警轮询锁超时(>{}ms)，跳过本轮", LOCK_TIMEOUT_MS);
                return;
            }
        } catch (InterruptedException e) {
            log.warn("[PLC] 报警轮询锁中断，跳过本轮");
            Thread.currentThread().interrupt();
            return;
        }

        try {
//            log.info("[PLC] 发起报警轮询 | 地址{}~{}, 共{}寄存器", START_ADDRESS, START_ADDRESS + REGISTER_COUNT - 1, REGISTER_COUNT);
            ModbusFrame response = sendService.sendReadCommand(START_ADDRESS, REGISTER_COUNT, REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response == null) {
                log.warn("[PLC] 报警轮询超时(>{}ms)，无响应", REQUEST_TIMEOUT_MS);
                return;
            }
            if (response.isException()) {
                log.warn("[PLC] 报警轮询异常响应 | TID={} | code=0x{}",
                        response.getHeader().getTransactionId(),
                        Integer.toHexString(response.getExceptionCode() != null ? response.getExceptionCode().getCode() : 0));
            }
        } catch (Exception e) {
            log.error("[PLC] 报警轮询异常 | {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        } finally {
            pollLock.unlock();
        }
    }
}
