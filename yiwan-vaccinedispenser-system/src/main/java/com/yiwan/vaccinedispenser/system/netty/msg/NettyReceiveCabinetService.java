package com.yiwan.vaccinedispenser.system.netty.msg;

/**
 * @author liuwei
 */
public interface NettyReceiveCabinetService {

    void receiveMsg(String[] bytesStr) throws Exception;
}
