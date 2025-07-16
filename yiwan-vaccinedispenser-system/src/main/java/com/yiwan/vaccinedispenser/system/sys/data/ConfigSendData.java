package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author 78671
 */
@Data
public class ConfigSendData {

    //A柜小皮带伺服移动到1-5层皮带伺服要走的距离
    private int belt1;
    private int belt2;
    private int belt3;
    private int belt4;
    private int belt5;

    //A柜IO开闭时间（10ms）
    private int ioWaitTime;

    //C柜送药第1-6接种台抬升走的距离
    private int cabinetC1;
    private int cabinetC2;
    private int cabinetC3;
    private int cabinetC4;
    private int cabinetC5;
    private int cabinetC6;

    //机械手向上抬升距离
    private int handUpDistance;

    //退苗退回的工作台
    private int returnWorkNum;

}
