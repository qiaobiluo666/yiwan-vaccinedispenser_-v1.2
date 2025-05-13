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
 * <p>
 * 柜体表
 * </p>
 *
 * @author vicente
 * @since 2023-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacCabinet extends Model<VacCabinet> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 柜体名称
     */
    private String name;



    /**
     * 状态 1/正常
     */
    private Integer status;



    private String ip ;

    private Integer port;


    /**
     * 0为A柜 11为B柜
     */
    private Integer type;



    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;



    /**
     * 是否删除  true/已删除  false/未删除
     */
    private Boolean deleted;



    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
