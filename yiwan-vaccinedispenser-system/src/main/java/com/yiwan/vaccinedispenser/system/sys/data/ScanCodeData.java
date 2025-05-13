package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/18 14:27
 */
@Data
public class ScanCodeData {
    //上方条形码
    private String aboveCode;
    //下方条形码
    private String belowCode;
    //侧方条形码
    private String sideCode;
    //条形码
    private String code;
    //伺服是否报警
    private Boolean isServoError;

}
