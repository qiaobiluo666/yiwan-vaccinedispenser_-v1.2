package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/18 14:27
 */
@Data
public class DistanceServoData {
    //伺服X
    private Integer servoX;
    //伺服Y
    private Integer servoY;
    //伺服Z
    private Integer servoZ;

    //左边的药盒长度
    private Integer vaccineLong;
    //右边药盒长度
    private Integer vaccineWide;
    //高度
    private Integer vaccineHigh;




    //判断长的边是否在左侧
    private Boolean isLeft;
    //是否要重新发药 药品立着
    private Boolean isReturn;


    private String aboveCode;

}
