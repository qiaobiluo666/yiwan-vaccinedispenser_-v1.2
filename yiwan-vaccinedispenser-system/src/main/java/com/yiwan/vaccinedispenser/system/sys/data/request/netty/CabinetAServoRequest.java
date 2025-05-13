package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 14:57
 * 伺服命令
 */
@Data
public class CabinetAServoRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 6569389881464615111L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;


    //伺服模式选择
    @NotNull(message = "伺服模式选择")
    private CabinetConstants.CabinetAServoCommand command;

    //请选择第几个伺服电机
    @NotNull(message = "请选择第几个伺服电机")
    private Integer mode;

    //伺服子命令
    private CabinetConstants.CabinetAServoStatus status;

    //运动距离
   private Integer distance;

   //运动速度
    private Integer speed;
}
