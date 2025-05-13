package com.yiwan.vaccinedispenser.system.domain.model.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * @author slh
 * @version 1.0
 * @desc 仓柜配置
 * @date 2024/3/5 18:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacMachineException extends Model<VacMachineException> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 异常码    1 IO超时 2 皮带超时 3 伺服报警 4 自动上药报警
     */

    private Integer code;

    /**
     * 药仓编号
     */

    private String boxNo;


    /**
     * 层数
     */

    private Integer lineNum;


    /**
     * 工作台
     */

    private Integer workNum;



    /**
     * 存放药品名称
     */

    private String drugName;

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
