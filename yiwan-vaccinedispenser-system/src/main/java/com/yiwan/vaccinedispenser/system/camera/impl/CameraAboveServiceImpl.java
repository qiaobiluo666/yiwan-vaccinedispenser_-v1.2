package com.yiwan.vaccinedispenser.system.camera.impl;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.camera.NettyReceiveCameraService;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetAMsg;
import com.yiwan.vaccinedispenser.system.netty.function.CabinetSysMsg;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author
 */
@Slf4j
@Service("cameraAboveService")
public class CameraAboveServiceImpl implements NettyReceiveCameraService {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Async("nettyThreadPool")
    @Override
    public void receiveMsg(String msg) {
        log.info("上方扫码数据！========================================：{}",msg);
        valueOperations.set(RedisKeyConstant.scanCode.ABOVE, msg);


    }


}
