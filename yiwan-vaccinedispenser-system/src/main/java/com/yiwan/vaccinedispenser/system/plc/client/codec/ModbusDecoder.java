package com.yiwan.vaccinedispenser.system.plc.client.codec;

import com.yiwan.vaccinedispenser.system.plc.protocol.MbapHeader;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Modbus TCP 报文解码器（Netty ByteToMessageDecoder）
 * <p>
 * 功能说明：将 TCP 字节流中的 Modbus TCP 帧解码为 ModbusFrame 对象。
 * 遵循标准 Modbus TCP 协议规范：
 * - MBAP 头 7 字节：事务标识符(2B) + 协议标识符(2B,固定0x0000) + 长度(2B) + 单元标识符(1B)
 * - PDU: 功能码(1B) + 数据(NB)
 * <p>
 * 输入参数：ByteBuf - Netty 字节缓冲区（由上游 LengthFieldBasedFrameDecoder 保证一帧完整）
 * 输出参数：out.add(ModbusFrame) - 解码后的帧对象
 * 副作用：非法帧（协议ID≠0 / 长度超范围 / 从站ID≠0x01）自动清除缓冲区
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *   2024-05-19 yiwan - 增加协议校验（协议ID/长度/从站ID）
 *
 * @author yiwan
 */
@Slf4j
public class ModbusDecoder extends ByteToMessageDecoder {

    /** MBAP 头固定长度 7 字节 */
    private static final int MBAP_LENGTH = 7;

    /**
     * 解码入口
     * <p>
     * 处理流程：
     * 1. 读取 MBAP 7 字节
     * 2. 校验协议标识符必须为 0x0000
     * 3. 校验长度必须在 [2, 256] 范围内
     * 4. 校验从站 ID 必须为 0x01
     * 5. 按长度等待数据完整后读取功能码和数据
     * <p>
     * 输入参数：
     *   in - Netty 字节缓冲区（已由上游处理一帧完整数据）
     *   out - 输出列表，添加解码后的 ModbusFrame
     *
     * @param ctx Netty 通道上下文
     * @param in  输入字节缓冲区
     * @param out 输出对象列表
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MBAP_LENGTH) {
            return;
        }

        in.markReaderIndex();

        int transactionId = in.readUnsignedShort();
        int protocolId = in.readUnsignedShort();
        int length = in.readUnsignedShort();
        int unitId = in.readUnsignedByte();

        if (protocolId != 0) {
            log.warn("[ModbusDecoder] 协议ID非法: 期望0x0000, 实际0x{}, 丢弃", Integer.toHexString(protocolId));
            in.clear();
            return;
        }

        if (length < 2 || length > 256) {
            log.warn("[ModbusDecoder] 长度非法: {} 超出[2,256], 丢弃", length);
            in.clear();
            return;
        }

        if (unitId != 0x01) {
            log.warn("[ModbusDecoder] 从站ID非法: 期望0x01, 实际0x{}, 丢弃", Integer.toHexString(unitId));
            in.clear();
            return;
        }

        int remaining = length - 1;
        if (in.readableBytes() < remaining) {
            in.resetReaderIndex();
            return;
        }

        int functionCode = in.readUnsignedByte();

        int dataLength = length - 2;
        byte[] data = null;
        if (dataLength > 0) {
            data = new byte[dataLength];
            in.readBytes(data);
        }


        MbapHeader header = new MbapHeader(transactionId, protocolId, length, unitId);
        ModbusFrame frame = new ModbusFrame(header, functionCode, data);
        out.add(frame);
    }
}
