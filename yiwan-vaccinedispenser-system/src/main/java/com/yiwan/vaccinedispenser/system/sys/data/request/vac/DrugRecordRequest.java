package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/5 18:45
 */
@Data
public class DrugRecordRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 9169335448182023593L;

    private Long id;
    //仓位id
    private Long machineId;
    //仓位名称
    private String machineNo;
    //疫苗id
    private Long vaccineId;
    //产品名称
    private String productName;
    //产品编码
    private String productNo;
    //电子监管码
    private String supervisedCode;
    //疫苗批号
    private String batchNo;

    //批次有效期
    private Date expiredAt;

    //创建 开始
    private Date createTimeStart;

    //创建 结束
    private Date createTimeEnd;

    //疫苗价格
    private String price;

    //标签 使用场景标签 01 民生
    private String tag;

    //是否在仓位  0在仓位 1不在仓位  2退药
    private String status;

    //机械手位置X
    private Integer autoX;

    //机械手位置Z
    private Integer autoZ;

    //存放药品数量
    private Integer vaccineNum;
    //发药可使用量
    private Integer vaccineUseNum;

    //是否要返回滑台
    private Boolean isReturn;

    //信息
    private String msg;


    //led位置
    private Integer ledNum;

    //层数
    private Integer lineNum;

    /**
     * 当前页
     */
    @Min(1)
    private Integer page;

    /**
     * 每页大小
     */
    @Min(1)
    private Integer size;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    /**
     * 是否删除  true/已删除  false/未删除
     */
    private int deleted;



}
