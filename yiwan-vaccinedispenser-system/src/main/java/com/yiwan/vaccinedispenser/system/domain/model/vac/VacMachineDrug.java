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
 * @desc 仓柜配置
 * @date 2024/3/5 18:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacMachineDrug extends Model<VacMachineDrug> {
    @Serial
    private static final long serialVersionUID = 4943446872315881649L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    //机器id
    private Long machineId;
    //药品id
    private Long vaccineId;

    //散装数量
    private  int num;

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
