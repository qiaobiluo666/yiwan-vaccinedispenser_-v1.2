package com.yiwan.vaccinedispenser.system.camera;

import cn.hutool.core.util.HexUtil;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
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
import java.util.List;
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

	private volatile SocketChannel socketChannel;

	private String name;

	private String host;
	private Integer port;

	private String data;

	@Autowired
	private VacMachineExceptionService vacMachineExceptionService;

	private NettyReceiveCameraService nettyReceiveCameraService;

	@Resource(name = "redisTemplate")
	private ValueOperations<String, String> valueOperations;


	//判断是否连接
	private volatile boolean isConnecting = false;

	public CameraClient(String host, Integer port, String name,NettyReceiveCameraService nettyReceiveCameraService) {
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
		SocketChannel currentChannel = socketChannel;
		if (currentChannel != null && currentChannel.isActive()) {
			isConnecting = false;
			return;
		}
		isConnecting = true;

		String redisKey;
		if("上方扫码相机".equals(name)){
			redisKey = RedisKeyConstant.cameraStatus.ABOVE;
		}else if("下方扫码相机".equals(name)) {
			redisKey = RedisKeyConstant.cameraStatus.BELOW;
		}else {
			redisKey = RedisKeyConstant.cameraStatus.SIDE;
		}
		// 发起连接时先标离线，防止Redis残留true误放行自动上药。
		try {
			if (valueOperations != null) {
				valueOperations.set(redisKey, "false");
			}
		} catch (Exception redisError) {
			log.error("[{}] 连接前更新相机离线状态失败", name, redisError);
		}
		log.info("[{}] 开始连接 {}:{}", name, host, port);

		//启动客户端去连接服务器端
		bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
			Map<String, Object> commandData = new HashMap<>();
			if("上方扫码相机".equals(name)){
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_HIGH.getCode());

			}else if("下方扫码相机".equals(name)) {
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_BELOW.getCode());
			}else {
				commandData.put("code", CommandEnums.DEVICE_STATUS_B_SCAN_SIDE.getCode());
			}

			if (!future.isSuccess()) {

				// 获取详细的错误信息
				Throwable cause = future.cause();
				String errorDetail = cause != null ? cause.getMessage() : "未知错误";
				String errorType = cause != null ? cause.getClass().getSimpleName() : "Unknown";
				
				log.warn("[{}] 连接失败 {}:{} - {}: {}", name, host, port, errorType, errorDetail);

				commandData.put("data", "fail");

				// 重连是主流程，必须先安排；报警、Redis 或前端通知失败都不能打断它。
				future.channel().eventLoop().schedule(this::connect, 10, TimeUnit.SECONDS);

				try {
					if (vacMachineExceptionService != null
							&& vacMachineExceptionService.getExceptionByName(name).isEmpty()) {
						vacMachineExceptionService.sendException(
								SettingConstants.MachineException.CONTROLLER.code, name, errorDetail);
					}
				} catch (Exception alarmError) {
					log.error("[{}] 连接失败报警写入异常，不影响10秒后自动重连", name, alarmError);
				}
				try {
					if (valueOperations != null) {
						valueOperations.set(redisKey,"false");
					}
				} catch (Exception redisError) {
					log.error("[{}] 更新相机离线状态失败，不影响自动重连", name, redisError);
				}
			} else {
				socketChannel = (SocketChannel) future.channel();
				isConnecting = false;
				log.info("[{}] 连接成功 {}:{}", name, host, port);


				if (vacMachineExceptionService != null) {
					// 延迟300ms再执行，足够等数据库初始化完成
					future.channel().eventLoop().schedule(() -> {
						try {
							List<VacMachineException> exceptions = vacMachineExceptionService.getExceptionByName(name);
							if (!exceptions.isEmpty()) {
								vacMachineExceptionService.delExceptionByName(exceptions);
							}
						} catch (Exception e) {
							log.error("[{}] 清除设备异常记录失败", name, e);
						}
					}, 300, TimeUnit.MILLISECONDS);
				}

				commandData.put("data", "success");
				try {
					if (valueOperations != null) {
						valueOperations.set(redisKey,"true");
					}
				} catch (Exception redisError) {
					log.error("[{}] 更新相机在线状态失败，TCP连接已成功", name, redisError);
				}
			}
			try {
				if (websocketService != null) {
					websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
				}
			} catch (Exception websocketError) {
				log.error("[{}] 推送相机连接状态失败，不影响连接和自动重连", name, websocketError);
			}
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
	public boolean sendCommand() {
//		ByteBuf buf = NettyUtils.encodeByteBuf(HexUtil.decodeHex(EXECUTE_COMMAND));
		SocketChannel currentChannel = socketChannel;
		if(currentChannel == null){
			// 首次连接或定时重连尚未成功，不叠加发起新连接。
			log.warn("[{}] 连接尚未建立，无法发送命令", name);
			return false;
		}
		if(currentChannel.isActive()){
			currentChannel.writeAndFlush(EXECUTE_COMMAND);
			VacUntil.sleep(150);
			return true;
		}else {
			log.warn("[{}] 连接断开，无法发送命令", name);
			if (!isConnecting()) {
				setConnecting(true);
				connect();
			}
			return false;
		}
	}

}
