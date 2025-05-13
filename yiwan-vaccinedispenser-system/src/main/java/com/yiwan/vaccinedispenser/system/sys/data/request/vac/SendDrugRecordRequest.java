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
public class SendDrugRecordRequest implements Serializable {


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
    //工作台编码
    private String workbenchNo;
    //工作台名称
    private String workbenchName;

    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;
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
    /**
     * 是否删除  true/已删除  false/未删除
     */
    private int deleted;

    //上药总数量
    private Integer totalNum;

}
