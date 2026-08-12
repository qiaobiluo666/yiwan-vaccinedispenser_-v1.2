package com.yiwan.vaccinedispenser.system.plc.manager;

import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.plc.data.RegisterInfo;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;

import java.util.List;

/**
 * PLC对外Service接口
 *
 * @author yiwan
 */
public interface PlcService {

    Result<Void> connect();

    Result<Void> disconnect();

    Result<Boolean> isConnected();

    Result<String> getStatus();

    Result<Void> startPoll();

    Result<Void> stopPoll();

    Result<Boolean> isPolling();

    Result<List<RegisterInfo>> getPlcStatus();

    /**
     * 通用读取保持寄存器
     *
     * @param startAddr     起始地址(十进制)
     * @param registerCount 寄存器数量
     * @return 响应帧
     */
    ModbusFrame sendReadCommand(int startAddr, int registerCount);
}
