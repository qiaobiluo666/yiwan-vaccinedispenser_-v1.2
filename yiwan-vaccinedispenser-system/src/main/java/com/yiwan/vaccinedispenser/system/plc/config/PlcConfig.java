package com.yiwan.vaccinedispenser.system.plc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PlcConfig {

    @Value("${plc.enable}")
    private boolean enable;

    @Value("${plc.auto-start}")
    private boolean autoStart;

    @Value("${plc.host}")
    private String host;

    @Value("${plc.port}")
    private int port;

    @Value("${plc.connect-timeout}")
    private int connectTimeout;

    @Value("${plc.re-connect-delay}")
    private int reConnectDelay;

    @Value("${plc.max-retry}")
    private int maxRetry;

    @Value("${plc.poll-enabled:true}")
    private boolean pollEnabled;

    @Value("${plc.alarm-poll-enabled:true}")
    private boolean alarmPollEnabled;
}
