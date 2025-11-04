package com.yiwan.vaccinedispenser.system.sys.service.other;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.ffmpeg.FFmpegManager;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigCameraData;
import com.yiwan.vaccinedispenser.system.sys.data.request.CameraRequest;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 78671
 */
@Service
@Slf4j
public class CameraService {
    @Autowired
    private SysConfigService sysConfigService;
    private final FFmpegManager ffmpegManager;

    public CameraService(FFmpegManager ffmpegManager) {
        this.ffmpegManager = ffmpegManager;
    }

//    private static final String[] RTSP_URLS = {
//            "rtsp://192.168.1.1:554/user=admin&password=123456&channel=1&stream=0.sdp",
//            "rtsp://192.168.1.2:554/user=admin&password=123456&channel=2&stream=0.sdp",
//            "rtsp://192.168.1.3:554/user=admin&password=123456&channel=3&stream=0.sdp",
//            "rtsp://192.168.1.4:554/user=admin&password=123456&channel=4&stream=0.sdp",
//            "rtsp://192.168.1.5:554/user=admin&password=123456&channel=5&stream=0.sdp",
//            "rtsp://192.168.1.6:554/user=admin&password=123456&channel=6&stream=0.sdp",
//            "rtsp://192.168.1.7:554/user=admin&password=123456&channel=7&stream=0.sdp",
//            "rtsp://192.168.1.8:554/user=admin&password=123456&channel=8&stream=0.sdp",
//    };


//    @PostConstruct
//    public void init() {
//        for (int i = 0; i < RTSP_URLS.length; i++) {
//            String streamName = "cam" + (i + 1);
//            ffmpegManager.startFFmpeg(RTSP_URLS[i], streamName);
//        }
//    }

    public void stopStream(String streamName) {
        ffmpegManager.stopFFmpeg(streamName);
    }

    public void stopStreamAll() {
        //拿出所有摄像头的ip和名称
        List<SysConfig> sysConfigList = sysConfigService.getCameraConfigData();
        //如果数据库有 那就使用数据库的
        for(SysConfig sysConfig: sysConfigList) {
            ffmpegManager.stopFFmpeg(sysConfig.getDescriptions());
        }

    }

    public void startStream(String rtspUrl, String streamName) {
        ffmpegManager.startFFmpeg(rtspUrl, streamName);
    }


    public List<CameraRequest> startStreamAll() {
        //拿出所有摄像头的ip和名称
        List<SysConfig> sysConfigList = sysConfigService.getCameraConfigData();
        List<CameraRequest> cameraRequestList = new ArrayList<>();

        String rtsp,name,title;
        int count = 1;
        //如果数据库有 那就使用数据库的
        for(SysConfig sysConfig: sysConfigList) {
            rtsp = "rtsp://"+sysConfig.getConfigValue()+"/user=admin&password=123456&channel="+count+"&stream=0.sdp";
            title = sysConfig.getDescriptions();
            name = "cam"+count;
            ffmpegManager.startFFmpeg(rtsp, name);
            count++;
            CameraRequest cameraRequest = new CameraRequest();
            cameraRequest.setRtspUrl(rtsp);
            cameraRequest.setName(name);
            cameraRequest.setTitle(title);
            cameraRequestList.add(cameraRequest);
        }
        log.info(JSON.toJSONString(cameraRequestList));
        return cameraRequestList;
    }


    public CameraRequest getShowUrl(){
        SysConfig sysConfig = sysConfigService.getCameraShowUrl();
        String rtsp = "rtsp://"+sysConfig.getConfigValue()+"/user=admin&password=123456&channel=cam6&stream=0.sdp";
        CameraRequest cameraRequest = new CameraRequest();
        cameraRequest.setRtspUrl(rtsp);
        cameraRequest.setName("cam6");
        return cameraRequest;
    }





}
