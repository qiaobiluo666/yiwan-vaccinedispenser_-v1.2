package com.yiwan.vaccinedispenser.system.domain.model.system;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @author 
 * 系统配置表
 */
@Data
public class SysConfig implements Serializable {
    private Long id;

    /**
     * 类别
     */

    private String category;


    /**
     * 类型
     */
    private String configType;

    /**
     * 名称
     */
    private String configName;

    /**
     * 值
     */
    private String configValue;

    private String descriptions;

    private Date createTime;

    private String createBy;

    private Date updateTime;

    private String updateBy;

    @TableLogic
    private Boolean deleted;

    @Serial
    private static final long serialVersionUID = 1L;
}