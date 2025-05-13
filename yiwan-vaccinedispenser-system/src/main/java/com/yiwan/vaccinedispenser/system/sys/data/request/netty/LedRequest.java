package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/12 9:29
 */
@Data
public class LedRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1800792390859099626L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //站号 第几块灯拓展板
    @Min(value = 1, message = "站号不能小于1")
    @NotNull(message = "站号不能为空")
    private Integer command;

    //执行指令 1 输出 2不输出 3 查询
    @NotNull(message = "执行指令不能为空")
    private CabinetConstants.LedMode mode;

    //执行指令 1 红灯 2绿灯 3 蓝灯
    @NotNull(message = "执行指令不能为空")
    private CabinetConstants.LedStatus status;

    //灯板id
    private Integer ledNum;

}
