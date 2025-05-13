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
 * @date 2024/2/20 0020 14:27
 * 掉药命令
 */
@Data
public class DropRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 7277815937747321540L;

    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //站号 第几块IO拓展板
    @Min(value = 1, message = "站号不能小于1")
    @NotNull(message = "站号不能为空")
    private Integer command;

    //执行指令 1 输出 2不输出 3 查询 4 自动
    @NotNull(message = "执行指令不能为空")
    private CabinetConstants.IOMode mode;

    //20个io口的指令  一共5个字节
    private List<Integer> ioList;

    //IO号
    private Integer ioNum;

    //开启时间
    private Integer times;

}
