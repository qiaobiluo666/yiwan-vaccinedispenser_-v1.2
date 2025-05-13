package com.yiwan.vaccinedispenser.system.camera;

import cn.hutool.core.util.HexUtil;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author 78671
 */
@Slf4j
public class CameraClient {
	@Autowired
	private WebsocketService websocketService;

	private Bootstrap bootstrap;

	private SocketChannel socketChannel;

	private String name;

	private String host;
	private int port;

	private String data;

	private NettyReceiveCameraService nettyReceiveCameraService;

	@Resource(name = "redisTemplate")
	private ValueOperations<String, String> valueOperations;


	//判断是否连接
	private boolean isConnecting = false;

	public CameraClient(String host, int port, String name,NettyReceiveCameraService nettyReceiveCameraService) {
		this.host = host;
		this.port = port;
		this.name = name;
		this.nettyReceiveCameraService = nettyReceiveCameraService;
	}



	public void start() throws Exception {
		bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup(1))
				.channel(NioSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel socketChannel) throws Exception {
						socketChannel.pipeline()
								.addLast(new StringDecoder())
								.addLast(new CameraDetaultHandler(CameraClient.this,name,nettyReceiveCameraService))
								.addLast(new StringEncoder());;
					}
				});

	}

	public void connect(){
		//启动客户端去连接服务器端
		bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
			Map<String, Object> commandData = new HashMap<>();
			String redisKey;
			if("上方扫码相机".equals(name)){
				redisKey = RedisKeyConstant.cameraStatus.ABOVE;
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_HIGH.getCode());

			}else if("下方扫码相机".equals(name)) {
				redisKey = RedisKeyConstant.cameraStatus.BELOW;
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_BELOW.getCode());
			}else {
				redisKey = RedisKeyConstant.cameraStatus.SIDE;
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_SIDE.getCode());
			}

			if (!future.isSuccess()) {
				log.warn("当前相机：{}, 执行重新连接到服务器{}:{}", name, host, port);
				valueOperations.set(redisKey,"false");
				commandData.put("data", "fail");
				future.channel().eventLoop().schedule(this::connect, 2, TimeUnit.SECONDS);
			} else {
				socketChannel = (SocketChannel) future.channel();
				log.info("当前客户端：{} 服务端连接成功...", name);
				valueOperations.set(redisKey,"true");
				commandData.put("data", "success");
				isConnecting = false;
			}
			websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
		});

	}

	//判断是正在连接
	public boolean isConnecting() {
		return isConnecting;
	}

	//设置isConnecting 状态
	public void setConnecting(boolean connecting) {
		isConnecting = connecting;
	}


	private static final String EXECUTE_COMMAND = "start";
	/**
	 * 同步任务扫码
	 */
	public void sendCommand() {
//		ByteBuf buf = NettyUtils.encodeByteBuf(HexUtil.decodeHex(EXECUTE_COMMAND));
		boolean active = socketChannel.isActive();
		if(active){
			socketChannel.writeAndFlush(EXECUTE_COMMAND);
			VacUntil.sleep(150);
		}else {
			log.warn("当前客户端：{} 已经断开了连接", name);
			if (!isConnecting()) {
				setConnecting(true);
				connect();
			}
		}

	}

}
