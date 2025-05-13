package com.yiwan.vaccinedispenser.system.camera;

import com.yiwan.vaccinedispenser.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/22 10:18
 */
@Service
@Slf4j
public class CameraSendMsg {

    @Autowired(required = false)
    private CameraClient aboveCamera;

    @Autowired(required = false)
    private CameraClient belowCamera;

    @Autowired(required = false)
    private CameraClient sideCamera;




//    private final CameraClient aboveCamera;
//    private final CameraClient belowCamera;
//    private final CameraClient sideCamera;

//    @Autowired
//    public CameraSendMsg(
//            @Qualifier("aboveCamera") CameraClient aboveCamera,
//            @Qualifier("belowCamera") CameraClient belowCamera,
//            @Qualifier("sideCamera") CameraClient sideCamera) {
//        this.aboveCamera = aboveCamera;
//        this.belowCamera = belowCamera;
//        this.sideCamera = sideCamera;
//    }

    public void sendCommandToAboveCamera() {
        if(aboveCamera!=null){
            aboveCamera.sendCommand();

        }else {
            log.warn("上方相机没连接");
            throw new ServiceException("上方相机没连接");
        }

    }

    public void sendCommandToBelowCamera() {
        if(belowCamera!=null){
            belowCamera.sendCommand();
        }else {
            log.warn("下方相机没连接");
            throw new ServiceException("下方相机没连接");
        }

    }

    public void sendCommandToSideCamera() {
        if(sideCamera!=null){
            sideCamera.sendCommand();
        }else {
            log.warn("侧方相机没连接");
            throw new ServiceException("侧方相机没连接");
        }

    }
}
