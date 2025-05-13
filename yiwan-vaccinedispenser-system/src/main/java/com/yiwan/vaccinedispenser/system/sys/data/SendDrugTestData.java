package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/26 11:25
 */
@Data
public class SendDrugTestData {
    //是否是左边是长边
    private Boolean isLeft;
    //是否要重新返回发药
    private Boolean isReturn;
    //上方是否扫的到
    private Boolean isAbove;
    //下方是否扫的到
    private Boolean isBelow;
    //侧边是否扫的到
    private Boolean isSide;







}
