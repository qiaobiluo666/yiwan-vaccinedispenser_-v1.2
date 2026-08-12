package com.yiwan.vaccinedispenser.system.plc.client;

import com.yiwan.vaccinedispenser.system.plc.client.codec.ModbusDecoder;
import com.yiwan.vaccinedispenser.system.plc.client.codec.ModbusEncoder;
import com.yiwan.vaccinedispenser.system.plc.config.PlcConfig;
import com.yiwan.vaccinedispenser.system.plc.data.PlcRequestCache;
import com.yiwan.vaccinedispenser.system.plc.data.PlcStatusDispatcher;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcConnectionManager;
import com.yiwan.vaccinedispenser.system.plc.protocol.MbapHeader;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PLC TCP 客户端（Netty 实现）
 * <p>
 * 功能说明：管理与 PLC 的 TCP 长连接，提供 Modbus TCP 帧发送与同步等待响应能力。
 * 包含自动重连机制、帧号自动生成、请求-响应事务匹配。
 * <p>
 * 输入参数：通过 PlcConfig 注入 host、port、connectTimeout、reConnectDelay、maxRetry
 * 输出参数：sendAndWait() 返回 ModbusFrame 响应帧
 * 副作用：自动缓存请求起始地址与数量到 Redis（通过 PlcRequestCache）
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *   2024-05-19 yiwan - 新增 frameToHex 日志方法
 *
 * @author yiwan
 */
@Slf4j
public class PlcClient {

    /** PLC 主机地址 */
    private final String host;
    /** PLC 端口号 */
    private final int port;
    /** 连接超时时间（毫秒） */
    private final int connectTimeout;
    /** 重连延迟（毫秒） */
    private final int reConnectDelay;
    /** 最大重连次数 */
    private final int maxRetry;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private SocketChannel socketChannel;

    /** 是否在线 */
    @Getter
    private volatile boolean online = false;

    private PlcConnectionManager connectionManager;

    /** 重连中标志 */
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    /** 重连锁 */
    private final Object reconnectLock = new Object();

    /** 帧号生成器, JVM 内永不重复，从 1 递增 */
    private final AtomicInteger transactionIdGen = new AtomicInteger(1);

    /** 发送队列：所有指令串行发出 */
    private final LinkedBlockingQueue<SendTask> sendQueue = new LinkedBlockingQueue<>();
    /** 发送线程 */
    private volatile Thread sendThread;
    /** 发送线程是否运行 */
    private volatile boolean sendThreadRunning = false;
    /** 发送线程等待响应超时 10s */
    private static final long RESPONSE_TIMEOUT_MS = 3000;
    /** 指令间隔 10s（一问一答模式） */
    private static final long COMMAND_INTERVAL_MS = 100;

    /** 等待响应的请求映射: 事务标识符(TID) -> CompletableFuture */
    private final ConcurrentHashMap<Integer, CompletableFuture<ModbusFrame>> pendingRequests = new ConcurrentHashMap<>();

    /** Redis 请求缓存 */
    private PlcRequestCache requestCache;

    /** PlcStatusDispatcher 分发器 */
    private PlcStatusDispatcher plcStatusDispatcher;

    /**
     * 构造 PLC 客户端
     *
     * @param plcConfig PLC 配置对象，包含 host/port/connectTimeout/reConnectDelay/maxRetry
     */
    public PlcClient(PlcConfig plcConfig) {
        this.host = plcConfig.getHost();
        this.port = plcConfig.getPort();
        this.connectTimeout = plcConfig.getConnectTimeout();
        this.reConnectDelay = plcConfig.getReConnectDelay();
        this.maxRetry = plcConfig.getMaxRetry();
    }

    /**
     * 设置 Redis 请求缓存
     *
     * @param requestCache PlcRequestCache 实例
     */
    public void setRequestCache(PlcRequestCache requestCache) {
        this.requestCache = requestCache;
    }

    /**
     * 获取 Redis 请求缓存
     *
     * @return PlcRequestCache 实例
     */
    public PlcRequestCache getRequestCache() {
        return requestCache;
    }

    /**
     * 设置 PlcStatusDispatcher 分发器
     *
     * @param plcStatusDispatcher PlcStatusDispatcher 实例
     */
    public void setPlcStatusDispatcher(PlcStatusDispatcher plcStatusDispatcher) {
        this.plcStatusDispatcher = plcStatusDispatcher;
    }

