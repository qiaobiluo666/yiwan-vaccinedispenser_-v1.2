package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import cn.hutool.core.date.DateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/6 9:01
*/
@Data
public class MachineListRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 5605713320595496734L;

    private Long id;
    //层数
    private Integer lineNum;
    //位置
    private Integer positionNum;
    //药仓编号
    private String boxNo;
    //药仓规格id
    private Long boxSpecId;
    //药仓规格名称
    private String boxSpecName;
    //药仓状态
    private Integer status;
    //药仓存放最大值
    private String boxMax;
    //药品id
    private Long vaccineId;

    //产品名称
    private String productName;

    //产品名称
    private String productNo;

    //药品数量
    private Integer vaccineNum;

    //发药可使用量
    private Integer vaccineUseNum;


    @JsonFormat(pattern = "yyyy-MM-dd")
    //药品保质期
    private Date expiredAt;

    private String batchNo;

    //灯板Id
    private Integer ledNum;

    //自动上药X
    private Integer autoX;
    //自动上药Z
    private Integer autoZ;


    //自动盘点X
    private Integer countX;
    //自动盘点Z
    private Integer countZ;

    //批量增加的数量
    private Integer addNum;
    //当前页
    @Min(1)
    private Integer page;
    //每页大小
    @Min(1)
    private Integer size;



}
