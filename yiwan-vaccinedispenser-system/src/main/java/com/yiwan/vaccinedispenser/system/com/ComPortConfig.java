package com.yiwan.vaccinedispenser.system.com;

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
public class ComPortConfig {

    @Value("${com.com1}")
    private String com1;

    @Value("${com.com2}")
    private String com2;

    @Value("${com.com3}")
    private String com3;

    @Value("${com.enable}")
    private boolean enabled;
}
