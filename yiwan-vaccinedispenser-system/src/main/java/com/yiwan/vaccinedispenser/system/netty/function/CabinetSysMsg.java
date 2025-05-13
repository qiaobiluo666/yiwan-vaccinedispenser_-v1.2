package com.yiwan.vaccinedispenser.system.netty.function;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineSys;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineSysMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.MachineSysRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacCabinetService;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineSysService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;


/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/11 11:12
 */
@Slf4j
@Component
public class CabinetSysMsg {
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Autowired
    private VacMachineSysService vacMachineSysService;


    @Autowired
    private VacCabinetService vacCabinetService;
    /**
     *
     * @param bytesStr
     * 读取系统参数接收信息
     */

    public  void receiveMsgCabinetSys(CabinetConstants.Cabinet workMode, String[] bytesStr) {
        //基础参数
        int cabinet = Integer.parseInt(bytesStr[6],16);
        int command = Integer.parseInt(bytesStr[8],16);
        int mode = Integer.parseInt(bytesStr[9],16);
        int status = Integer.parseInt(bytesStr[10],16);
        VacMachineSys vacMachineSys = new VacMachineSys();
        vacMachineSys.setWorkMode(workMode.num);
        vacMachineSys.setCabinet(cabinet);
        vacMachineSys.setCommand(command);
        vacMachineSys.setMode(mode);
        vacMachineSys.setStatus(status);
        UpdateWrapper<VacMachineSys> updateWrapper = new UpdateWrapper<VacMachineSys>()
                .eq("work_mode",workMode.num)
                .eq("cabinet",cabinet)
                .eq("command",command)
                .eq("mode",mode)
                .eq("status",status);

        switch (bytesStr[8]) {

            //0x01 -系统参数
            case "01"->{
                vacMachineSys = receiveSys(vacMachineSys,bytesStr);
                if(vacMachineSys!=null){
                    if("02".equals(bytesStr[9])){
                        String ipType = valueOperations.get(RedisKeyConstant.IP_SET);
                        Integer type;
                        if(ipType!=null){
                            type= Integer.parseInt(ipType);
                            updateWrapper = new UpdateWrapper<VacMachineSys>()
                                    .eq("work_mode",type)
                                    .eq("cabinet",cabinet)
                                    .eq("command",command)
                                    .eq("mode",mode)
                                    .eq("status",status);
                            vacMachineSys.setWorkMode(type);
                            vacMachineSys.setCabinet(type);
                            //删除IP
                            redisTemplate.delete(RedisKeyConstant.IP_SET);
                        }else {
                            type = cabinet;
                        }

                        vacCabinetService.updateVacCabinet(type,vacMachineSys.getIp());

                    }
                    vacMachineSysService.saveOrUpdate(vacMachineSys,updateWrapper);
                }
            }
            //0x02-步进电机参数
            case "02","03"->{
                vacMachineSys = receiveStepOrServo(vacMachineSys,bytesStr);
                if(vacMachineSys!=null){
                    vacMachineSysService.saveOrUpdate(vacMachineSys,updateWrapper);
                }
            }

            //0x04 - 时间参数
            case "04"->{
                vacMachineSys = receiveTime(vacMachineSys,bytesStr);
                if(vacMachineSys!=null){
                    vacMachineSysService.saveOrUpdate(vacMachineSys,updateWrapper);
                }
            }

            //0x05 - 私有参数
            case "05" ->{
                vacMachineSys = receiveMsg(vacMachineSys,bytesStr);
                if(vacMachineSys!=null){
                    vacMachineSysService.saveOrUpdate(vacMachineSys,updateWrapper);
                }

            }

        }

    }



