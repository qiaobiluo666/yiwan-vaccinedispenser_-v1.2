package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.web.Result;
import com.yiwan.vaccinedispenser.system.sys.data.request.CameraRequest;
import com.yiwan.vaccinedispenser.system.sys.service.other.CameraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author 78671
 */
@RestController
@Slf4j
@RequestMapping("/camera")
public class CameraController {
    @Autowired
    private CameraService cameraService;


    @PostMapping("/start")
    public Result start(@RequestBody CameraRequest cameraRequest) {
        cameraService.startStream(cameraRequest.getRtspUrl(), cameraRequest.getName());
        return Result.success();
    }

    @GetMapping("/startAll")
    public Result startAll() {
        return Result.success(cameraService.startStreamAll());
    }

    @PostMapping("/stop")
    public Result stop(@RequestBody CameraRequest cameraRequest) {
        cameraService.stopStream(cameraRequest.getName());
        return Result.success();
    }

    @GetMapping("/stopAll")
    public Result stopAll() {
        cameraService.stopStreamAll();
        return Result.success();
    }

    @GetMapping("/showUrl")
    public Result getShowUrl(){
        return Result.success( cameraService.getShowUrl());
    }

}
