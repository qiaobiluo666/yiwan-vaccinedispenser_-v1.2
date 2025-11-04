package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

//自动上药传参
@Data
public class AutoData {

    //层数
    private Integer lineNum;

    //开始仓位
    private  String startBoxNo;

    //结束仓位
    private  String endBoxNo;



    //机械手走的速度
    private Integer speed;

    //传感器的检测距离
    private Integer  sensorDis;

    //阈值
    private Integer threshold;

    //仓位偏移量
    private  Integer offsetDis;


}
