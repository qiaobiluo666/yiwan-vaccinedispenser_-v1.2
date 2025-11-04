package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/6 9:01
 */
@Data
public class LockerInfoRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 5605713320595496734L;

    //仓位数据
    List<VacMachineRequest> dataList;

    //自动上药状态
    String  sendStatus;

    //异常清理状态
    String  errorCleanStatus;

    //库存盘点状态
    String  inventoryStatus;

    //医院名称
    String hospitalName;

}
