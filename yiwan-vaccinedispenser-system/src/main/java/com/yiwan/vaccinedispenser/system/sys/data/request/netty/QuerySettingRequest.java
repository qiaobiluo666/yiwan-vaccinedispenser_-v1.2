package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/27 14:43
 */
@Data
public class QuerySettingRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1551889281784577967L;

    //工作模式-对应的ab板子 给哪块板子发指令
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    private CabinetConstants.Cabinet cabinet;

    private Integer command;

    private Integer mode;

    private Integer status;


}
