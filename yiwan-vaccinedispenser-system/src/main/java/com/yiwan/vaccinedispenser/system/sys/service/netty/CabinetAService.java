package com.yiwan.vaccinedispenser.system.sys.service.netty;


import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;

import java.util.List;
import java.util.Map;

/**
 * @author slh
 * @date 2023/5/8
 * @Description
 * A柜业务逻辑
 */
public interface CabinetAService {

    /**
     *掉药控制指令
     */
    void dropCommand(DropRequest request);

    /**
     *
     */
    void ledCommand(LedRequest request);

    /**
     *
     * @param request
     * A柜皮带伺服控制
     */
    void servo(CabinetAServoRequest request);




    /**
     *
     * @param request
     * A柜步进电机控制
     */
    void step(CabinetAStepRequest request);




    /**
     *
     * @param request
     * AB柜 输出
     */
     void outPut(OutPutRequest request);

    /**
     *
     * @param request
     * AB柜A类传感器输入
     */
    void intPut(InPutRequest request);


    //获取距离
    void getDistance(CabinetAGetDistanceRequest request);


    //获取A、B、C柜的所有传感器状态

    Map<String,String> getInputAll();

}
