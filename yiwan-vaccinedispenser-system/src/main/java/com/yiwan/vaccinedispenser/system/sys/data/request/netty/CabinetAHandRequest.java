package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 14:57
 * 伺服命令
 */
@Data
public class CabinetAHandRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 6569389881464615111L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;


    //请选择第几个伺服电机
    @NotNull(message = "请选择第几个伺服电机")
    private Integer servoX;

    //运动距离
    private Integer distanceX;


    //请选择第几个伺服电机
    @NotNull(message = "请选择第几个伺服电机")
    private Integer servoZ;

    //运动距离
    private Integer distanceZ;

    //向上抬升距离
    private Integer upDistance;




}
