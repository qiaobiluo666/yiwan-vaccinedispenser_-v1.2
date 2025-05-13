package com.yiwan.vaccinedispenser.system.netty.msg.impl;

import com.yiwan.vaccinedispenser.system.netty.function.CabinetAMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetSysMsg;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author
 */
@Slf4j
@Service("nettyReceiveCabinetAService")
public class NettyReceiveCabinetAServiceImpl implements NettyReceiveCabinetService {

    @Autowired
    private CabinetAMsg cabinetAMsg;
    @Autowired
    private CabinetSysMsg cabinetSysMsg;
    @Async("nettyThreadPool")
    @Override
    public void receiveMsg(String[] bytesStr) throws IOException {
        Integer workMode =10;
        //确定该帧号下的指令收到
        cabinetAMsg.receiveMsgFrame(bytesStr,workMode);

        switch (bytesStr[6]) {
            //A柜返回的指令
            case "0A" -> cabinetAMsg.receiveMsgCabinetA( bytesStr);

            //B柜返回的指令
            case "0B" -> {
                log.info("A柜收到的消息是：{}", (Object) bytesStr);
                log.warn("A柜不执行B柜的指令");
            }
//
            case "0C" -> {
                log.info("A柜收到的消息是：{}", (Object) bytesStr);
                log.warn("A柜不执行C柜的指令");
            }
            default -> log.warn("收到未知指令：{}", (Object) bytesStr);
        }
    }


}
