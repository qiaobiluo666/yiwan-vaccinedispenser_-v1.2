package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;



@Slf4j
public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final SendDrugThreadManager sendDrugThreadManager;
    private final NettyClient nettyClient;
    private final NettyReceiveCabinetService receiveService;

    public NettyClientHandler(NettyClient nettyClient, NettyReceiveCabinetService receiveService,SendDrugThreadManager sendDrugThreadManager) {
        this.nettyClient = nettyClient;
        this.receiveService = receiveService;
        this.sendDrugThreadManager = sendDrugThreadManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        nettyClient.onConnected();
        log.info("[{}] 连接成功", nettyClient.getName());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = NettyUtils.decodeByteBuf(buf);
            String[] bytesStr = NettyUtils.getFormatHexStr(bytes).toUpperCase().split(" ");
            receiveService.receiveMsg(bytesStr);
        } catch (Exception e) {
            log.error("[{}] 处理消息异常: {} - {}", nettyClient.getName(), 
                e.getClass().getSimpleName(), e.getMessage());
        } finally {
            if (msg instanceof ByteBuf) {
                ((ByteBuf) msg).release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 获取详细的错误信息
        String errorType = cause.getClass().getSimpleName();
        String errorMsg = cause.getMessage();
        String remoteAddress = ctx.channel() != null && ctx.channel().remoteAddress() != null 
            ? ctx.channel().remoteAddress().toString() : "unknown";
        
        // 区分不同类型的异常，记录详细信息
        if (cause instanceof ReadTimeoutException) {
            log.warn("[{}] 读超时断开 - 远程地址: {}", nettyClient.getName(), remoteAddress);
        } else if (cause instanceof WriteTimeoutException) {
            log.warn("[{}] 写超时断开 - 远程地址: {}", nettyClient.getName(), remoteAddress);
        } else if (cause instanceof java.io.IOException) {
            log.warn("[{}] IO异常断开 - {}: {} - 远程地址: {}", 
                nettyClient.getName(), errorType, errorMsg, remoteAddress);
        } else {
            log.error("[{}] 异常断开 - {}: {} - 远程地址: {}", 
                nettyClient.getName(), errorType, errorMsg, remoteAddress, cause);
        }
        
        // 先标记离线
        nettyClient.onDisconnected();
        
        // 关闭连接
        Channel channel = ctx.channel();
        if (channel != null && channel.isActive()) {
            ctx.close();
        }
        
        // 使用统一的重连方法，确保异常后能重连（不依赖 channelInactive）
        nettyClient.scheduleReconnect("异常触发");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        String remoteAddress = ctx.channel() != null && ctx.channel().remoteAddress() != null 
            ? ctx.channel().remoteAddress().toString() : "unknown";
        log.warn("[{}] 连接断开 - 远程地址: {}", nettyClient.getName(), remoteAddress);
        
        nettyClient.setOnline(false);

        if("B".equals(nettyClient.getName()) || "A".equals(nettyClient.getName())){
            sendDrugThreadManager.stop();
        }

        // 使用统一的重连方法，确保不会重复重连
        nettyClient.scheduleReconnect("连接断开");
    }

    // 处理空闲状态事件（心跳检测）
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case READER_IDLE:
                    // 暂不强制断开，待心跳包机制上线后再启用
                    break;
                case WRITER_IDLE:
                    // 写空闲，不处理，等待服务器端心跳
                    break;
                case ALL_IDLE:
                    // 暂不强制断开，待心跳包机制上线后再启用
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}



