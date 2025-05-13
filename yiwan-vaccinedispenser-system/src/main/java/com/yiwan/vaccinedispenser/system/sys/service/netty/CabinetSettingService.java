package com.yiwan.vaccinedispenser.system.sys.service.netty;


import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.CabinetSysResponse;

/**
 * @author slh
 * @date 2023/5/8
 * @Description
 * 系统参数业务逻辑
 */
public interface CabinetSettingService {

    //设置仓柜
    void setCabinet(WorkSettingRequest request);

    //设置IP地址和端口号
    void setIpPort(IPSettingRequest request);

    //设置步进电机参数
    void setStep(StepSettingData data);

    //设置伺服电机参数
    void setServo(ServoSettingData data);

    //设置时间参数
    void setTime(TimeSettingRequest request);

    //设置私有参数
    void setPrivate(PrivateSettingData request);





    //设置仓柜
    void getCabinet(WorkSettingRequest request);

    //设置IP地址和端口号
    void getIpPort(IPSettingRequest request);

    //获取版本号
    void getVersion(CabinetConstants.Cabinet workMode);


    //设置步进电机参数
    void getStep(StepSettingData data);

    //设置伺服电机参数
    void getServo(ServoSettingData data);

    //设置时间参数
    void getTime(TimeSettingRequest request);



    //设置私有参数
    void getPrivate(PrivateSettingData request);

    //获取所有参数
    void getAllSys(CabinetConstants.Cabinet workMode );


    //获取参数list
    CabinetSysResponse getAllSysList(CabinetConstants.Cabinet workMode);

}
