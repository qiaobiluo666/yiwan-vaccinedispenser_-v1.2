package com.yiwan.vaccinedispenser.system.domain.model.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/5 18:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacBoxSpec extends Model<VacBoxSpec> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    /**
     * 规格名称
     */

    private String name;


    /**
     * 药仓宽度
     */

    private Integer length;


    /**
     * 疫苗宽度范围 length-range-length 都可以进这个药仓
     */

    private Integer ranges;




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
