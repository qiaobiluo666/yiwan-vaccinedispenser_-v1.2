package com.yiwan.vaccinedispenser.core.config;

import org.springframework.stereotype.Component;

/**
 * @author slh
 * @date 2023/5/10
 * @Description
 * 定义全局变量设置 帧数的全局变量
 */
@Component
public class FrameNumberConfig {


    private  static   Integer frameNumber=0;


    public static synchronized  Integer  getFrameNumber(){

        if (frameNumber>=10000){
            frameNumber=0;
        }
        frameNumber++;
        return frameNumber;
    }



}
