package com.yiwan.vaccinedispenser.system.sys.data.request.sys;

import lombok.Data;

import javax.validation.constraints.Min;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/4/16 11:00
 */
@Data
public class SysConfigRequest {
    private Long id;
    //类别
    private String category;
    //类型
    private String configType;
    //名称
    private String configName;
    //值
    private String configValue;
    //描述
    private String descriptions;



    /**
     * 当前页
     */
    @Min(1)
    private Integer page;

    /**
     * 每页大小
     */
    @Min(1)
    private Integer size;








}
