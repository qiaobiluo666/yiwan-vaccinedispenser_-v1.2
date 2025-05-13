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
    String productName;

    //数据库的长宽高
    Integer drugWide;
    Integer drugHigh;
    Integer drugLong;

    //实际的长宽高
    Integer realWide;
    Integer realHigh;
    Integer realLong;

    //是否符合
    Boolean isRight;

}
