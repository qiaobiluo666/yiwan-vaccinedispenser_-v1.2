package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 15:14
 * 步进电机指令
 */
@Data
public class CabinetCStepRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -776692512964683018L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;
    //步进模式选择
    @NotNull(message = "步进模式选择")
    private CabinetConstants.CabinetCStepCommand command;

    //步进模式选择
    private int mode;

    @NotNull(message = "步进运动状态")
    private CabinetConstants.CabinetCStepStatus status;


    //运动角度
    private Integer distance;
   //速度
    private Integer speed;

}
