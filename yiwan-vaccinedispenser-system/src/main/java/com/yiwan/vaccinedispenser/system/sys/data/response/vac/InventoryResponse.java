package com.yiwan.vaccinedispenser.system.sys.data.response.vac;

import lombok.Data;

import java.time.LocalDate;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/5/27 9:30
 */
@Data
public class InventoryResponse {

    //药品id
    private Long vaccineId;
    //有多少个仓位
    private Integer productCount;
    //最新上药时间
    private LocalDate latestUpdateTime;

    //疫苗种类
    private String vaccineMinorName;

    //厂家
    private String manufacturerName;


    //产品名称
    private String productName;

    //产品id
    private String productNo;

    //药品总数量
    private Integer totalVaccineNum;

    //机器状态 1 正常 2 多人份
    private Integer status;

    //今日上苗数量
    private Integer sendDrugNum;

    //今日发苗数量
    private Integer useDrugNum;

}
