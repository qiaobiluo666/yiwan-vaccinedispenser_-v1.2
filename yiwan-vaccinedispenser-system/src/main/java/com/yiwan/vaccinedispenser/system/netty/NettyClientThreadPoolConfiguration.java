package com.yiwan.vaccinedispenser.system.netty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Slf4j
@Configuration
public class NettyClientThreadPoolConfiguration {

    @Bean("nettyThreadPool")
    public ExecutorService getNettyReceiveThreadPool(){
        return new ThreadPoolExecutor(480, 1000,
                1000, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(800),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "t_netty_pool_" + r.hashCode());
                    }
                }
        );
    }
}
