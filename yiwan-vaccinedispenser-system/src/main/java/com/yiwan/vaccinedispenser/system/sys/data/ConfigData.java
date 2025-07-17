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
    private Integer sensorDistanceX;
    private Integer sensorDistanceY;
    private Integer sensorDistanceZ;



    //左边距离传感器初始距离
    private Integer  leftConstants;
    //右边距离传感器初始距离
    private Integer  rightConstants;
    //上方距离传感器初始距离
    private Integer heightConstants;


    //直角点位角度
    private double  tableAngle;
    //直角点位Y坐标
    private Integer tableX;
    //直角点位X坐标
    private Integer tableY;
    //Z轴零位到皮带的距离
    private Integer tableZ;


    //激光传感器X
    private Integer lenDistanceX;
    //激光传感器Y
    private Integer lenDistanceY;
    // 激光传感器Z
    private Integer lenDistanceZ;


    //上方扫码X
    private Integer aboveScanX;
    //上方扫码Y
    private Integer aboveScanY;
    //上方扫码Z
    private Integer aboveScanZ;


    //侧边扫码X
    private Integer sideScanX;
    //侧边扫码Y
    private Integer sideScanY;
    //侧边扫码Z
    private Integer sideScanZ;


    //下方扫码X
    private Integer belowScanX;
    //下方扫码Y
    private Integer belowScanY;
    //下方扫码Z
    private Integer belowScanZ;

    //掉药距离X
    private Integer dropX;
    //掉药距离Y
    private Integer dropY;
    //掉药距离Z皮带传感器发生异常皮带传感器发生异常
    private Integer dropZ;

    //废药距离X
    private Integer wasteX;
    //废药距离Y
    private Integer wasteY;
    //废药距离Z
    private Integer wasteZ;

    //机械手总宽度
    private Integer handLen;

    //夹药空隙
    private Integer gap;

    //提前夹药空隙
    private Integer early;

    //机械手回原位X
    private Integer handInitX;
    //机械手回原位Z
    private Integer handInitZ;

    //小皮带走的距离
    private Integer smallBeltDistance;

    //手动上药 机械手就绪位置X
    private Integer handDrugX;

    //手动上药  机械手就绪位置Z
    private Integer handDrugZ;

    //自动上药 掉药区域位置是加还是减
    private String dropXAdd;

    //10层板板长
    private  Integer   lineLong;

    //右边旋转角度
    private Integer rightAngle;

    //左边旋转角度
    private Integer leftAngle;

}