    /**
     * 获取 PlcStatusDispatcher 分发器
     *
     * @return PlcStatusDispatcher 实例
     */
    public PlcStatusDispatcher getPlcStatusDispatcher() {
        return plcStatusDispatcher;
    }

    /**
     * 设置连接管理器
     *
     * @param connectionManager 连接管理器实例
     */
    public void setConnectionManager(PlcConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * 获取 PLC 主机地址
     *
     * @return 主机地址字符串
     */
    public String getHost() {
        return host;
    }

    /**
     * 获取 PLC 端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 初始化 Netty 客户端
     * <p>
     * 创建一个 NioEventLoopGroup 线程组, 配置 Bootstrap 并编排 Pipeline：
     * ModbusDecoder → ModbusEncoder → PlcClientHandler
     * 仅首次调用有效，重复调用不会重新初始化。
     * 副作用：创建 EventLoopGroup 和 Bootstrap 实例
     */
    public void init() {
        if (group != null) {
            return;
        }
        startSendThread();
        log.debug("[PLC] 初始化 TCP 客户端");
        group = new NioEventLoopGroup(1);

        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
                .option(io.netty.channel.ChannelOption.TCP_NODELAY, true)
                .option(io.netty.channel.ChannelOption.SO_REUSEADDR, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new ModbusDecoder())
                                .addLast(new ModbusEncoder())
                                .addLast(new PlcClientHandler(PlcClient.this));
                    }
                });
    }

    /**
     * 发起 TCP 连接
     * <p>
     * 连接成功回调: 置 online=true, 通知 connectionManager.onConnected()
     * 连接失败回调: 按 reConnectDelay 延迟自动重连
     * 输入参数：无（使用构造时传入的 host/port/connectTimeout）
     * 副作用：可能触发 scheduleReconnect()
     */
    public void connect() {
        if (online && socketChannel != null && socketChannel.isActive()) {
            return;
        }

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                String errorDetail = cause != null ? cause.getMessage() : "未知错误";
                String errorType = cause != null ? cause.getClass().getSimpleName() : "Unknown";
                log.warn("[PLC] 连接失败 {}:{} - {}: {}", host, port, errorType, errorDetail);
                scheduleReconnect("连接失败");
            } else {
                socketChannel = (SocketChannel) future.channel();
                online = true;
                resetReconnectState();
                log.info("[PLC] 连接成功 {}:{}", host, port);
                if (connectionManager != null) {
                    connectionManager.onConnected();
                }
            }
        });
    }

    /**
     * 断开 TCP 连接
     * <p>
     * 清理资源: 关闭 socketChannel、shutdown EventLoopGroup
     * 通知 connectionManager.onDisconnected()
     * 副作用：重置重连状态
     */
    public void disconnect() {
        resetReconnectState();
        online = false;
        stopSendThread();
        if (socketChannel != null) {
            if (socketChannel.isActive()) {
                socketChannel.close();
            }
            socketChannel = null;
        }
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
        log.info("[PLC] 已断开连接 {}:{}", host, port);
        if (connectionManager != null) {
            connectionManager.onDisconnected();
        }
    }

    /**
     * 调度异步重连
     * <p>
     * 使用 group.schedule() 在 reConnectDelay 毫秒后执行 connect()
     * 同一时间仅允许一个重连任务运行 (AtomicBoolean reconnecting 控制)
     * 输入参数：reason - 触发重连的原因描述
     * 有效范围：reason 任意非空字符串
     * 副作用：修改 reconnecting 标志
     *
     * @param reason 重连原因（用于日志）
     */
    public void scheduleReconnect(String reason) {
        synchronized (reconnectLock) {
            if (online && socketChannel != null && socketChannel.isActive()) {
                resetReconnectState();
                return;
            }

            if (reconnecting.get()) {
                return;
            }

            if (group == null) {
                log.warn("[PLC] 重连取消，客户端已关闭");
                return;
            }

            reconnecting.set(true);

            group.schedule(() -> {
                try {
                    reconnecting.set(false);
                    connect();
                } catch (Exception e) {
                    log.error("[PLC] 重连异常: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
                    resetReconnectState();
                    scheduleReconnect("重连异常");
                }
            }, reConnectDelay, TimeUnit.MILLISECONDS);

            log.debug("[PLC] 已调度重连，{}ms 后执行", reConnectDelay);
        }
    }

    /** 重置重连状态 */
    private void resetReconnectState() {
        synchronized (reconnectLock) {
            reconnecting.set(false);
        }
    }

    /**
     * 发送 Modbus 帧并同步等待响应（一问一答）
     * <p>
     * 自动生成事务标识符(TID) → 解析帧数据中的起始地址与寄存器数量
     * → 缓存到 Redis → 入队由发送线程串行发出 → 阻塞等待 CompletableFuture.get(timeout, unit)
     * <p>
     * 发送线程已经保证了一问一答 + 10s 间隔，本方法只负责阻塞调用方直到收到响应或超时。
     *
     * @param frame   请求帧 (data[0..1]=起始地址, data[2..3]=寄存器数量)
     * @param timeout 超时时间 (建议 3000ms)
     * @param unit    时间单位
     * @return 响应帧，超时或连接不可用时返回 null
     */
    public ModbusFrame sendAndWait(ModbusFrame frame, long timeout, TimeUnit unit) {
        if (!isActive()) {
            log.warn("[PLC] 连接不可用");
            return null;
        }
        int transactionId = transactionIdGen.getAndIncrement() & 0xFFFF;
        frame.getHeader().setTransactionId(transactionId);

        byte[] reqData = frame.getData();
        int startAddr = 0;
        int registerCount = 0;
        if (reqData != null && reqData.length >= 4 && requestCache != null) {
            startAddr = ((reqData[0] & 0xFF) << 8) | (reqData[1] & 0xFF);
            registerCount = ((reqData[2] & 0xFF) << 8) | (reqData[3] & 0xFF);
            requestCache.save(transactionId, startAddr, registerCount);
        }

        CompletableFuture<ModbusFrame> future = new CompletableFuture<>();
        pendingRequests.put(transactionId, future);

        String logInfo = String.format("[PLC] 发送指令 | TID=%d(0x%x) | 起始地址=%d(0x%x) | 数量=%d(0x%x) | %s",
                transactionId, transactionId, startAddr, startAddr, registerCount, registerCount,
                frameToHex(frame, transactionId));

        sendQueue.offer(new SendTask(frame, future, logInfo));

        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            log.warn("[PLC] 响应超时 | TID={}", transactionId);
            return null;
        } catch (Exception e) {
            log.error("[PLC] 请求异常 | TID={} | {}", transactionId, e.getMessage(), e);
            return null;
        } finally {
            pendingRequests.remove(transactionId);
        }
    }

    /**
     * 异步发送 Modbus 帧（发送线程会等待响应，调用方不阻塞）
     * <p>
     * 适用于写指令（功能码 0x06/0x10/0x05/0x0F），
     * 入队后由发送线程串行发出并等待 PLC 响应。
     *
     * @param frame 待发送的 ModbusFrame
     */
    public void sendAsync(ModbusFrame frame) {
        if (!isActive()) {
            log.warn("[PLC] 连接不可用，无法异步发送");
            return;
        }
        int transactionId = transactionIdGen.getAndIncrement() & 0xFFFF;
        frame.getHeader().setTransactionId(transactionId);

        CompletableFuture<ModbusFrame> future = new CompletableFuture<>();
        pendingRequests.put(transactionId, future);

        String logInfo = String.format("[PLC] 异步发送 | TID=%d(0x%x) | %s",
                transactionId, transactionId, frameToHex(frame, transactionId));

        sendQueue.offer(new SendTask(frame, future, logInfo));
    }

    /**
     * 完成等待中的请求（由 PlcClientHandler 在收到响应时调用）
     * <p>
     * 通过事务标识符查找对应的 CompletableFuture 并 complete
     * 输入参数：
     *   transactionId - 事务标识符（帧号）
     *   response - PLC 响应帧
     *
     * @param transactionId 事务标识符
     * @param response      响应帧
     */
    public void completePendingRequest(int transactionId, ModbusFrame response) {
        CompletableFuture<ModbusFrame> future = pendingRequests.remove(transactionId);
        if (future != null) {
            future.complete(response);
        }
    }

    /** 通知客户端已连接 */
    public void onConnected() {
        this.online = true;
    }

    /** 通知客户端已断开（由 PlcClientHandler.channelInactive 调用） */
    public void onDisconnected() {
        this.online = false;
        if (connectionManager != null) {
            connectionManager.onDisconnected();
        }
    }

    /**
     * 检查连接是否可用
     *
     * @return true 表示在线且通道活跃
     */
    public boolean isActive() {
        return online && socketChannel != null && socketChannel.isActive();
    }

    /**
     * 获取 Netty SocketChannel
     *
     * @return SocketChannel 实例
     */
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * 启动发送线程（一问一答模式）
     * <p>
     * 单线程从队列取任务，串行写入 SocketChannel，每发一条指令都等待 PLC 响应，
     * 收到响应后间隔 10s 再发下一条指令。未收到响应前队列阻塞等待。
     */
    private void startSendThread() {
        if (sendThreadRunning) {
            return;
        }
        sendThreadRunning = true;
        sendThread = new Thread(() -> {
            log.info("[PLC] 发送线程已启动（一问一答模式, 间隔10s）");
            while (sendThreadRunning) {
                try {
                    SendTask task = sendQueue.take();
                    if (socketChannel != null && socketChannel.isActive()) {
                        socketChannel.writeAndFlush(task.frame);
                        log.info(task.logInfo);

                        // 一问一答：等待 PLC 响应（没收到回答前队列等待）
                        if (task.future != null) {
                            try {
                                task.future.get(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                            } catch (TimeoutException e) {
                                log.warn("[PLC] 响应超时 | TID={}, 继续下一指令",
                                        task.frame.getHeader().getTransactionId());
                            } catch (Exception e) {
                                log.error("[PLC] 响应异常 | TID={} | {}",
                                        task.frame.getHeader().getTransactionId(),
                                        e.getMessage(), e);
                            }
                        }

                        // 间隔 10s 再发下一条
                        if (sendThreadRunning) {
                            Thread.sleep(COMMAND_INTERVAL_MS);
                        }
                    } else if (task.future != null) {
                        task.future.completeExceptionally(new Exception("PLC连接不可用"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[PLC] 发送线程异常", e);
                }
            }
            log.info("[PLC] 发送线程已停止");
        }, "t_plc_send");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /**
     * 停止发送线程
     */
    public void stopSendThread() {
        sendThreadRunning = false;
        if (sendThread != null) {
            sendThread.interrupt();
            sendThread = null;
        }
        // 清空队列，异常完成所有等待的请求
        SendTask task;
        while ((task = sendQueue.poll()) != null) {
            if (task.future != null) {
                task.future.completeExceptionally(new Exception("PLC发送已停止"));
            }
        }
    }

    /**
     * 发送任务（内部类）
     */
    private static class SendTask {
        final ModbusFrame frame;
        final CompletableFuture<ModbusFrame> future; // 由发送线程等待响应完成
        final String logInfo;

        SendTask(ModbusFrame frame, CompletableFuture<ModbusFrame> future, String logInfo) {
            this.frame = frame;
            this.future = future;
            this.logInfo = logInfo;
        }
    }

    /**
     * 将 ModbusFrame 编码为 Hex 字符串（用于日志输出）
     * <p>
     * 格式: TID(2B) PID(2B) Length(2B) UID(1B) FC(1B) Data(NB)
     * 输入参数：
     *   frame - ModbusFrame 实例
     *   transactionId - 事务标识符
     *
     * @param frame         待编码的帧
     * @param transactionId 事务标识符
     * @return 空格分隔的 Hex 字符串，如 "00 01 00 00 00 06 01 03 01 1A 00 19"
     */
    private String frameToHex(ModbusFrame frame, int transactionId) {
        MbapHeader header = frame.getHeader();
        int pduLength = 1 + (frame.getData() != null ? frame.getData().length : 0);
        StringBuilder sb = new StringBuilder();
        byte[] data = frame.getData();
        int length = pduLength + 1;
        appendHex(sb, (transactionId >> 8) & 0xFF);
        appendHex(sb, transactionId & 0xFF);
        appendHex(sb, 0);
        appendHex(sb, 0);
        appendHex(sb, (length >> 8) & 0xFF);
        appendHex(sb, length & 0xFF);
        appendHex(sb, header.getUnitId());
        appendHex(sb, frame.getFunctionCode());
        if (data != null) {
            for (byte b : data) {
                appendHex(sb, b & 0xFF);
            }
        }
        return sb.toString();
    }

    /**
     * 向 StringBuilder 追加一个字节的 Hex 值
     *
     * @param sb    StringBuilder
     * @param value 字节值 (0~255)
     */
    private void appendHex(StringBuilder sb, int value) {
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(String.format("%02X", value));
    }
}
