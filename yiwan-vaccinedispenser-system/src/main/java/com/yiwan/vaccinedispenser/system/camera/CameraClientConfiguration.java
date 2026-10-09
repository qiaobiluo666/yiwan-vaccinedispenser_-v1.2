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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 78671
 */
@Slf4j
@Configuration
@ConditionalOnExpression("${netty.enable:true}")
public class CameraClientConfiguration {

    /** 相机Bean先交给Spring完成依赖注入，应用完全启动后再建立TCP连接。 */
    private final List<CameraClient> cameraClients = new CopyOnWriteArrayList<>();


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
        cameraClients.add(cameraClient);
        return cameraClient;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void connectCamerasAfterApplicationReady() {
        log.info("程序已完全启动，开始初始化并连接{}台扫码相机", cameraClients.size());
        for (CameraClient cameraClient : cameraClients) {
            try {
                cameraClient.start();
                cameraClient.connect();
            } catch (Exception e) {
                log.error("扫码相机初始化连接失败", e);
            }
        }
    }
}



