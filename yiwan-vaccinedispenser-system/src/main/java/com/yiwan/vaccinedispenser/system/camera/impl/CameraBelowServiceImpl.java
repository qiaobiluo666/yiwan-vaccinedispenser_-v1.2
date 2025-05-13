package com.yiwan.vaccinedispenser.system.camera.impl;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.camera.NettyReceiveCameraService;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetAMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetCMsg;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author slh
 */
@Slf4j
@Service("cameraBelowService")
public class CameraBelowServiceImpl implements NettyReceiveCameraService {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Async("nettyThreadPool")
    @Override
    public void receiveMsg(String msg) throws Exception {
//        valueOperations.set(RedisKeyConstant.scanCode.BELOW, msg);
        if (!"NoRead".equals(msg)) {
            String lastCode = valueOperations.get(RedisKeyConstant.scanCode.LAST_BELOW);
            if (!"NoRead".equals(lastCode)) {
                //如果上一次的扫码值跟这次的相等 说明重复扫码  不相等 则是扫到新码
                if (!lastCode.equals(msg)) {
                    valueOperations.set(RedisKeyConstant.scanCode.LAST_BELOW, msg);
                    valueOperations.set(RedisKeyConstant.scanCode.BELOW, msg);
                }
            }else {
                valueOperations.set(RedisKeyConstant.scanCode.LAST_BELOW, msg);
                valueOperations.set(RedisKeyConstant.scanCode.BELOW, msg);
            }

        }
    }
}