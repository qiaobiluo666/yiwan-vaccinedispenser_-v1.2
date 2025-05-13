package com.yiwan.vaccinedispenser.system.sys.data.request.netty;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @date 2024/2/20 0020 17:47
 * IP参数设置
 */
@Data
public class IPSettingRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1629674474525550937L;


    //工作模式-对应的ab板子 给哪块板子发指令
    @NotNull(message = "工作模式-对应的ab板子")
    private CabinetConstants.Cabinet workMode;

    //选择柜子的指令
    @NotNull(message = "哪个柜子不能为空")
    private CabinetConstants.Cabinet cabinet;

    /**
     * ip地址
     *  */
    @ApiModelProperty(value = "ip地址",dataType = "String",name = "ip")
    @NotBlank(message = "ip地址不能为空")
    private String ip;

    /**
     * 端口号
     *  */
    @ApiModelProperty(value = "端口号",dataType = "String",name = "port")
    @NotNull(message = "端口号不能为空")
    private Integer port;

}
