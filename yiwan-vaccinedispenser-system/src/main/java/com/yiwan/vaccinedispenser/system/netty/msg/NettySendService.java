package com.yiwan.vaccinedispenser.system.netty.msg;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.web.Result;

public interface NettySendService {

    /**
     * 通过网络发送命令
     * @param cabinetType 仓柜的类型(值的是A柜、B柜、C柜)
     * @param msg 发送的字节数组
     */
    Result sendMsg(CabinetConstants.Cabinet cabinetType, byte[] msg, int frame);

}
