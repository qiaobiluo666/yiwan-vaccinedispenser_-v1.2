package com.yiwan.vaccinedispenser.system.plc.client.codec;

import com.yiwan.vaccinedispenser.system.plc.protocol.MbapHeader;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Modbus TCP 报文编码器（Netty MessageToByteEncoder）
 * <p>
 * 功能说明：将 ModbusFrame 对象编码为 Modbus TCP 协议字节流写入 ByteBuf。
 * 编码顺序：
 * - 事务标识符(2B) + 协议标识符(2B,0x0000) + 长度(2B) + 单元标识符(1B) + 功能码(1B) + 数据(NB)
 * <p>
 * 输入参数：ModbusFrame 对象
 * 输出参数：ByteBuf - 写入 Netty 通道的字节缓冲区
 * 副作用：无
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *
 * @author yiwan
 */
@Slf4j
public class ModbusEncoder extends MessageToByteEncoder<ModbusFrame> {

    /**
     * 编码入口
     * <p>
     * 输入参数：
     *   frame - 待发送的 Modbus 帧
     *   out - 目标字节缓冲区
     *
     * @param ctx   Netty 通道上下文
     * @param frame 待编码的 ModbusFrame
     * @param out   输出 ByteBuf
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, ModbusFrame frame, ByteBuf out) {
        MbapHeader header = frame.getHeader();

        int pduLength = 1 + (frame.getData() != null ? frame.getData().length : 0);

        out.writeShort(header.getTransactionId());
        out.writeShort(header.getProtocolId());
        out.writeShort(pduLength + 1);
        out.writeByte(header.getUnitId());
        out.writeByte(frame.getFunctionCode());

        if (frame.getData() != null && frame.getData().length > 0) {
            out.writeBytes(frame.getData());
        }

    }
}
