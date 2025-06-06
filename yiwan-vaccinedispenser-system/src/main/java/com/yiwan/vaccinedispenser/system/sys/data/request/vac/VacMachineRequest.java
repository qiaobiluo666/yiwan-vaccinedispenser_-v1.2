package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author 78671
 */
@Data
public class VacMachineRequest {

    private Long id;
    //疫苗是否全部退回
    private Boolean backAll;
    // 层数
    private Integer lineNum;

    //位置
    private Integer positionNum;

    // 药仓编号
    private String boxNo;


    //药仓规格id
    private Long boxSpecId;

    // 药仓规格名称

    private String boxSpecName;


    // 药仓状态  0 关闭 1 正常
    private Integer status;


    // 药仓存放最大值
    private String boxMax;


    // 药品id
    private Long vaccineId;

    // 产品名称
    private String productName;

    // 产品编码
    private String productNo;


    //存放药品数量
    private Integer vaccineNum;


    // 发药可使用量
    private Integer vaccineUseNum;


    // 批次有效期
    private Date expiredAt;

    //疫苗批号
    private String batchNo;

    //灯板Id
    private Integer ledNum;


    // 自动上药X
    private Integer autoX;

    //自动上药Z
    private Integer autoZ;


    //库存盘点X


    private Integer countX;

    //库存盘点Z
    private Integer countZ;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    //是否删除  true/已删除  false/未删除
    private int deleted;
}
