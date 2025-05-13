package com.yiwan.vaccinedispenser.system.domain.model.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

import static com.baomidou.mybatisplus.annotation.IdType.NONE;

/**
 * @author 
 * 后台用户表
 */
@Data
public class SysUser implements Serializable {

    @TableId(type = NONE)
    private Long id;

    /**
     * 手机号
     */
    @TableField(value = "mobile")
    private String mobile;

    /**
     * 真实姓名
     */
    @TableField(value = "real_name")
    private String realName;

    /**
     * 用户名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 邮箱
     */
    @TableField(value = "mailbox")
    private String mailbox;

    /**
     * 省份code
     */
    @TableField(value = "province_code")
    private Integer provinceCode;

    /**
     * 省份
     */
    @TableField(value = "province")
    private String province;

    /**
     * 城市code
     */
    @TableField(value = "city_code")
    private Integer cityCode;

    /**
     * 城市
     */
    @TableField(value = "city")
    private String city;

    /**
     * 状态 0 解冻 1  冻结
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 0/对外开放   1/不对外开放
     */
    @TableField(value = "types")
    private Integer types;

    /**
     * 医院部门s
     */
    @TableField(value = "hos_department_ids")
    private String hosDepartmentIds;

    @TableField(value = "api_hos_area_id")
    private String apiHosAreaId;

    @TableField(value = "area_floor_id")
    private Long areaFloorId;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "last_login_time")
    private Date lastLoginTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 修改人
     */
    @TableField(value = "create_by")
    private String createBy;

    /**
     * 修改人
     */
    @TableField(value = "update_by")
    private String updateBy;

    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    private static final long serialVersionUID = 1L;
}