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
public class BoxSpecListRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 5605713320595496734L;


    private Long id;


    /**
     * 规格名称
     */

    private String name;


    /**
     * 药仓宽度
     */

    private Integer length;


    /**
     * 疫苗宽度范围 length-range-length 都可以进这个药仓
     */

    private Integer ranges;



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
