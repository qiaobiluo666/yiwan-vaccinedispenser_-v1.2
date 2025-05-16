package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/8 15:36
 */
@Data
public class RedisDrugListData {

    //taskId
    private String taskId;

    private String requestNo;

    private String boxNo;



    //药仓id
    private Long machineId;

    //药仓编号
    private String machineNo;

    //层数
    private Integer lineNum;
    //位置
    private Integer positionNum;



    //产品名称
    private String productName;
    //产品编号
    private String productNo;

    //批号
    private String batchNo;

    private Date expiredAt;

    //第几层皮带
    private Integer beltNum;

    //工作台
    private Integer workbenchNum;

    //工作台编码
    private String workbenchNo;

    //工作台名称
    private String workbenchName;

    //uuid
    private UUID uuid;


    //机器状态 1 正常 2 多人份
    private int machineStatus;

}
