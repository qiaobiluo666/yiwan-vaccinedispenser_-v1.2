package com.yiwan.vaccinedispenser.system.sys.service.netty;


import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetBApplyRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetBGetDistanceRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetBServoRequest;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.CabinetBStepRequest;

/**
 * @author slh
 * @date 2023/5/8
 * @Description
 * A柜业务逻辑
 */
public interface CabinetBService {
    //自动上药
    void apply(CabinetBApplyRequest request);

    //获取距离
    void getDistance(CabinetBGetDistanceRequest request);


    //伺服
    void servo(CabinetBServoRequest request);

    //步进电机
    void step(CabinetBStepRequest request);



}
