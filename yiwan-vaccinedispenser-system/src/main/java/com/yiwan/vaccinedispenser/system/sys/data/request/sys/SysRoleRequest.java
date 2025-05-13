package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @author zhw
 * @date 2023/9/15
 * @Description
 */
@Data
public class SysRoleRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 6471260769192595584L;

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

    @Min(1)
    private Integer page;

    @Min(1)
    private Integer size;

}
