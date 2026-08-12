package com.yiwan.vaccinedispenser.system.plc.manager;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.system.plc.client.PlcClient;
import com.yiwan.vaccinedispenser.system.plc.config.PlcConfig;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        plcClient.setConnectionManager(this);
        if (plcConfig.isAutoStart()) {
            log.info("[PLC] 开机自启已开启，开始初始化连接");
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
        pollManager.start();
        alarmPollManager.start();
        //连接成功，清除异常记录中的PLC掉线记录
        List<com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException> exceptions = vacMachineExceptionService.getExceptionByName(PLC_NAME);
        if (exceptions != null && !exceptions.isEmpty()) {
            vacMachineExceptionService.delExceptionByName(exceptions);
            log.info("[PLC] 已清除PLC掉线异常记录({}条)", exceptions.size());
        }
        log.info("[PLC] 连接管理器：已连接，轮询已启动");
    }

    public void onDisconnected() {
        this.connected = false;
        pollManager.stop();
        alarmPollManager.stop();
        //记录PLC掉线异常（仅当无重复记录时）
        if (vacMachineExceptionService.getExceptionByName(PLC_NAME).isEmpty()) {
            String errorMsg = "当前客户端：PLC PLC连接断开";
            vacMachineExceptionService.sendException(SettingConstants.MachineException.CONTROLLER.code, PLC_NAME, errorMsg);
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
