package com.yiwan.vaccinedispenser.system.camera;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/21 9:40
 */
@Component
@Data
public class CameraPortConfig {

    @Value("${camera.above}")
    private String above;

    @Value("${camera.below}")
    private String below;

    @Value("${camera.side}")
    private String side;

    @Value("${camera.enable}")
    private boolean enabled;
}
