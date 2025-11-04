package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 14:57
 * 获取距离传感器
 */
@Data
public class CabinetAGetDistanceRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 3646188880863838448L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //距离传感器模式选择 给哪块板子发指令
    @NotNull(message = "距离传感器模式选择")
    private CabinetConstants.CabinetAGetDistanceCommand command;

    //传感器站号
    private Integer mode;

    //伺服id
    private Integer status;

    //阈值
    private Integer threshold;

    //传感器距离
    private Integer sensorDis;

    //伺服运动速度
    private Integer speed;

    //起始距离
    private Integer startDis;

    //结束距离
    private Integer endDis;




}
