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
    private int productCount;
    //最新上药时间
    private LocalDate latestUpdateTime;
    //产品名称
    private String productName;

    //产品id
    private String productNo;

    //药品总数量
    private int totalVaccineNum;

    //机器状态 1 正常 2 多人份
    private int status;



}
