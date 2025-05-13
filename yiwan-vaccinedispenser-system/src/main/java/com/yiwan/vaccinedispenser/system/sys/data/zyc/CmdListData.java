package com.yiwan.vaccinedispenser.system.sys.data.zyc;

import lombok.Data;

import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/6/7 10:19
 */
@Data
public class CmdListData {
    //任务ID
    private String taskId;
    //疫苗产品编码List
    List<String> productNos;
    //价格
    private String price;
    //标签
    private String tag;
}
