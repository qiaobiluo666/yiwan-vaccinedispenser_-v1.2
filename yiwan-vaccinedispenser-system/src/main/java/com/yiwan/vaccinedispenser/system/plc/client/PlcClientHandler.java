package com.yiwan.vaccinedispenser.system.plc.client;

import com.yiwan.vaccinedispenser.system.plc.data.PlcRegisterReader;
import com.yiwan.vaccinedispenser.system.plc.data.RegisterInfo;
import com.yiwan.vaccinedispenser.system.plc.protocol.FunctionCode;
import com.yiwan.vaccinedispenser.system.plc.protocol.MbapHeader;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Netty 入站处理器 - 处理 PLC Modbus TCP 响应
 * <p>
 * 功能说明：
 * 1. 接收 ModbusFrame 解码结果，完成 pending 请求的 CompletableFuture
 * 2. 对 0x03 读保持寄存器响应，按 Redis 缓存的起始地址解析寄存器数据
 * 3. 下发解析结果到 PlcStatusDispatcher 进行日志和业务分发
 * <p>
 * 输入参数：Netty pipeline 传入的 ModbusFrame 对象
 * 输出参数：无（数据存入 latestRegisterList 供外部查询）
 * 副作用：自动调用 plClient.completePendingRequest() 解除 sendAndWait 阻塞
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *   2024-05-19 yiwan - 重构为 Redis 匹配起始地址
 *
 * @author yiwan
 */
@Slf4j
public class PlcClientHandler extends ChannelInboundHandlerAdapter {

    /** PLC 客户端引用 */
    private final PlcClient plcClient;

    /** 最新解析的寄存器列表
     * -- GETTER --
     *  获取最新解析的寄存器列表
     *
     * @return RegisterInfo 列表，未解析过时返回 null
     */
    @Getter
    private volatile List<RegisterInfo> latestRegisterList;

    /**
     * 构造处理器
     *
     * @param plcClient PlcClient 实例，用于完成 pending 请求
     */
    public PlcClientHandler(PlcClient plcClient) {
        this.plcClient = plcClient;
    }

    /**
     * 通道活跃回调 - 连接建立成功
     * <p>
     * 通知 PlcClient 更新在线状态
     *
     * @param ctx Netty 通道上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        plcClient.onConnected();
        log.info("[PLC] 连接成功 {}:{}", plcClient.getHost(), plcClient.getPort());
    }

    /**
     * 通道读回调 - 收到 PLC 响应
     * <p>
     * 处理流程：
     * 1. 仅处理 ModbusFrame 类型
     * 2. 完成 pending 请求（解除 sendAndWait 阻塞）
     * 3. 解析 0x03 读保持寄存器响应
     * <p>
     * 输入参数：msg - 上游 ModbusDecoder 解码出的 ModbusFrame
     *
     * @param ctx Netty 通道上下文
     * @param msg 上游传递的消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ModbusFrame)) {
            return;
        }

        ModbusFrame frame = (ModbusFrame) msg;
        int transactionId = frame.getHeader().getTransactionId();

        //收到响应完整Hex打印
        log.info("[PLC] 收到响应 | TID={} | Hex: {}", transactionId, frameToHex(frame));

        plcClient.completePendingRequest(transactionId, frame);

        if (frame.isException()) {
            log.warn("[PLC] 异常响应 | TID={} | fc=0x{} | code=0x{}",
                    transactionId, Integer.toHexString(frame.getActualFunctionCode()),
                    Integer.toHexString(frame.getExceptionCode() != null ? frame.getExceptionCode().getCode() : 0));
            return;
        }

        if (frame.getFunctionCode() == FunctionCode.READ_HOLDING_REGISTERS.getCode()) {
            handleReadResponse(frame, transactionId);
        } else {
            byte[] data = frame.getData();
            if (data != null && data.length >= 4 && frame.getFunctionCode() == FunctionCode.WRITE_SINGLE_REGISTER.getCode()) {
                int respAddr = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                int respVal = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                log.info("[PLC] 写寄存器响应 | TID={} | fc=0x6 | 地址={}(0x{}) | 值={}(0x{})",
                        transactionId, respAddr, Integer.toHexString(respAddr), respVal, Integer.toHexString(respVal));
            } else if (data != null && data.length >= 4 && frame.getFunctionCode() == FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode()) {
                int respAddr = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                int respCount = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                log.info("[PLC] 写多寄存器响应 | TID={} | fc=0x10 | 地址={}(0x{}) | 数量={}",
                        transactionId, respAddr, Integer.toHexString(respAddr), respCount);
            } else {
                log.info("[PLC] 其他功能码响应 | TID={} | fc=0x{} | data={}",
                        transactionId, Integer.toHexString(frame.getFunctionCode()),
                        data != null ? bytesToHex(data) : "");
            }
        }
    }

    /**
     * 处理 0x03 读保持寄存器响应
     * <p>
     * 处理流程：
     * 1. 从 Redis 按事务标识符(TID)查找起始地址
     * 2. 去掉响应数据的第 1 字节(byteCount)，剩余为裸寄存器数据
     * 3. 调用 PlcRegisterReader.parse() 解析
     * 4. 调用 PlcStatusDispatcher.dispatch() 分发
     * <p>
     * 输入参数：
     *   frame - 响应帧
     *   transactionId - 事务标识符（用于 Redis 查询）
     *
     * @param frame         响应帧
     * @param transactionId 事务标识符
     */
    private void handleReadResponse(ModbusFrame frame, int transactionId) throws Exception {
        byte[] data = frame.getData();
        if (data == null || data.length < 1) {
            return;
        }

        byte[] registerBytes = Arrays.copyOfRange(data, 1, data.length);

        int startAddr = 200;
        if (plcClient.getRequestCache() != null) {
            int[] meta = plcClient.getRequestCache().getAndRemove(transactionId);
            if (meta != null) {
                startAddr = meta[0];
            }
        }

        List<RegisterInfo> registerList = PlcRegisterReader.parse(registerBytes, startAddr);
        if (!registerList.isEmpty()) {
            this.latestRegisterList = registerList;
            //全量打印收到的寄存器数据
            StringBuilder sb = new StringBuilder();
            for (RegisterInfo info : registerList) {
                sb.append(" [").append(info.getAddress()).append("]")
                        .append(info.getParamName()).append("=")
                        .append(info.isBarcode() ? info.getBarcodeValue() : info.getActualValue());
            }

            if (plcClient.getPlcStatusDispatcher() != null) {
                plcClient.getPlcStatusDispatcher().dispatch(registerList);
            }
        }
    }

