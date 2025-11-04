package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.system.dispensing.SendDrugFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        log.info("当前客户端：{} 连接服务器成功", nettyClient.getName());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = NettyUtils.decodeByteBuf(buf);
        String[] bytesStr = NettyUtils.getFormatHexStr(bytes).toUpperCase().split(" ");
        receiveService.receiveMsg(bytesStr);
        buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        nettyClient.onDisconnected();
        log.error("当前客户端：{} 发生异常: {}", nettyClient.getName(), cause.getMessage(), cause);
        ctx.close(); // 只关闭，不直接重连
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        log.warn("当前客户端：{} 断开连接", nettyClient.getName());
        nettyClient.setOnline(false);

         if("B".equals(nettyClient.getName()) || "A".equals(nettyClient.getName())){
             log.warn("断开连接,停止自动上药");
             sendDrugThreadManager.stop();
         }

        // EventLoop 内调度重连 — 串行执行
        ctx.channel().eventLoop().schedule(() -> {
            log.info("当前客户端：{} 开始重连...", nettyClient.getName());
            nettyClient.connect();
        }, 2, TimeUnit.SECONDS);
    }
}



