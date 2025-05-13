package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/27 14:36
 */
@Data
public class ServoSettingData implements Serializable {

    @Serial
    private static final long serialVersionUID = 2290131667262734665L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    @NotNull(message = "哪个柜子不能为空")
    private CabinetConstants.Cabinet cabinet;

    //伺服ID
    @NotNull(message = "伺服id不能为空")
    private Integer mode;

    //单圈脉冲
    @NotNull(message = "单圈脉冲不能为空")
    private BigInteger pulse;

    //单圈距离
    @NotNull(message = "单圈距离不能为空")
    private BigInteger distance;

    //最大运行距离
    @NotNull(message = "最大运行距离不能为空")
    private BigInteger maxDistance;

    //速度
    @NotNull(message = "速度(%)不能为空")
    private BigInteger speed;

    //回原速度
    @NotNull(message = "回原速度(%)不能为空")
    private BigInteger returnSpeed;

    //加速度时间
    @NotNull(message = "加速度时间不能为空")
    private BigInteger accelerationTime;

    //减速时间
    private BigInteger decelerationTime;

    //加加速度
    @NotNull(message = "加加速度不能为空")
    private BigInteger acceleration;


    //原点信号开关
    private Integer zeroSwitch;

    //原点方向
    @NotNull(message = "原点方向不能为空")
    private Integer zero;

}
