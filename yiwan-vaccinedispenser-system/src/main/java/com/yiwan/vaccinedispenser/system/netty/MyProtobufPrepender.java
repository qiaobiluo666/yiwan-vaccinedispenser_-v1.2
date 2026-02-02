package com.yiwan.vaccinedispenser.system.netty;

import cn.hutool.core.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public class MyProtobufPrepender  extends ByteToMessageDecoder {

    // 最大等待数据包长度，防止恶意或错误数据导致内存溢出
    private static final int MAX_FRAME_LENGTH = 8192;
    // 最大跳过字节数，防止无效数据一直占用缓冲区
    private static final int MAX_SKIP_BYTES = 1024;
    private int skippedBytes = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 5) {
            // 重置跳过字节计数
            if (in.readableBytes() < 5) {
                skippedBytes = 0;
                break;
            }
            
            // 标记当前读取位置
            in.markReaderIndex();

            // 读取开头 2 字节
            short start = in.readShort();

            // 校验开头 2 字节
            if (!Objects.equals(String.format("%04X", start), "A55A")) {
                // 开头字节不匹配，跳过当前字节
                in.resetReaderIndex();
                in.skipBytes(1);
                skippedBytes++;
                
                // 如果跳过太多字节，可能是数据流错误，重置并等待新数据
                if (skippedBytes > MAX_SKIP_BYTES) {
                    log.warn("跳过字节数超过限制 {}，重置解码器", MAX_SKIP_BYTES);
                    skippedBytes = 0;
                    in.clear();
                    break;
                }
                continue;
            }

            // 重置跳过计数
            skippedBytes = 0;

            // 读取数据长度（低位在前，高位在后）
            short dataLengthLow = in.readUnsignedByte();
            short dataLengthHigh = in.readUnsignedByte();
            int dataLength = (dataLengthHigh << 8) | dataLengthLow;

            // 检查数据长度是否合理
            if (dataLength < 0 || dataLength > MAX_FRAME_LENGTH) {
                log.warn("数据长度异常: {}，跳过当前数据包", dataLength);
                in.resetReaderIndex();
                in.skipBytes(1);
                continue;
            }

            // 等待至少有 dataLength + 1 字节可读（包括数据长度字段和末尾 1 字节）
            if (in.readableBytes() < dataLength + 1) {
                // 数据未完整到达，重置读取位置等待下一次解码
                in.resetReaderIndex();
                break;
            }

            // 读取数据部分（长度为 dataLength）
            ByteBuf data = in.readSlice(dataLength);

            // 读取末尾 1 字节
            byte end = in.readByte();

            // 校验末尾 1 字节
            if (end != 0x55) {
                // 末尾字节不匹配，跳过当前字节并继续寻找下一个有效数据
                log.warn("数据包末尾校验失败，期望 0x55，实际: 0x{}", String.format("%02X", end & 0xFF));
                in.resetReaderIndex();
                in.skipBytes(1);
                continue;
            }

            // 合并完整的指令数据
            ByteBuf message = ctx.alloc().buffer(4 + dataLength + 1);
            message.writeShort(start);
            message.writeByte(dataLengthLow);
            message.writeByte(dataLengthHigh);
            message.writeBytes(data);
            message.writeByte(end);

            // 将解码的指令数据添加到输出列表中
            out.add(message);
        }
    }

}