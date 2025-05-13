package com.yiwan.vaccinedispenser.system.sysinit;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enable mybatis helper configuration
 *
 * @author gaigeshen
 */
// 多个注解包，中间用逗号分隔  类似 @MapperScan(basePackages = {"", ""})  tmny 20230608
@MapperScan(basePackages = "com.yiwan.vaccinedispenser.system.sys.dao")
@Configuration
public class MybatisHelperConfiguration {


  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
    paginationInnerInterceptor.setDbType(DbType.MYSQL);
    paginationInnerInterceptor.setOverflow(true);
    interceptor.addInnerInterceptor(paginationInnerInterceptor);
    OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor = new OptimisticLockerInnerInterceptor();
    interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor);
    return interceptor;
  }
}
