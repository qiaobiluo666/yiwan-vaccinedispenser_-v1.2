package com.yiwan.vaccinedispenser.system.camera;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacCabinet;
import com.yiwan.vaccinedispenser.system.netty.NettyClient;
import com.yiwan.vaccinedispenser.system.netty.msg.NettyReceiveCabinetService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacCabinetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 78671
 */
@Slf4j
@Configuration
public class CameraClientConfiguration {

    // 用一个固定的线程池
    ExecutorService threadPool = Executors.newFixedThreadPool(3);


    @Autowired
    private CameraPortConfig cameraPortConfig;

    @Autowired
    @Qualifier("cameraAboveService")
    private NettyReceiveCameraService cameraAboveService;

    @Autowired
    @Qualifier("cameraBelowService")
    private NettyReceiveCameraService cameraBelowService;


    @Autowired
    @Qualifier("cameraSideService")
    private NettyReceiveCameraService cameraSideService;






    @Bean("aboveCamera")
    public CameraClient aboveCamera() throws Exception {
        if(cameraPortConfig.isEnabled()){
            return getOneNetty(cameraPortConfig.getAbove(), 2001, "上方扫码相机",cameraAboveService);
        }else {
            return null;
        }

    }

    @Bean("belowCamera")
    public CameraClient belowCamera() throws Exception {
        if(cameraPortConfig.isEnabled()){
            return getOneNetty(cameraPortConfig.getBelow(), 2001, "下方扫码相机",cameraBelowService);
        }else {
            return null;
        }

    }

    @Bean("sideCamera")
    public CameraClient sideCamera() throws Exception{

        if(cameraPortConfig.isEnabled()){
            return getOneNetty(cameraPortConfig.getSide(), 2001, "侧方扫码相机",cameraSideService);
        }else {
            return null;
        }
    }


    public CameraClient getOneNetty(String host, int port, String name, NettyReceiveCameraService nettyReceiveCameraService) throws Exception {
        CameraClient cameraClient = new CameraClient(host, port, name, nettyReceiveCameraService);
        cameraClient.start();
        threadPool.submit(()->{
            try {
                cameraClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return cameraClient;
    }
}



