package com.yiwan.vaccinedispenser.web.controller.vac;

import com.yiwan.vaccinedispenser.core.web.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author slh
 */
@RestController
@Slf4j
@RequestMapping("/plc-webvisu")
public class PlcWebvisuController {

    @Value("${plc-webvisu.url:http://192.168.1.6:8080/webvisu.htm}")
    private String webvisuUrl;

    @GetMapping("/url")
    public Result getUrl() {
        log.info(webvisuUrl);
        return Result.success(Map.of("url", webvisuUrl));
    }
}
