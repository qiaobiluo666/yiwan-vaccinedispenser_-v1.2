package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc 获取疫苗处方合集
 * @date 2024/6/7 9:51
 */
@Data
public class GetVaccineMsgData {
    //请求ID
    private String requestNo;
    //工作台编码
    private String workbenchNo;
    //取苗指令集合
    private List<CmdListData> cmdList;

}
