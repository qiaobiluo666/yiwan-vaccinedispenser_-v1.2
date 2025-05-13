package com.yiwan.vaccinedispenser.system.netty.msg.impl;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.netty.msg.NettySendService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("!${netty.enable:true} ")
public class NettySendCabinetServiceDisableImpl implements NettySendService {

    @Override
    public Result sendMsg(CabinetConstants.Cabinet cabinetType, byte[] msg, int frame) {
        log.info("debug测试发送命令为：{}", NettyUtils.getFormatHexStr(msg));
        return Result.fail("debug测试发送命令为："+ NettyUtils.getFormatHexStr(msg));
    }

}
