package com.yiwan.vaccinedispenser.system.domain.model.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@EqualsAndHashCode(callSuper = false)
public class VacGetVaccine extends Model<VacGetVaccine> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;



    /**
     * 请求Id
     */

    private String requestNo;

    /**
     * 任务Id
     */

    private String taskId;

    /**
     * 产品编码
     */
    private String productNo;

    /**
     * 产品名称
     */
    private String productName;


    /**
     * 疫苗价格
     */
    private String price;

    /**
     * 标签 使用场景标签 01 民生
     */
    private String tag;

    /**
     * '-1 未发药 0 发药中 1 发药完成'
     */
    private String status;



    /**
     * 工作台编码
     */

    private String workbenchNo;


    /**
     * 工作台名称
     */
    private String workbenchName;

    /**
     * 工作台ID
     */
    private Integer workbenchNum;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    /**
     * 是否删除  true/已删除  false/未删除
     */
    private int deleted;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
