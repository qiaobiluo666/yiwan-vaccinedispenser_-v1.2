package com.yiwan.vaccinedispenser.system.sys.data.request.netty;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 17:47
 * 工作模式参数设置
 */
@Data
public class WorkSettingRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 344241696694729076L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    @NotNull(message = "哪个柜子不能为空")
    private CabinetConstants.Cabinet cabinet;

    //选择柜子的指令
    @NotNull(message = "设置成哪个柜子")
    private CabinetConstants.Cabinet setCabinet;


}