    /**
     * 通道关闭回调 - 连接断开
     * <p>
     * 通知 PlcClient 下线并触发自动重连
     *
     * @param ctx Netty 通道上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String addr = ctx.channel() != null && ctx.channel().remoteAddress() != null
                ? ctx.channel().remoteAddress().toString() : "unknown";
        log.warn("[PLC] 连接断开 | {}", addr);
        plcClient.onDisconnected();
        plcClient.scheduleReconnect("连接断开");
    }

    /**
     * 异常捕获回调
     * <p>
     * IO 异常捕获后通知 PlcClient 下线并自动重连
     *
     * @param ctx   Netty 通道上下文
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String errorType = cause.getClass().getSimpleName();
        String errorMsg = cause.getMessage();
        String addr = ctx.channel() != null && ctx.channel().remoteAddress() != null
                ? ctx.channel().remoteAddress().toString() : "unknown";

        if (cause instanceof IOException) {
            log.warn("[PLC] IO断开 | {}:{} | {}", errorType, errorMsg, addr);
        } else {
            log.error("[PLC] 异常断开 | {}:{} | {}", errorType, errorMsg, addr, cause);
        }

        plcClient.onDisconnected();
        Channel channel = ctx.channel();
        if (channel != null && channel.isActive()) {
            ctx.close();
        }
        plcClient.scheduleReconnect("异常触发");
    }

    /**
     * 将ModbusFrame转为空格分隔的Hex字符串（完整报文，方便查问题）
     *
     * @param frame ModbusFrame
     * @return Hex字符串，如 "00 01 00 00 00 06 01 03 01 1A 00 19"
     */
    /**
     * 字节数组转 Hex 字符串（用于日志）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    private static String frameToHex(ModbusFrame frame) {
        MbapHeader header = frame.getHeader();
        int pduLength = 1 + (frame.getData() != null ? frame.getData().length : 0);
        int length = pduLength + 1;
        StringBuilder sb = new StringBuilder();
        appendHex(sb, (header.getTransactionId() >> 8) & 0xFF);
        appendHex(sb, header.getTransactionId() & 0xFF);
        appendHex(sb, (header.getProtocolId() >> 8) & 0xFF);
        appendHex(sb, header.getProtocolId() & 0xFF);
        appendHex(sb, (length >> 8) & 0xFF);
        appendHex(sb, length & 0xFF);
        appendHex(sb, header.getUnitId());
        appendHex(sb, frame.getFunctionCode());
        if (frame.getData() != null) {
            for (byte b : frame.getData()) {
                appendHex(sb, b & 0xFF);
            }
        }
        return sb.toString();
    }

    private static void appendHex(StringBuilder sb, int value) {
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(String.format("%02X", value));
    }
}
