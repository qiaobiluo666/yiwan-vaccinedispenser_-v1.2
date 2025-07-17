package com.yiwan.vaccinedispenser.system.sys.data;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/11/28 15:43
 */
@Data
public class VaccineData {
    //疫苗名称
    private String productName;

    //数据库的长宽高
    private Integer drugWide;
    private Integer drugHigh;
    private Integer drugLong;

    //实际的长宽高
    private Integer realWide;
    private Integer realHigh;
    private Integer realLong;

    //是否符合
    private Boolean isRight;

}
