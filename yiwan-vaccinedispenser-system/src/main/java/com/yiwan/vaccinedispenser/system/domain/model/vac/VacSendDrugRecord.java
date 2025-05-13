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
public class VacSendDrugRecord extends Model<VacSendDrugRecord> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;




    /**
     * 仓位id
     */

    private Long machineId;

    /**
     * 仓位名称
     */

    private String machineNo;

    /**
     * 疫苗id
     */

    private Long vaccineId;

    /**
     * 产品名称
     */

    private String productName;

    /**
     * 产品编码
     */

    private String productNo;

    /**
     * 电子监管码
     */

    private String supervisedCode;


    /**
     * 疫苗批号
     */

    private String batchNo;


    /**
     * 批次有效期
     */

    private Date expiredAt;



    /**
     * 疫苗价格
     */
    private String price;



    /**
     * 标签 使用场景标签 01 民生
     */
    private String tag;

    /**
     * 工作台编码
     */
    private String workbenchNo;


    /**
     * 工作台名称
     */
    private String workbenchName;


    /**
     * 出药状态  正常 1 不正常0
     */
    private Integer status;

    /**
     * 错误描述
     */

    private String description;


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
