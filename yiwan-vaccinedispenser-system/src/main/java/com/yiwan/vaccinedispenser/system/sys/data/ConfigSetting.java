package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author 78671
 */
@Data
public class ConfigSetting {

    //政采云发药模块 是否启用
    private String zcySend;
    //政采云自动上药模块 是否启用
    private String zcyAuto;
    //C柜挡片 是否配置
    private String cBlank;
    //C柜抬升 是否配置
    private String cLifting;
    //B柜自动上药参数
    private String bFindX;


    //C柜挡片早上开启时间
    private String cBlankOpenMorning;
    //C柜挡片早上关闭时间
    private String cBlankCloseMorning;
    //C柜挡片下午开启时间
    private String cBlankOpenAfternoon;
    //C柜挡片关闭开启时间
    private String cBlankCloseAfternoon;



}
