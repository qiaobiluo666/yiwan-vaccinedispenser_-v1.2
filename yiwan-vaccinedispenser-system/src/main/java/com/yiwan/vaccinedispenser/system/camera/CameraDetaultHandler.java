package com.yiwan.vaccinedispenser.system.camera;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.netty.NettyClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author 78671
 */
@Slf4j
//@Component
public class CameraDetaultHandler extends SimpleChannelInboundHandler<String> {

	private CameraClient cameraClient;
	NettyReceiveCameraService nettyReceiveCameraService;
	private String name;
	public CameraDetaultHandler(CameraClient cameraClient,String name,NettyReceiveCameraService nettyReceiveCameraService){
		this.cameraClient =cameraClient;
		this.nettyReceiveCameraService = nettyReceiveCameraService;
		this.name = name;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String remoteAddress = ctx.channel() != null && ctx.channel().remoteAddress() != null 
			? ctx.channel().remoteAddress().toString() : "unknown";
		log.warn("[{}] 连接断开 - 远程地址: {}", name, remoteAddress);
		super.channelInactive(ctx);
		if (!cameraClient.isConnecting()) {
			cameraClient.setConnecting(true);
			cameraClient.connect();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// 获取详细的错误信息
		String errorType = cause.getClass().getSimpleName();
		String errorMsg = cause.getMessage();
		String remoteAddress = ctx.channel() != null && ctx.channel().remoteAddress() != null 
			? ctx.channel().remoteAddress().toString() : "unknown";
		
		if (cause instanceof java.io.IOException) {
			log.warn("[{}] IO异常断开 - {}: {} - 远程地址: {}", 
				name, errorType, errorMsg, remoteAddress);
		} else {
			log.error("[{}] 异常断开 - {}: {} - 远程地址: {}", 
				name, errorType, errorMsg, remoteAddress, cause);
		}
		
		ctx.channel().close();
		if (!cameraClient.isConnecting()) {
			cameraClient.setConnecting(true);
			cameraClient.connect();
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//		log.info("{}收到返回值:{}",name,msg);
		msg = msg.replace("\r\n", msg);
		nettyReceiveCameraService.receiveMsg(msg);

	}


}
