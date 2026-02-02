package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import java.util.List;

@Data
public class MachineInventoryRequest {

    //库存盘点指定的仓位列表
    List<String> boxNoList;

    //库存盘点指定的行列表
    List<Integer>  lineList;



}
