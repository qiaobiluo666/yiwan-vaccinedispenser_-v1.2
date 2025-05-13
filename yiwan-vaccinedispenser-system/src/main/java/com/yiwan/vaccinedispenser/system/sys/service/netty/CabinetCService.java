package com.yiwan.vaccinedispenser.system.sys.service.netty;


import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;

/**
 * @author slh
 * @date 2023/5/8
 * @Description
 * A柜业务逻辑
 */
public interface CabinetCService {

    //药指令
    void sendDrug(CabinetCSendDrugRequest request);

    //伺服
    void servo(CabinetCServoRequest request);

    //步进 （长皮带伺服 脉冲）
    void step(CabinetCStepRequest request);

}
