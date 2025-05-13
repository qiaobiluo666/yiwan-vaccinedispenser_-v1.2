package com.yiwan.vaccinedispenser.core.websocket;

import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.Map;

public interface WebsocketService {

    /**
     * 发送一条消息
     * @param pageId  页面的id
     * @param message 发送的消息
     * @throws IOException
     */
    void sendInfo(int pageId, String message) throws IOException;

    /**
     * 发送一条消息
     * @param pageId  页面的id
     * @param message 发送的消息
     * @throws IOException
     */
    void sendInfo(int pageId, Map<String,Object> message) throws IOException;



    /**
     * 页面发送的消息
     * @param pageId  页面的id
     * @param session 对话
     * @param message 发送的消息
     */
    void processMessage(int pageId, Session session, String message);
}
