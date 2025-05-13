package com.yiwan.vaccinedispenser.core.websocket.impl;

import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.core.websocket.utils.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class WebsocketServiceImpl implements WebsocketService {

    @Override
    public void sendInfo(int userId, String message) throws IOException {
        WebSocketUtils.sendInfo(message, userId);
    }


    @Override
    public void sendInfo(int userId, Map<String,Object> message) throws IOException {
//        log.info("发送websocket信息：{}",message);
        WebSocketUtils.sendInfo(message, userId);
    }
    @Override
    public void processMessage(int pageId, Session session, String message) {
//        log.info("【websocket消息】收到客户端发来的消息:{}", message);
//        session.sendText("数据，已经收到了");   // 测试回复功能
    }
}
