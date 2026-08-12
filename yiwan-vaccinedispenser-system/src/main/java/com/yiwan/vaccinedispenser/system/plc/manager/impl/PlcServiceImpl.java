package com.yiwan.vaccinedispenser.system.plc.manager.impl;

import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.plc.client.PlcClient;
import com.yiwan.vaccinedispenser.system.plc.client.PlcClientHandler;
import com.yiwan.vaccinedispenser.system.plc.data.PlcRegisterReader;
import com.yiwan.vaccinedispenser.system.plc.data.RegisterInfo;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcConnectionManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcPollManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcService;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class PlcServiceImpl implements PlcService {

    private final PlcConnectionManager connectionManager;
    private final PlcPollManager pollManager;
    private final PlcSendService sendService;
    private final PlcClient plcClient;

    public PlcServiceImpl(PlcConnectionManager connectionManager, PlcPollManager pollManager,
                          PlcSendService sendService, PlcClient plcClient) {
        this.connectionManager = connectionManager;
        this.pollManager = pollManager;
        this.sendService = sendService;
        this.plcClient = plcClient;
    }

    @Override
    public Result<Void> connect() {
        log.info("[PLC] 手动连接");
        connectionManager.connect();
        return Result.success();
    }

    @Override
    public Result<Void> disconnect() {
        log.info("[PLC] 手动断开");
        connectionManager.disconnect();
        return Result.success();
    }

    @Override
    public Result<Boolean> isConnected() {
        return Result.success(connectionManager.isConnected());
    }

    @Override
    public Result<String> getStatus() {
        return Result.success(connectionManager.isConnected()
                ? "连接正常, 重试=" + connectionManager.getRetryCount()
                : "未连接, 重试=" + connectionManager.getRetryCount());
    }

    @Override
    public Result<Void> startPoll() {
        log.info("[PLC] 启动轮询");
        pollManager.start();
        return Result.success();
    }

    @Override
    public Result<Void> stopPoll() {
        log.info("[PLC] 停止轮询");
        pollManager.stop();
        return Result.success();
    }

    @Override
    public Result<Boolean> isPolling() {
        return Result.success(pollManager.isRunning());
    }

    @Override
    public Result<List<RegisterInfo>> getPlcStatus() {
        if (plcClient.getSocketChannel() != null && plcClient.getSocketChannel().pipeline() != null) {
            PlcClientHandler handler = (PlcClientHandler) plcClient.getSocketChannel().pipeline().get(PlcClientHandler.class);
            if (handler != null && handler.getLatestRegisterList() != null) {
                return Result.success(handler.getLatestRegisterList());
            }
        }
        return Result.success(null);
    }

    @Override
    public ModbusFrame sendReadCommand(int startAddr, int registerCount) {
        return sendService.sendReadCommand(startAddr, registerCount);
    }
}
