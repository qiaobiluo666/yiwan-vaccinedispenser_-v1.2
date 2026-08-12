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
 * PLC 状态轮询管理器 (地址200~320)
 * <p>
 * 功能说明：通过 PlcSendService 周期性读取地址200~320共121个保持寄存器。
 * 使用 ReentrantLock.tryLock(1s) 加锁机制防止并发执行。
 * 采用 scheduleWithFixedDelay 确保每轮完成后间隔100ms再发起下一轮。
 * <p>
 * 输入参数：通过 PlcSendService 发送读指令（起始地址200，数量121）
 * 输出参数：无（解析结果通过 PlcStatusDispatcher 输出日志）
 * 副作用：无
 * <p>
 * 加锁说明：
 *   - tryLock(1, SECONDS)：获取不到锁等待1秒，超时则跳过本轮并打印警告
 *   - unlock() 在 finally 中释放，确保不会死锁
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *   2024-05-19 yiwan - 锁机制由 lock() 改为 tryLock(1s) 超时处理
 *
 * @author yiwan
 */
@Slf4j
public class PlcPollManager {

    /** 读取起始地址 200 */
    private static final int START_ADDRESS = 200;
    /** 读取寄存器数量 (320-200+1 = 121) */
    private static final int REGISTER_COUNT = 121;
    /** 轮询间隔 500ms (上一轮完成后隔500ms再发下一轮) */
    private static final int POLL_INTERVAL_MS = 500;
    /** 每次请求超时 3000ms */
    private static final int REQUEST_TIMEOUT_MS = 3000;
    /** 锁超时时间 1000ms (tryLock 等待超时) */
    private static final int LOCK_TIMEOUT_MS = 1000;

    private final PlcSendService sendService;
    private final PlcConfig plcConfig;
    private final ScheduledExecutorService pollScheduler;
    private final ReentrantLock pollLock = new ReentrantLock();

    private volatile ScheduledFuture<?> pollTask;
    private volatile boolean running = false;

    public PlcPollManager(PlcSendService sendService, PlcConfig plcConfig) {
        this.sendService = sendService;
        this.plcConfig = plcConfig;
        this.pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "t_plc_poll");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动轮询
     * <p>
     * 采用 scheduleWithFixedDelay 确保上一轮全部逻辑(包括解析)完成后
     * 间隔 POLL_INTERVAL_MS(100ms) 再触发下一轮
     */
    public synchronized void start() {
        if (!plcConfig.isPollEnabled()) {
            log.info("[PLC] 轮询已通过配置文件关闭(plc.poll-enabled=false)");
            return;
        }
        if (running) {
            log.warn("[PLC] 轮询已在运行中");
            return;
        }
        running = true;
        pollTask = pollScheduler.scheduleWithFixedDelay(
                this::doPoll, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.info("[PLC] 轮询启动 | 间隔={}ms | 地址={}~{}",
                POLL_INTERVAL_MS, START_ADDRESS, START_ADDRESS + REGISTER_COUNT - 1);
    }

    /**
     * 停止轮询
     * <p>
     * 取消定时任务并释放资源
     */
    public synchronized void stop() {
        running = false;
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
        log.info("[PLC] 轮询已停止");
    }

    /**
     * 获取轮询运行状态
     *
     * @return true 表示轮询正在运行
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 单次轮询执行体
     * <p>
     * 加锁机制：
     *   1. tryLock(1s) 尝试获取锁，超时则跳过本轮（防止指令堆积）
     *   2. 获取锁后发送读取指令并等待响应
     *   3. finally 中 unlock() 释放锁
     * <p>
     * 超时处理：
     *   - 锁超时: 打印警告，跳过本轮，下一轮继续
     *   - 请求超时(sendAndWait返回null): 打印警告，本轮跳过
     *   - 异常响应: 打印异常码
     * <p>
     * 可靠性保证：
     *   - scheduleWithFixedDelay 确保间隔稳定
     *   - ReentrantLock 非公平锁，吞吐量优先
     *   - unlock 在 finally 块，异常不导致死锁
     */
    private void doPoll() {
        try {
            if (!pollLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("[PLC] 轮询锁超时(>{}ms)，跳过本轮", LOCK_TIMEOUT_MS);
                return;
            }
        } catch (InterruptedException e) {
            log.warn("[PLC] 轮询锁中断，跳过本轮");
            Thread.currentThread().interrupt();
            return;
        }

        try {
            ModbusFrame response = sendService.sendReadCommand(START_ADDRESS, REGISTER_COUNT, REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response == null) {
                log.warn("[PLC] 轮询超时(>{}ms)，无响应", REQUEST_TIMEOUT_MS);
                return;
            }
            if (response.isException()) {
                log.warn("[PLC] 轮询异常响应 | TID={} | code=0x{}",
                        response.getHeader().getTransactionId(),
                        Integer.toHexString(response.getExceptionCode() != null ? response.getExceptionCode().getCode() : 0));
            }
        } catch (Exception e) {
            log.error("[PLC] 轮询异常 | {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        } finally {
            pollLock.unlock();
        }
    }
}
