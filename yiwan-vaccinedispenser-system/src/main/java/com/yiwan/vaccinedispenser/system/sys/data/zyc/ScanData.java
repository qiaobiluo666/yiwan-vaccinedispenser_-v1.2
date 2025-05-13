package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc 扫码信息
 * @date 2024/6/3 16:55
 */
@Data
public class ScanData {
    //批次
    private String batchNo;
    //有效期
    private Date expiredAt;
    //价格
    private Long price;
    //产品编码
    private String productNo;
    //电子监管码
    private String supervisedCode;
    //标签
    private  String tag;

}
