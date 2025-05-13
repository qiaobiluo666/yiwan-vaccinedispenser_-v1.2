package com.yiwan.vaccinedispenser.system.netty.msg.impl;



import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.web.ErrorCode;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.netty.NettyClient;
import com.yiwan.vaccinedispenser.system.netty.msg.NettySendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
@Primary
@ConditionalOnExpression("${netty.enable:true} ")
public class NettySendCabinetServiceImpl implements NettySendService {

    @Resource
    Map<String , NettyClient> nettyClientMap;

    @Override
    public Result sendMsg(CabinetConstants.Cabinet cabinetType, byte[] msg, int frame) {
        NettyClient nettyClient = getNettyClient(cabinetType);
        if(nettyClient==null){
            log.error("发送{}指令异常:{}",cabinetType.desc,msg);
            return Result.failure(ErrorCode.CONTROL_BOARD_INSTRUCT_ERROR);
        }else {
           return nettyClient.send(msg,frame,cabinetType);
        }
    }


    /**
     * 通过类型进行获取对应的client
     * @param cabinetType 机柜的类型
     * @return
     */
    private NettyClient getNettyClient(CabinetConstants.Cabinet cabinetType){
        NettyClient client = null;
        switch (cabinetType){
            case CAB_A:
                client = nettyClientMap.get("cabinetAClient");
                break;
            case CAB_B:
                client = nettyClientMap.get("cabinetBClient");
                break;

            case CAB_C:
                client = nettyClientMap.get("cabinetCClient");
                break;
            default:
                break;
        }

        return client;
    }
}
