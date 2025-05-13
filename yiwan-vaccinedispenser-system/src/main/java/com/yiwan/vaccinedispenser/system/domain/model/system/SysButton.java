package com.yiwan.vaccinedispenser.system.domain.model.system;

import lombok.Data;
import lombok.ToString;

/**
 * @author 78671
 */
@Data
@ToString
public class SysButton {

    private Long id;
    //按钮名称
    private String buttonName;
    //描述
    private String description;
    //字典项
    private String buttonDict;
    //状态  0 正常
    private Integer status;
    //创建时间
    private java.util.Date createTime;
    //创建人
    private Long createBy;
}
