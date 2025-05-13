package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/28 16:24
 */
@Data
public class MachineExceptionRequest {
    //id
    private Long id;
    //异常码    1 IO超时 2 皮带超时 3 伺服报警 4 自动上药报警
    private Integer code;
    //药仓编号
    private String boxNo;
    //层数
    private Integer lineNum;
    //工作台
    private Integer workNum;
    //存放药品名称
    private String drugName;
    //错误描述
    private String description;

    //当前页
    @Min(1)
    private Integer page;

    //每页大小
    @Min(1)
    private Integer size;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    //是否删除  true/已删除  false/未删除
    private int deleted;
}
