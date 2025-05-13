package com.yiwan.vaccinedispenser.system.netty.msg.impl;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetAMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetBMsg;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author slh
 */
@Slf4j
@Service("nettyReceiveCabinetBService")
public class NettyReceiveCabinetBServiceImpl implements NettyReceiveCabinetService {

    @Autowired
    private CabinetAMsg cabinetAMsg;

    @Autowired
    private CabinetBMsg cabinetBMsg;

    @Async("nettyThreadPool")
    @Override
    public void receiveMsg(String[] bytesStr) throws IOException {
        Integer workMode =11;
        //确定该帧号下的指令收到
        cabinetAMsg.receiveMsgFrame(bytesStr,workMode);
        switch (bytesStr[6]){

            //A柜返回的指令
            case "0A":
                log.info("收到{}:{}", CabinetConstants.CabinetBType.APPLY.desc, NettyUtils.StringListToString(bytesStr));
                log.warn("B柜不执行A柜的指令");
            //B柜返回的指令
            case "0B":
                cabinetBMsg.receiveMsgCabinetB(bytesStr);
                break;
            //设置系统参数
            case "80":

                break;

            case "81":
//                ageCabinetSysService.processingSysResponseOperation(bytesStr,workMode );
                break;
            default:
                log.warn("收到未知指令：{}", (Object) bytesStr);
                break;

        }
//        log.info("B柜收到的消息是：{}", (Object) bytesStr);
    }
}