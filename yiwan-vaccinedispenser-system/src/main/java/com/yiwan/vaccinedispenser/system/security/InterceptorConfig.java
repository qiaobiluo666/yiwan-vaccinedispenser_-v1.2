package com.yiwan.vaccinedispenser.system.security;

import com.yiwan.vaccinedispenser.core.security.CurrentUserMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class InterceptorConfig  extends WebMvcConfigurerAdapter {
 
    @Autowired
    private CheckTokenInterceptor  checkTokenInterceptor;
 
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(checkTokenInterceptor)
                // 目录相关
                .addPathPatterns("/menu/**")
                // 权限
                .addPathPatterns("/role/**")
                // 用户管理
                .addPathPatterns("/sys-user/**")
                //获取系统参数
                .addPathPatterns("/sys-config/**")
                // 疫苗发药管理
                .addPathPatterns("/drug/**")

                // 药仓规格管理
                .addPathPatterns("/box-spec/**")

                // 仓柜管理
                .addPathPatterns("/machine/**")

                //设备异常
                .addPathPatterns("/exception/**")
                //工作台配置
                .addPathPatterns("/workbench/**")



        ;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentUserMethodArgumentResolver());
        super.addArgumentResolvers(argumentResolvers);
    }

    @Bean
    public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
        return new CurrentUserMethodArgumentResolver();
    }
}