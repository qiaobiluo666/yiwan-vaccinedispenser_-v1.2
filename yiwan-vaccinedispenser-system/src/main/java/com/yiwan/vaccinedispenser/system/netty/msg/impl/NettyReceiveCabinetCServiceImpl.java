package com.yiwan.vaccinedispenser.system.netty.msg.impl;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetAMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetCMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetSysMsg;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author slh
 */
@Slf4j
@Service("nettyReceiveCabinetCService")
public class NettyReceiveCabinetCServiceImpl implements NettyReceiveCabinetService {

    @Autowired
    private CabinetAMsg cabinetAMsg;


    @Autowired
    private CabinetCMsg cabinetCMsg;

    @Autowired
    private CabinetSysMsg cabinetSysMsg;

    @Async("nettyThreadPool")
    @Override
    public void receiveMsg(String[] bytesStr) throws Exception {
        Integer workMode =11;
        //确定该帧号下的指令收到
        cabinetAMsg.receiveMsgFrame(bytesStr,workMode);
        switch (bytesStr[6]){
            //A柜返回的指令
            case "0A":
                log.warn("B柜不执行A柜的指令");
                //B柜返回的指令
            case "0B":
                break;
            case "0C":
                cabinetCMsg.receiveMsgCabinetC(bytesStr);
            //设置系统参数
            case "80":
                break;
            //获取系统参数
            case "81" :
                log.info("收到A柜{}:{}",CabinetConstants.CabinetSettingType.GET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
                cabinetSysMsg.receiveMsgCabinetSys(CabinetConstants.Cabinet.CAB_A,bytesStr);

            default:
                log.warn("收到未知指令：{}", (Object) bytesStr);
                break;

        }
//        log.info("B柜收到的消息是：{}", (Object) bytesStr);
    }
}