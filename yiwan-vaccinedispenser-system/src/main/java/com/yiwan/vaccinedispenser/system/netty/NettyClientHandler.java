package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Administrator
 */
@Slf4j
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private final NettyClient nettyClient;
    private final NettyReceiveCabinetService receiveService;



    /**
     * 构造方法
     */
    public NettyClientHandler(NettyClient nettyClient, NettyReceiveCabinetService receiveService){
        this.nettyClient = nettyClient;
        this.receiveService = receiveService;
    }

    /**
     * 当通道准备就绪（激活）时触发
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 设置成在线
        nettyClient.setOnline(true);
        log.info("当前的client：{} 连接服务器成功",nettyClient.getName());
    }


    /**
     * 读取数据
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = NettyUtils.decodeByteBuf(buf);
        String[] bytesStr = NettyUtils.getFormatHexStr(bytes).toUpperCase().split(" ");
        receiveService.receiveMsg(bytesStr);
        buf.release();
    }



//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (evt instanceof IdleStateEvent event) {
//            if (event.state() == IdleState.READER_IDLE) {
//                log.info("读取超时，连接断开处理逻辑");
//                // 读取超时，连接断开处理逻辑
//                ctx.close();
//                nettyClient.setOnline(false); // 设置成离线
//                nettyClient.connect();
//            } else if (event.state() == IdleState.WRITER_IDLE) {
//                // 写入超时，发送心跳消息
//                ctx.writeAndFlush(HEARTBEAT_MESSAGE.duplicate());
//            }
//        }
//    }



    /**
     * 发生异常时的处理
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        log.error("当前的client：{} 发生了异常处理",nettyClient.getName());
        // 设置成离线
        nettyClient.setOnline(false);
        log.warn("重新连接");
        ctx.close();
        // 检查是否正在连接
        if (!nettyClient.isConnecting()) {
            nettyClient.setConnecting(true);
            nettyClient.connect();
        }
    }

    /**
     * 连接断开了
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("当前的client：{} 断开了连接",nettyClient.getName());
        // 设置成离线
        nettyClient.setOnline(false);
        ctx.close();
        // 检查是否正在连接
        if (!nettyClient.isConnecting()) {
            nettyClient.setConnecting(true);
            nettyClient.connect();
        }

    }

}