package com.yiwan.vaccinedispenser.system.plc.manager;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.system.plc.client.PlcClient;
import com.yiwan.vaccinedispenser.system.plc.config.PlcConfig;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PlcConnectionManager {

    private static final String PLC_NAME = "PLC";

    private final PlcClient plcClient;
    private final PlcConfig plcConfig;
    private final PlcPollManager pollManager;
    private final AlarmPollManager alarmPollManager;
    private final VacMachineExceptionService vacMachineExceptionService;

    @Getter
    private volatile boolean connected = false;

    private final AtomicInteger retryCount = new AtomicInteger(0);

    public PlcConnectionManager(PlcClient plcClient, PlcConfig plcConfig, PlcPollManager pollManager, AlarmPollManager alarmPollManager, VacMachineExceptionService vacMachineExceptionService) {
        this.plcClient = plcClient;
        this.plcConfig = plcConfig;
        this.pollManager = pollManager;
        this.alarmPollManager = alarmPollManager;
        this.vacMachineExceptionService = vacMachineExceptionService;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        plcClient.setConnectionManager(this);
        if (plcConfig.isAutoStart()) {
            log.info("[PLC] 程序已完全启动，开始初始化连接");
            plcClient.init();
            plcClient.connect();
        } else {
            log.info("[PLC] 开机自启已关闭，等待手动启动");
        }
    }

    public void connect() {
        plcClient.init();
        plcClient.connect();
    }

    public void disconnect() {
        plcClient.disconnect();
    }

    public boolean isConnected() {
        return plcClient.isActive();
    }

    public void onConnected() {
        this.connected = true;
        retryCount.set(0);
        try {
            pollManager.start();
            alarmPollManager.start();
        } catch (Exception pollError) {
            log.error("[PLC] 连接成功但启动轮询失败", pollError);
        }
        try {
            //连接成功，清除异常记录中的PLC掉线记录
            List<com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException> exceptions =
                    vacMachineExceptionService.getExceptionByName(PLC_NAME);
            if (exceptions != null && !exceptions.isEmpty()) {
                vacMachineExceptionService.delExceptionByName(exceptions);
                log.info("[PLC] 已清除PLC掉线异常记录({}条)", exceptions.size());
            }
        } catch (Exception alarmError) {
            log.error("[PLC] 清除掉线报警失败，PLC连接和轮询不受影响", alarmError);
        }
        log.info("[PLC] 连接管理器：已连接，轮询已启动");
    }

    public void onDisconnected() {
        this.connected = false;
        try {
            pollManager.stop();
            alarmPollManager.stop();
        } catch (Exception pollError) {
            log.error("[PLC] 停止轮询失败，不影响自动重连", pollError);
        }
        try {
            //记录PLC掉线异常（仅当无重复记录时）
            if (vacMachineExceptionService.getExceptionByName(PLC_NAME).isEmpty()) {
                String errorMsg = "当前客户端：PLC PLC连接断开";
                vacMachineExceptionService.sendException(
                        SettingConstants.MachineException.CONTROLLER.code, PLC_NAME, errorMsg);
            }
        } catch (Exception alarmError) {
            log.error("[PLC] 写入掉线报警失败，不影响自动重连", alarmError);
        }
        log.info("[PLC] 连接管理器：已断开，轮询已停止");
    }

    public int getRetryCount() {
        return retryCount.get();
    }

    public void resetRetryCount() {
        retryCount.set(0);
    }
}
