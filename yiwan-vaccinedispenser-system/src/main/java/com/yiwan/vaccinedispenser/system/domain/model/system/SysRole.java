package com.yiwan.vaccinedispenser.system.domain.model.system;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 
 * 
 */
@Data
public class SysRole implements Serializable {
    private Long id;

    private String roleName;

    /**
     * 描述
     */
    private String descriptions;

    /**
     * 上级角色id
     */
    private Long pid;

    /**
     * 上级角色名称
     */
    private String pname;

    /**
     * 1：超级管理员层；2：管理员层：3：业务角色层
     */
    private String level;

    /**
     * 创建人 
     */
    private String createdBy;

    /**
     * 0  对外开放         1  不对外开放
     */
    private String types;

    /**
     * 状态 0/关闭  1/正常
     */
    private Integer status;

    private Date createTime;

    @TableLogic
    private Boolean deleted;

    private static final long serialVersionUID = 1L;
}