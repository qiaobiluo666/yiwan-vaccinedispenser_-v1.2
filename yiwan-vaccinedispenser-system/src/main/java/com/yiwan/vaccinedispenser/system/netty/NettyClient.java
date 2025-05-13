package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;



/**
 * @author 78671
 */
@Slf4j
public final class NettyClient {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private WebsocketService websocketService;

    private String host;
    private int port;
    private String name;
    private Bootstrap bootstrap;
    private EventLoopGroup group;

    // 客户端是否在线true:在线，false:离线
    private boolean online = false;

    public boolean isOnline() {
        return online;
    }
    public void setOnline(boolean online) {
        this.online = online;
    }

    private SocketChannel socketChannel;

    private NettyReceiveCabinetService nettyReceiveService;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    //判断是否连接
    private boolean isConnecting = false;

    public NettyClient(String host, int port, String name, NettyReceiveCabinetService nettyReceiveService) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.nettyReceiveService = nettyReceiveService;
    }

    public String getName() {
        return name;
    }

    public void init() {
        //客户端需要一个事件循环组
        log.info("当前客户端：{}, 进行初始化", name);
        group = new NioEventLoopGroup();
        //创建客户端启动对象
        // bootstrap 可重用, 只需在NettyClient实例化的时候初始化即可.
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //加入处理器
                        ch.pipeline()
//                        .addLast(new IdleStateHandler(0,0,10, TimeUnit.SECONDS))
                        .addLast(new MyProtobufPrepender())
                        .addLast(new NettyClientHandler(NettyClient.this, nettyReceiveService));
                    }
                });
    }

    public void connect() {
        //启动客户端去连接服务器端
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            Map<String, Object> commandData = new HashMap<>();
            String redisKey;
            if("A".equals(name)){
                redisKey = RedisKeyConstant.controlStatus.CABINET_A;
                commandData.put("code", CommandEnums.DEVICE_STATUS_A_CONTR.getCode());
            }else if("B".equals(name)) {
                redisKey =RedisKeyConstant.controlStatus.CABINET_B;
                commandData.put("code", CommandEnums.DEVICE_STATUS_B_CONTR.getCode());
            }else {
                redisKey = RedisKeyConstant.controlStatus.CABINET_C;
                commandData.put("code", CommandEnums.DEVICE_STATUS_C_CONTR.getCode());
            }

            if (!future.isSuccess()) {
                log.warn("当前客户端：{}, 执行重新连接到服务器{}:{}", name, host, port);
                valueOperations.set(redisKey,"false");
                commandData.put("data", "fail");
                future.channel().eventLoop().schedule(this::connect, 2, TimeUnit.SECONDS);
            } else {
                socketChannel = (SocketChannel) future.channel();
                log.info("当前客户端：{} 服务端连接成功...", name);
                valueOperations.set(redisKey,"true");
                commandData.put("data", "sucess");
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



    /**
     * 仅仅是发送命令
     * @param msg 需要发送数组
     */
    public void send(byte[] msg) {
        ByteBuf buf = NettyUtils.encodeByteBuf(msg);
        // 判断通道是否正常
        if (socketChannel.isActive()) {
            socketChannel.writeAndFlush(buf);
        } else {
            log.warn("当前客户端：{} 已经断开了连接", name);
        }
    }


    /**
     * 发送数据
     *
     * @param msg
     */
    public Result send(byte[] msg, int frame, CabinetConstants.Cabinet cabinetType) {
        ByteBuf buf = NettyUtils.encodeByteBuf(msg);
        if(socketChannel!=null){
            boolean active = socketChannel.isActive();
            //判断是否接收到指令
            boolean flag = false;
            boolean isReceMsg = false;
            int count = 0;
            if (active) {
                socketChannel.writeAndFlush(buf);

//            long timeout = System.currentTimeMillis();
//            //判断命令是否收到00 如果1秒内没有收到00 发命令
//            while ((System.currentTimeMillis() - timeout) < SettingConstants.COMMAND_WAIT_TIMEOUT) {
//                if(boxType.equals(BoxEmunConstants.BoxType.BOX_A)){
//                    isReceMsg = "true".equals(valueOperations.get(RedisConstants.CABINET_A_RECEIVE_MSG));
//                } else if (boxType.equals(BoxEmunConstants.BoxType.BOX_B)) {
//                    isReceMsg = "true".equals(valueOperations.get(RedisConstants.CABINET_B_RECEIVE_MSG));
//
//                } else if (boxType.equals(BoxEmunConstants.BoxType.BOX_C)) {
//                    isReceMsg = "true".equals(valueOperations.get(RedisConstants.CABINET_C_RECEIVE_MSG));
//                }
//
//                if(isReceMsg){
//                    break;
//                }
//                CommenUtil.sleep(20);
//            }

//            String typeKey = String.format(RedisConstants.FRAME_NUMBER_COMMAND, frame);
//            valueOperations.set(typeKey, "false", 30, TimeUnit.SECONDS);
//            socketChannel.writeAndFlush(buf);
//            //将A、B、C状态先重置
//            if(boxType.equals(BoxEmunConstants.BoxType.BOX_A)){
//                valueOperations.set(RedisConstants.CABINET_A_RECEIVE_MSG, "false", 30, TimeUnit.SECONDS);
//            } else if (boxType.equals(BoxEmunConstants.BoxType.BOX_B)) {
//                valueOperations.set(RedisConstants.CABINET_B_RECEIVE_MSG, "false", 30, TimeUnit.SECONDS);
//
//            } else if (boxType.equals(BoxEmunConstants.BoxType.BOX_C)) {
//                valueOperations.set(RedisConstants.CABINET_C_RECEIVE_MSG, "false", 30, TimeUnit.SECONDS);



//            //命令没有收到00重发
//            timeout = System.currentTimeMillis();
//            while ((System.currentTimeMillis() - timeout) < SettingConstants.COMMAND_WAIT_TIMEOUT) {
//                if (Objects.equals(valueOperations.get(typeKey), "true")) {
//                    flag = true;
//                    break;
//                }
//                CommenUtil.sleep(200);
//            }
//            if (!flag) {
//                while (count < COMMAND_WAIT_TIMES ) {
//                    count = retrySend(msg, count);
//                    CommenUtil.sleep(20);
//                }
//            }
//
//            if(count==COMMAND_WAIT_TIMES){
//                //指令超时写入数据库
//                ageCabinetExceptionService.timeoutException(msg,boxType);
//                log.error("超时发送{}次失败，不再重发",COMMAND_WAIT_TIMES+1);
//                return Result.failure(ErrorCode.CONTROL_BOARD_INSTRUCT_MISSING);
//            }
                return Result.success();
            } else{
                log.warn("当前客户端：{} 已经断开了连接", name);
                if (!isConnecting()) {
                    setConnecting(true);
                    connect();
                }
                return Result.failure(ErrorCode.CONTROL_BOARD_DISCONNECT);
            }
        }
        return Result.success();
    }


//    /**
//     * 发送数据
//     *
//     * @param msg
//     */
//    public int retrySend(byte[] msg, int count) {
//        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
//        String x1 = HexUtils.reverse(HexUtils.toHex(frameNumberNow,4)).substring(0,2);
//        String x2 = HexUtils.reverse(HexUtils.toHex(frameNumberNow,4)).substring(2,4);
//        msg[4] =Integer.valueOf(x1, 16).byteValue();
//        msg[5] =Integer.valueOf(x2, 16).byteValue();
//        ByteBuf buf = HexUtils.encodeByteBuf(msg);
//        buf.retain();
//        boolean active = socketChannel.isActive();
//        //判断是否接收到指令
//        if (active) {
//            log.error("发送{}命令超时！第{}次重发命令！命令为：{}", name,  count+1, HexUtils.getFormatHexStr(msg));
//            String typeKey = String.format(RedisConstants.FRAME_NUMBER_COMMAND, frameNumberNow);
//            valueOperations.set(typeKey, "false", 30, TimeUnit.SECONDS);
//            socketChannel.writeAndFlush(buf);
//            long timeout = System.currentTimeMillis();
//            while (System.currentTimeMillis() - timeout < SettingConstants.COMMAND_WAIT_TIMEOUT) {
//                if (Objects.equals(valueOperations.get(typeKey), "true")) {
//                    count = COMMAND_WAIT_TIMES + 1;
//                    return count;
//                }
//            }
//
//            count++;
//            return count;
//
//        }
//        else {
//            log.info("当前客户端：{} 已经断开了连接", name);
//            count++;
//            return count;
//        }
//    }





}