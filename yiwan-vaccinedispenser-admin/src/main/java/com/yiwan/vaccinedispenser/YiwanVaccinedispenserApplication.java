package com.yiwan.vaccinedispenser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
*  Name：杭州医万智能装备有限公司 疫苗智能终端后端系统V1.0
*  Date：2024-02-26
*  Author：yiwan tech
**/


@EnableScheduling
@DependsOn(value = "springContextUtil")
@EnableAsync
@SpringBootApplication

public class YiwanVaccinedispenserApplication {

	public static void main(String[] args) {
		SpringApplication.run(YiwanVaccinedispenserApplication.class, args);
	}

}
