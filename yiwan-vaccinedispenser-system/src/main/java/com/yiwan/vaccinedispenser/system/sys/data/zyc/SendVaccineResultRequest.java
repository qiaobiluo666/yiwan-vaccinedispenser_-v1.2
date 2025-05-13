package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc 发苗结果
 * @date 2024/5/17 9:34
 */
@Data
public class SendVaccineResultRequest {
    //原请求ID
    private String requestNo;
    //指令执行结果
    private List<SendVaccineResultData> result;
}
