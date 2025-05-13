package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author 78671
 */
@Data
public class SendBtnData {
    //产品名称
    private String productNo;
    //产品名称
    private String productName;
    //厂家
    private String manufacturerName;
    //余量
    private  Integer totalNum;


}
