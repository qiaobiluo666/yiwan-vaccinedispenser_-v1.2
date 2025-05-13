package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 16:33
 * 输出控制指令
 */
@Data
public class OutPutRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -5913101228474935230L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    @NotNull(message = "哪个柜子不能为空")
    private CabinetConstants.Cabinet cabinet;

    //执行指令 1 输出 2不输出 3 查询
    @NotNull(message = "输出控制指令不能为空")
    private CabinetConstants.OutPutCommand command;

    @NotNull(message = "选择第几个输出控制")
    private Integer mode;

}
