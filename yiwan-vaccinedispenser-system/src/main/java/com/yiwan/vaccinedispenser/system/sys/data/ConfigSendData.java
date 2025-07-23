package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author 78671
 */
@Data
public class ConfigSendData {

    //A柜小皮带伺服移动到1-5层皮带伺服要走的距离
    private Integer belt1;
    private Integer belt2;
    private Integer belt3;
    private Integer belt4;
    private Integer belt5;

    //A柜IO开闭时间（10ms）
    private Integer ioWaitTime;

    //C柜送药第1-6接种台抬升走的距离
    private Integer cabinetC1;
    private Integer cabinetC2;
    private Integer cabinetC3;
    private Integer cabinetC4;
    private Integer cabinetC5;
    private Integer cabinetC6;

    //机械手向上抬升距离
    private Integer handUpDistance;

    //机械手运动到C柜X位置
    private Integer handMoveCX;

    //机械手运动到C柜Z位置
    private Integer handMoveCZ;

    //机械收下药步进距离
    private  Integer handMoveCStepDis;


    //取药步进伸出距离
    private Integer handDropStepDis;


    //退苗退回的工作台
    private Integer returnWorkNum;



}
