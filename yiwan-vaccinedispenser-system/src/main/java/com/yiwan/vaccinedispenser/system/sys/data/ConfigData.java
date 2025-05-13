package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc 系统参数的data
 * @date 2024/4/18 16:00
 */

@Data
public class ConfigData {
    //激光传感器偏移量
    private int sensorDistanceX;
    private int sensorDistanceY;
    private int sensorDistanceZ;



    //左边距离传感器初始距离
    private int  leftConstants;
    //右边距离传感器初始距离
    private int  rightConstants;
    //上方距离传感器初始距离
    private int heightConstants;


    //直角点位角度
    private double  tableAngle;
    //直角点位Y坐标
    private int tableX;
    //直角点位X坐标
    private int tableY;
    //Z轴零位到皮带的距离
    private int tableZ;


    //激光传感器X
    private int lenDistanceX;
    //激光传感器Y
    private int lenDistanceY;
    // 激光传感器Z
    private int lenDistanceZ;


    //上方扫码X
    private int aboveScanX;
    //上方扫码Y
    private int aboveScanY;
    //上方扫码Z
    private int aboveScanZ;


    //侧边扫码X
    private int sideScanX;
    //侧边扫码Y
    private int sideScanY;
    //侧边扫码Z
    private int sideScanZ;


    //下方扫码X
    private int belowScanX;
    //下方扫码Y
    private int belowScanY;
    //下方扫码Z
    private int belowScanZ;

    //掉药距离X
    private int dropX;
    //掉药距离Y
    private int dropY;
    //掉药距离Z皮带传感器发生异常皮带传感器发生异常
    private int dropZ;

    //废药距离X
    private int wasteX;
    //废药距离Y
    private int wasteY;
    //废药距离Z
    private int wasteZ;

    //机械手总宽度
    private int handLen;

    //夹药空隙
    private int gap;

    //提前夹药空隙
    private int early;

    //机械手回原位X
    private int handInitX;
    //机械手回原位Z
    private int handInitZ;

    //小皮带走的距离
    private int smallBeltDistance;

    //手动上药 机械手就绪位置X
    private int handDrugX;

    //手动上药  机械手就绪位置Z
    private int handDrugZ;

    //自动上药 掉药区域位置是加还是减
    private String dropXAdd;

    //10层板板长
    private  int   lineLong;

    //右边旋转角度
    private int rightAngle;

    //左边旋转角度
    private int leftAngle;

}
