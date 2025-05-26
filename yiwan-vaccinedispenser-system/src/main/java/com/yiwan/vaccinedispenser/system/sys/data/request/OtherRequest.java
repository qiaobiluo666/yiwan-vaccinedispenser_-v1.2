package com.yiwan.vaccinedispenser.system.sys.data.request;

import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/29 10:24
 */
@Data
public class OtherRequest {

    private Integer type;
    private CabinetConstants.Cabinet workMode;
    private Integer ledLine;
    private Integer ledNum;
    private Integer ioNumStart;
    private Integer ioNumEnd;
    private Integer count;
    private Integer time;
    private Integer ioLine;
    private Integer ioWaitTime;
    private Integer cabinetNumStart;
    private Integer cabinetNumEnd;
    private Integer cabinetLine;
    private Integer cabinetWaitTime;

    //机械手层数
    private Integer handLine;
    //第一个仓位X数据
    private Integer autoXOne;

    //灯与灯的间隔时间
    private Integer ledTime;


}