    /**
     *
     * @param bytesStr
     *  系统参数信息处理
     */
    public VacMachineSys receiveSys(VacMachineSys vacMachineSys, String[] bytesStr){

        switch (bytesStr[9]){
            //0x01-工作模式
            case "01"->{
                vacMachineSys.setWorkType(Integer.parseInt(bytesStr[11],16));
                return vacMachineSys;
            }
            //0x02-IP设置
            case "02"->{
                //ip地址
                String ip = Integer.parseInt(bytesStr[11],16)
                        +"." +Integer.parseInt(bytesStr[12],16)
                        +"."+Integer.parseInt(bytesStr[13],16)
                        +"."+Integer.parseInt(bytesStr[14],16);
                //端口号
                Integer port =NettyUtils.parseHexStringArrayInt(bytesStr, 15,2);
                vacMachineSys.setIp(ip);
                vacMachineSys.setPort(port);
                return vacMachineSys;
            }
            //0x03-版本号
            case "03"->{
                StringBuilder versionBuilder = new StringBuilder();
                versionBuilder.append(Integer.parseInt(bytesStr[11], 16))
                        .append(".")
                        .append(Integer.parseInt(bytesStr[12], 16))
                        .append(".")
                        .append(Integer.parseInt(bytesStr[13], 16))
                        .append(".")
                        .append(Integer.parseInt(bytesStr[14], 16))
                        .append(" ")
                        .append(Integer.parseInt(bytesStr[15], 16))
                        .append(Integer.parseInt(bytesStr[16], 16));

                int version17 = Integer.parseInt(bytesStr[17], 16);
                int version18 = Integer.parseInt(bytesStr[18], 16);

                if (version17 < 10) {
                    versionBuilder.append("0");
                }
                versionBuilder.append(version17);

                if (version18 < 10) {
                    versionBuilder.append("0");
                }
                versionBuilder.append(version18);

                String version = versionBuilder.toString();
                vacMachineSys.setVersion(version);
                return vacMachineSys;
            }

            default -> {
                return null;
            }
        }


    }



    /**
     *
     * @param bytesStr
     *  伺服或是步进信息处理
     */
    public VacMachineSys receiveStepOrServo(VacMachineSys vacMachineSys, String[] bytesStr){
        vacMachineSys.setPulse(NettyUtils.parseHexStringArray(bytesStr, 11,4));
        vacMachineSys.setDistance(NettyUtils.parseHexStringArray(bytesStr, 15,4));
        vacMachineSys.setMaxDistance(NettyUtils.parseHexStringArray(bytesStr, 19,4));
        vacMachineSys.setSpeed(NettyUtils.parseHexStringArray(bytesStr, 23,2));
        vacMachineSys.setReturnSpeed(NettyUtils.parseHexStringArray(bytesStr, 25,2));
        vacMachineSys.setAccelerationTime(NettyUtils.parseHexStringArray(bytesStr, 27,2));
        vacMachineSys.setDecelerationTime(NettyUtils.parseHexStringArray(bytesStr, 29,2));
        vacMachineSys.setAcceleration(NettyUtils.parseHexStringArray(bytesStr, 31,2));
        vacMachineSys.setZeroSwitch(Integer.parseInt(bytesStr[32],16));
        vacMachineSys.setZero(Integer.parseInt(bytesStr[33],16));
        return vacMachineSys;
    }

    /**
     *
     * @param bytesStr
     *  超时时间信息处理
     */
    public VacMachineSys receiveTime(VacMachineSys vacMachineSys, String[] bytesStr){
        vacMachineSys.setTimeLong(NettyUtils.parseHexStringArray(bytesStr, 11,2));
        return vacMachineSys;
    }


    /**
     *
     * @param bytesStr
     *  超时时间信息处理
     */
    public VacMachineSys receiveMsg(VacMachineSys vacMachineSys, String[] bytesStr){

        //如果是A的私有参数
        if( Integer.parseInt(bytesStr[9],16)==1){
            //方向数据 0 前进 1 后退
            vacMachineSys.setZero(Integer.parseInt(bytesStr[11],16));
            //运动距离
            vacMachineSys.setDistance(NettyUtils.parseHexStringArray(bytesStr, 12,4));
        //B柜的私有参数
        }else if(Integer.parseInt(bytesStr[9],16)==2){
            vacMachineSys.setDistance(NettyUtils.parseHexStringArray(bytesStr, 11,2));
        //C柜私有参数
        }else {
            vacMachineSys.setDistance(NettyUtils.parseHexStringArray(bytesStr, 11,4));
        }
        return vacMachineSys;
    }

}
