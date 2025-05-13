package com.yiwan.vaccinedispenser.system.sys.data.request.netty;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author slh
 * @date 2024/2/20 0020 17:47
 * 时间参数设置
 */
@Data
public class TimeSettingRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 344241696694729076L;

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
    @NotNull(message = "请选择什么柜相关的参数")
    private Integer status;

    //超时时间长 10ms
    private BigInteger timeLong;

}
