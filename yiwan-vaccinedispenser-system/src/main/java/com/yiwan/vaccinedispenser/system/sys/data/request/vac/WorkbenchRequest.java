package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/6 9:01
 */
@Data
public class WorkbenchRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 5605713320595496734L;

    private Long id;

    //工作台编码
    private String workbenchNo;
    //工作台名称
    private String workbenchName;
    //工作台ID
    private Integer workbenchNum;

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
