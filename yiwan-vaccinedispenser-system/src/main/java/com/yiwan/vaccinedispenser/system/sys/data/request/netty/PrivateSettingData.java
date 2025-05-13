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
 * @date 2024/6/12 10:56
 */
@Data
public class PrivateSettingData implements Serializable {


    @Serial
    private static final long serialVersionUID = 1782628098254759622L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    @NotNull(message = "哪个柜子不能为空")
    private CabinetConstants.Cabinet cabinet;

    //选择柜的指令
    @NotNull(message = "请选择什么柜相关的参数")
    private CabinetConstants.SettingTimeCabinetMode mode;

    //选择哪种相关参数时间
    @NotNull(message = "具体参数")
    private Integer status;

    //方向数据 0 前进 1 后退
    private Integer zero;


    private BigInteger distance;

}
