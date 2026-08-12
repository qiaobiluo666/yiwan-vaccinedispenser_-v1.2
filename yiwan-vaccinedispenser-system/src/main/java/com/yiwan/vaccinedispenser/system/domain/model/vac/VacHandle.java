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
 * @desc 药盒步进配置
 * @date 2026/06/30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacHandle extends Model<VacHandle> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 药盒最小长度
     */
    private Integer lenMin;

    /**
     * 药盒最大长度
     */
    private Integer lenMax;

    /**
     * 步进伸出距离
     */
    private Integer stepExtendDis;

    /**
     * 步进升高距离
     */
    private Integer stepLiftDis;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    /**
     * 0:正常 1:删除
     */
    private int deleted;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
