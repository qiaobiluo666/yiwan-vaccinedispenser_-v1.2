package com.yiwan.vaccinedispenser.system.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacCabinet;
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
@ConditionalOnExpression("${netty.enable:true}")
public class NettyClientConfiguration {

    // 用一个固定的线程池
    ExecutorService threadPool = Executors.newFixedThreadPool(3);

    @Autowired
    @Qualifier("nettyReceiveCabinetAService")
    private NettyReceiveCabinetService nettyReceiveCabinetAService;

    @Autowired
    @Qualifier("nettyReceiveCabinetBService")
    private NettyReceiveCabinetService nettyReceiveCabinetBService;


    @Autowired
    @Qualifier("nettyReceiveCabinetCService")
    private NettyReceiveCabinetService nettyReceiveCabinetCService;



    @Autowired
    private VacCabinetService vacCabinetService;

    /**
     * 生成boxA对象
     * 如果 netty.boxA.enable=true生成，如果是false。不执行，生成对象
     * @return
     * @throws Exception
     */
    @Bean("cabinetAClient")
    public NettyClient nettyClientBean() throws  Exception {
        VacCabinet oneAgeCabinet = vacCabinetService.getOneVacCabinet(CabinetConstants.Cabinet.CAB_A.name, 1);
        if (oneAgeCabinet != null) {
            String ip = oneAgeCabinet.getIp();
            Integer port = oneAgeCabinet.getPort();

            return getOneNetty(ip, port, CabinetConstants.Cabinet.CAB_A.name, nettyReceiveCabinetAService);
        } else {
            return null;
        }
    }

    /**
     * 生成boxB client对象
     * @return
     * @throws Exception
     */
    @Bean("cabinetBClient")
    public NettyClient nettyClientBeanB() throws Exception {

        VacCabinet oneAgeCabinet = vacCabinetService.getOneVacCabinet(CabinetConstants.Cabinet.CAB_B.name,  1);
        if (oneAgeCabinet != null) {
            String ip = oneAgeCabinet.getIp();
            Integer port = oneAgeCabinet.getPort();

            return getOneNetty(ip, port, CabinetConstants.Cabinet.CAB_B.name, nettyReceiveCabinetBService);
        } else {
            return null;
        }
    }

    /**
     * 生成boxB client对象
     * @return
     * @throws Exception
     */
    @Bean("cabinetCClient")
    public NettyClient nettyClientBeanC() throws Exception {

        VacCabinet oneAgeCabinet = vacCabinetService.getOneVacCabinet(CabinetConstants.Cabinet.CAB_C.name,  1);
        if (oneAgeCabinet != null) {
            String ip = oneAgeCabinet.getIp();
            Integer port = oneAgeCabinet.getPort();

            return getOneNetty(ip, port, CabinetConstants.Cabinet.CAB_C.name, nettyReceiveCabinetCService);
        } else {
            return null;
        }
    }
    /**
     * 生成一个netty对象
     * @param host 服务端的地址
     * @param port 服务端的端口号
     * @param name 服务的名称，日志用来区分的
     * @param nettyReceiveService 接收对象
     * @return
     */
    public NettyClient getOneNetty(String host, int port, String name, NettyReceiveCabinetService nettyReceiveService) {
        NettyClient nettyClient = new NettyClient(host, port, name, nettyReceiveService);
        nettyClient.init();
        threadPool.submit(()->{
            try {
                nettyClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return nettyClient;
    }

}
