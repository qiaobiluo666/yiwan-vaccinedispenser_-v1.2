package com.yiwan.vaccinedispenser.core.websocket.utils;


import cn.hutool.extra.spring.SpringUtil;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.core.websocket.impl.WebsocketServiceImpl;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import javax.websocket.server.PathParam;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@ServerEndpoint(path = "/websocket/{pageId}",port = "${websocket.port}")
public class WebSocketUtils {

    WebsocketService websocketService = SpringUtil.getBean(WebsocketServiceImpl.class);

    Lock lock = new ReentrantLock();

    /**静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。*/
    private static int onlineCount = 0;
    /**concurrent包的线程安全集合，也可以map改成set，用来存放每个客户端对应的MyWebSocket对象。*/
    // concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static ConcurrentHashMap<Integer, CopyOnWriteArraySet<WebSocketUtils>> webSocketMap = new ConcurrentHashMap<>();
    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;
    /**接收userId*/
    private Integer pageId=0;



    private static final Map<Session, Long> lastHeartbeat = new ConcurrentHashMap<>();

    /**
     *建立ws连接前的配置
     */
//   @BeforeHandshake
//    public void handshake(Session session, HttpHeaders headers, @RequestParam String req, @RequestParam MultiValueMap reqMap, @PathVariable String arg, @PathVariable MappingChange.Map pathMap){
//        //采用stomp子协议
//        session.setSubprotocols("stomp");
//        if (!"ok".equals(req)){
//            System.out.println("Authentication failed!");
//            session.close();
//        }
//    }

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @RequestParam String req, @RequestParam MultiValueMap reqMap, @PathVariable int pageId/*, @PathVariable MappingChange.Map pathMap*/){
        this.session = session;
        this.pageId=pageId;
        lastHeartbeat.put(session, System.currentTimeMillis());
        CopyOnWriteArraySet<WebSocketUtils> set = null;


        // 每次创建必须是单线程，防止创建多个
        lock.lock();
        try {
            set = webSocketMap.get(pageId);
            if(set == null){
                set = new CopyOnWriteArraySet<>();
                webSocketMap.put(pageId,set);
            }
        } catch (Exception e) {

        }finally {
            lock.unlock();
        }


        if(!set.contains(this)){
            //加入集合中
            set.add(this);
        }

        log.info("页面id:"+pageId+",当前在线人数为:" + set.size());

        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("页面id:"+pageId+",网络异常!!!!!!");
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        CopyOnWriteArraySet<WebSocketUtils> set = webSocketMap.get(pageId);
        if(set.contains(this)){
            set.remove(this);
            //从集合中删除
        }
//        log.info("页面id:"+pageId+",当前在线人数为:" + set.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
//        log.error("页面id:" + this.pageId + "错误,原因:" + throwable.getMessage());
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message,  @PathVariable int pageId) {
//        log.info("【websocket消息】收到客户端发来的消息:{}", message);
        websocketService.processMessage(pageId, session, message);
        if ("undefined".equals(message)) {
            lastHeartbeat.put(session, System.currentTimeMillis());
            // 发送心跳响应
            session.sendText("undefined");

            // 响应心跳
        }

//        {"toUserId":"10","contentText":"hello websocket"}
//        JSONObject jsonObject = JSONObject.parseObject(message);
//        String toUserId = jsonObject.getString("toUserId");
//        String contentText = jsonObject.getString("contentText");
//
//        try {
//            sendInfo(contentText,toUserId);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }




    @OnBinary
    public void onBinary(Session session, byte[] bytes) {
        for (byte b : bytes) {
            System.out.println(b);
        }
        session.sendBinary(bytes);
    }

    @OnEvent
    public void onEvent(Session session, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    System.out.println("read idle");
                    break;
                case WRITER_IDLE:
                    System.out.println("write idle");
                    break;
                case ALL_IDLE:
                    System.out.println("all idle");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.sendText(JSON.toJSONString(message));
    }

    public void sendMessage(Map<String,Object> message) throws IOException {
        this.session.sendText(JSON.toJSONString(message));
    }


    /**
     * 发送自定义消息
     * */
    public static void sendInfo(String message,@PathParam("pageId") int pageId) throws IOException {
//        log.info("发送消息到:"+pageId+"，报文:"+message);
        if(webSocketMap.containsKey(pageId)) {
            Set<WebSocketUtils> webSocketUtilsList = webSocketMap.get(pageId);

            for (WebSocketUtils webSocketUtils : webSocketUtilsList) {
                webSocketUtils.sendMessage(message);
            }
        }

//        }else{
//            log.error("页面id:"+pageId+",不在线！");
//        }
    }

    /**
     * 发送自定义消息
     * */
    public static void sendInfo(Map<String,Object> message, @PathParam("pageId") int pageId) throws IOException {
//        log.info("发送消息到:"+pageId+"，报文:"+message);
        if(webSocketMap.containsKey(pageId)){
            Set<WebSocketUtils> webSocketUtilsList = webSocketMap.get(pageId);

            for (WebSocketUtils webSocketUtils : webSocketUtilsList) {
                webSocketUtils.sendMessage(message);
            }

        }
//        else{
//            log.error("页面id:"+pageId+",不在线！");
//        }
    }


    @Scheduled(fixedRate = 30000) // 每60秒执行一次
    public void checkHeartbeat() {
        lastHeartbeat.forEach((session, timestamp) -> {
            if (System.currentTimeMillis() - timestamp > 30000) { // 60秒没有心跳
                session.close();
            }
        });
    }


}