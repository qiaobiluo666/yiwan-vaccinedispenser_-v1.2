package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/9/6 10:52
 */
@Data
public class InventoryReportData {

    //疫苗产品编码
    private String productNo;
    // 疫苗批号
    private String batchNo;
    //疫苗价格
    private String price;
    //是否民生
    private  Integer livelihood;
    //库存数量
    private Long quantity;

}
