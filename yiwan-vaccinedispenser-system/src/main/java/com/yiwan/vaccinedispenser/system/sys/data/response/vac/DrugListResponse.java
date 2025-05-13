package com.yiwan.vaccinedispenser.system.sys.data.response.vac;

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
public class DrugListResponse implements Serializable {


    @Serial
    private static final long serialVersionUID = 5605713320595496734L;

    /**
     * 疫苗名称
     */

    private String name;

    /**
     * 疫苗长度
     */

    private String drugLong;


    /**
     * 疫苗宽度
     */

    private String drugWide;


    /**
     * 疫苗高度
     */

    private String drugHigh;


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
