package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineException;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class NettyClient {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;

    private String host;
    private int port;
    @Getter
    private String name;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private SocketChannel socketChannel;
    private NettyReceiveCabinetService nettyReceiveService;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Setter
    private volatile boolean online = false;

    // 重连间隔时间（秒）
    private static final int RECONNECT_INTERVAL_SECONDS = 10;

    /** ================= 重连管理相关 ================ */
    // 重连状态标记，防止重复重连
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    // 当前重连任务，用于取消重复的重连请求
    private volatile ScheduledFuture<?> reconnectTask = null;
    // 重连任务锁，确保重连逻辑的线程安全
    private final Object reconnectLock = new Object();

    /** ================= 队列发送相关 ================ */
    private final ConcurrentLinkedQueue<SendCommand> sendQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService sendScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean schedulerStarted = new AtomicBoolean(false);
    // 每 200ms 发送一次
    private final int sendIntervalMs = 200;

    public NettyClient(String host, int port, String name, NettyReceiveCabinetService nettyReceiveService) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.nettyReceiveService = nettyReceiveService;
    }

    public void init() {
        log.debug("[{}] 初始化", name);
        group = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                // 连接超时时间增加到 5 秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // 启用 TCP KeepAlive，保持连接活跃
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 启用 TCP_NODELAY，减少延迟
                .option(ChannelOption.TCP_NODELAY, true)
                // SO_REUSEADDR 允许重用地址
                .option(ChannelOption.SO_REUSEADDR, true)
                // 设置接收缓冲区大小
                .option(ChannelOption.SO_RCVBUF, 65536)
                // 设置发送缓冲区大小
                .option(ChannelOption.SO_SNDBUF, 65536)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new MyProtobufPrepender())
                                .addLast(new NettyClientHandler(NettyClient.this, nettyReceiveService, sendDrugThreadManager));
                    }
                });
    }

    public void connect() {
        // 检查是否已经在线，避免重复连接
        if (online && socketChannel != null && socketChannel.isActive()) {
            return;
        }

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            Map<String, Object> commandData = new HashMap<>();
            String redisKey;

            if ("A".equals(name)) {
                redisKey = RedisKeyConstant.controlStatus.CABINET_A;
                commandData.put("code", CommandEnums.DEVICE_STATUS_A_CONTR.getCode());
            } else if ("B".equals(name)) {
                redisKey = RedisKeyConstant.controlStatus.CABINET_B;
                commandData.put("code", CommandEnums.DEVICE_STATUS_B_CONTR.getCode());
            } else {
                redisKey = RedisKeyConstant.controlStatus.CABINET_C;
                commandData.put("code", CommandEnums.DEVICE_STATUS_C_CONTR.getCode());
            }

            if (!future.isSuccess()) {
                // 获取详细的错误信息
                Throwable cause = future.cause();
                String errorDetail = cause != null ? cause.getMessage() : "未知错误";
                String errorType = cause != null ? cause.getClass().getSimpleName() : "Unknown";
                
                log.warn("[{}] 连接失败 {}:{} - {}: {}", name, host, port, errorType, errorDetail);
                
                String errorMsg = String.format("当前客户端：%s 连接失败 %s:%s - %s", name, host, port, errorDetail);

                if (vacMachineExceptionService.getExceptionByName(name).isEmpty()) {

                    vacMachineExceptionService.sendException(SettingConstants.MachineException.CONTROLLER.code, name, errorMsg);

                }

                valueOperations.set(redisKey, "false");
                commandData.put("data", "fail");

                // 连接失败后，使用统一的重连方法调度下一次重连
                scheduleReconnect("连接失败");

            } else {
                socketChannel = (SocketChannel) future.channel();
                online = true;
                // 连接成功，重置重连标记
                resetReconnectState();
                log.info("[{}] 连接成功 {}:{}", name, host, port);

                List<VacMachineException> exceptions = vacMachineExceptionService.getExceptionByName(name);
                if (!exceptions.isEmpty()) {
                    vacMachineExceptionService.delExceptionByName(exceptions);
                }

                valueOperations.set(redisKey, "true");
                commandData.put("data", "success");

            }

            websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(), commandData);
        });
    }

    /**
     * 统一的重连调度入口（企业级实现，防止重复重连）
     * @param reason 重连原因，用于日志记录
     */
    public void scheduleReconnect(String reason) {
        synchronized (reconnectLock) {
            // 如果已经在线，不需要重连
            if (online && socketChannel != null && socketChannel.isActive()) {
                resetReconnectState();
                return;
            }

            // 如果已经在重连中，取消之前的重连任务
            if (reconnecting.get()) {
                if (reconnectTask != null && !reconnectTask.isDone()) {
                    reconnectTask.cancel(false);
                }
                // 重置重连状态，以便重新调度
                reconnecting.set(false);
                reconnectTask = null;
            }

            // 设置重连标记
            reconnecting.set(true);

            // 使用 EventLoopGroup 调度重连任务
            reconnectTask = group.schedule(() -> {
                try {
                    // 重置重连标记，允许下次重连
                    reconnecting.set(false);
                    connect();
                } catch (Exception e) {
                    log.error("[{}] 重连异常: {} - {}", name, e.getClass().getSimpleName(), e.getMessage(), e);
                    // 重连异常后，重置状态并重新调度
                    resetReconnectState();
                    scheduleReconnect("重连异常");
                }
            }, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);

            log.debug("[{}] 已调度重连，{}秒后执行", name, RECONNECT_INTERVAL_SECONDS);
        }
    }

    /**
     * 重置重连状态（连接成功或需要取消重连时调用）
     */
    private void resetReconnectState() {
        synchronized (reconnectLock) {
            reconnecting.set(false);
            if (reconnectTask != null && !reconnectTask.isDone()) {
                reconnectTask.cancel(false);
            }
            reconnectTask = null;
        }
    }

    /** 启动定时发送任务 */
    private void startSendScheduler() {
        if (schedulerStarted.compareAndSet(false, true)) {
            sendScheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!online) {
                        return;
                    }
                    SendCommand cmd = sendQueue.poll();
                    if (cmd != null) {
                        ByteBuf buf = NettyUtils.encodeByteBuf(cmd.data);
                        try {
                            if (socketChannel != null && socketChannel.isActive() && socketChannel.isWritable()) {
                                socketChannel.writeAndFlush(buf);
                            } else {
                                // 连接不可用，重新入队
                                sendQueue.offer(cmd);
                                buf.release(); // 释放资源
                                
                                // 如果连接断开，触发重连
                                if (socketChannel == null || !socketChannel.isActive()) {
                                    onDisconnected();
                                }
                            }
                        } catch (Exception e) {
                            buf.release(); // 确保释放资源
                            sendQueue.offer(cmd); // 重新入队
                            log.error("[{}] 发送异常: {} - {}", name, e.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("[{}] 发送调度异常: {} - {}", name, e.getClass().getSimpleName(), e.getMessage());
                }
            }, 0, sendIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /** 入队发送（业务层调用这个方法） */
    public Result send(byte[] msg, int frame, CabinetConstants.Cabinet cabinetType) {
        if (!online) {
            return Result.fail( "连接未建立，无法发送消息");
        }
        sendQueue.offer(new SendCommand(msg, frame, cabinetType));
        return Result.success();
    }

    /** 连接成功时调用 */
    public void onConnected() {
        setOnline(true);
        resetReconnectState(); // 连接成功，重置重连状态
        startSendScheduler();
    }

    /** 掉线处理（Handler 调用） */
    public void onDisconnected() {
        this.online = false;
        sendQueue.clear();
        // 注意：这里不触发重连，重连由 channelInactive 或 exceptionCaught 统一处理
    }

    /** 内部封装的命令对象 */
    private static class SendCommand {
        final byte[] data;
        final int frame;
        final CabinetConstants.Cabinet cabinetType;

        SendCommand(byte[] data, int frame, CabinetConstants.Cabinet cabinetType) {
            this.data = data;
            this.frame = frame;
            this.cabinetType = cabinetType;
        }
    }
}
