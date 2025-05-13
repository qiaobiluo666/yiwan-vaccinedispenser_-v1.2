package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/5/17 9:36
 */
@Data
public class SendVaccineResultData {
    //任务ID
    private String taskId;
    //0 失败  1发苗成功
    private String sendResult;
    //失败原因 1：发苗机异常 2、无库存 3、其他问题
    private String failReason;
}
