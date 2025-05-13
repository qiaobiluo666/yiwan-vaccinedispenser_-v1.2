package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/9/5 12:59
 */
@Data
public class VaccineCodeData {
    //监管码
    List<String> result;
    //是否成功
    Boolean success;
    //报错信息
    String  msg;
}
