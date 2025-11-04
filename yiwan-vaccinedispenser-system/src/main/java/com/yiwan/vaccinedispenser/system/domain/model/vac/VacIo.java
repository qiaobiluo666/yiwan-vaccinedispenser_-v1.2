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
 * @author 78671
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacIo extends Model<VacIo> {


    @Serial
    private static final long serialVersionUID = 3524272153186605897L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    private Integer lenMin;

    private Integer lenMax;

    private Integer ioTime;

    
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
