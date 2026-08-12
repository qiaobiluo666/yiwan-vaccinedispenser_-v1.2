package com.yiwan.vaccinedispenser.system.plc.config;

import com.yiwan.vaccinedispenser.system.plc.client.PlcClient;
import com.yiwan.vaccinedispenser.system.plc.data.PlcRequestCache;
import com.yiwan.vaccinedispenser.system.plc.data.PlcStatusDispatcher;
import com.yiwan.vaccinedispenser.system.plc.manager.AlarmPollManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcConnectionManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcPollManager;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcSendService;
import com.yiwan.vaccinedispenser.system.plc.manager.PlcService;
import com.yiwan.vaccinedispenser.system.plc.manager.impl.PlcServiceImpl;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnExpression("${plc.enable:true}")
public class PlcClientConfiguration {

    @Bean("plcClient")
    public PlcClient plcClient(PlcConfig plcConfig, PlcRequestCache requestCache, PlcStatusDispatcher plcStatusDispatcher) {
        PlcClient plcClient = new PlcClient(plcConfig);
        plcClient.setRequestCache(requestCache);
        plcClient.setPlcStatusDispatcher(plcStatusDispatcher);
        plcClient.init();
        log.debug("[PLC] PlcClient Bean创建完成");
        return plcClient;
    }

    @Bean("plcConnectionManager")
    public PlcConnectionManager plcConnectionManager(PlcClient plcClient, PlcConfig plcConfig, PlcPollManager pollManager, AlarmPollManager alarmPollManager, VacMachineExceptionService vacMachineExceptionService) {
        return new PlcConnectionManager(plcClient, plcConfig, pollManager, alarmPollManager, vacMachineExceptionService);
    }

    @Bean("plcPollManager")
    public PlcPollManager plcPollManager(PlcSendService sendService, PlcConfig plcConfig) {
        return new PlcPollManager(sendService, plcConfig);
    }

    @Bean("alarmPollManager")
    public AlarmPollManager alarmPollManager(PlcSendService sendService, PlcConfig plcConfig) {
        return new AlarmPollManager(sendService, plcConfig);
    }

    @Bean("plcService")
    public PlcService plcService(PlcConnectionManager connectionManager, PlcPollManager pollManager,
                                  PlcSendService sendService, PlcClient plcClient) {
        return new PlcServiceImpl(connectionManager, pollManager, sendService, plcClient);
    }
}
