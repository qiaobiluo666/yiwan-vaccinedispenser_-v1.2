package com.yiwan.vaccinedispenser.system.sys.service.netty.impl;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.config.FrameNumberConfig;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacMachineSys;
import com.yiwan.vaccinedispenser.system.netty.msg.NettySendService;
import com.yiwan.vaccinedispenser.system.sys.dao.VacMachineSysMapper;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.data.response.vac.CabinetSysResponse;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetSettingService;
import com.yiwan.vaccinedispenser.system.until.CRC16Modbus;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/2/28 15:59
 */
@Service
@Slf4j
public class CabinetSettingServiceImpl implements CabinetSettingService {

    @Autowired
    private NettySendService nettySendService;

    @Autowired
    private VacMachineSysMapper vacMachineSysMapper;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    @Override
    public void setCabinet(WorkSettingRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SETTING.num;
        int mode = CabinetConstants.SettingMode.WORK.num;
        int status = 0;
        int setCabinet = request.getSetCabinet().num;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_WORK.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(NettyUtils.intToHexString(setCabinet,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getCabinet().desc, CabinetConstants.SettingMode.WORK.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void setIpPort(IPSettingRequest request) {
        valueOperations.set(RedisKeyConstant.IP_SET, String.valueOf(request.getCabinet().num));
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SETTING.num;
        int mode = CabinetConstants.SettingMode.IP.num;
        int status = 0;
        int ip1 = Integer.parseInt(request.getIp().split("\\.")[0]);
        int ip2 = Integer.parseInt(request.getIp().split("\\.")[1]);
        int ip3 = Integer.parseInt(request.getIp().split("\\.")[2]);
        int ip4 = Integer.parseInt(request.getIp().split("\\.")[3]);
        int port = request.getPort();
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_IP.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(NettyUtils.intToHexString(ip1,1));
        stringBuilder.append(NettyUtils.intToHexString(ip2,1));
        stringBuilder.append(NettyUtils.intToHexString(ip3,1));
        stringBuilder.append(NettyUtils.intToHexString(ip4,1));
        stringBuilder.append(NettyUtils.intToHexString(port,2));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getCabinet().desc, CabinetConstants.SettingMode.IP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void setStep(StepSettingData data) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = data.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.STEP.num;
        //第几个步进电机
        int mode =data.getMode();
        //设置所有参数
        int status =0;
        //单圈脉冲
        BigInteger pulse = data.getPulse();
        //单圈距离
        BigInteger distance = data.getDistance();
        //最大运行距离
        BigInteger maxDistance = data.getMaxDistance();
        //速度(%)
        BigInteger speed = data.getSpeed();
        //回原速度(%)
        BigInteger returnSpeed = data.getReturnSpeed();
        //加速度时间
        BigInteger accelerationTime = data.getAccelerationTime();
        //减速度时间
        BigInteger decelerationTime = data.getDecelerationTime();
        //加加速度
        BigInteger acceleration = data.getAcceleration();
        //原点信号开关
        int zeroSwitch = data.getZeroSwitch();
        //原点方向
        int zero = data.getZero();
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_STEP.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));

        stringBuilder.append(NettyUtils.intToHexString(pulse,4));

        stringBuilder.append(NettyUtils.intToHexString(distance,4));
        stringBuilder.append(NettyUtils.intToHexString(maxDistance,4));
        stringBuilder.append(NettyUtils.intToHexString(speed,2));
        stringBuilder.append(NettyUtils.intToHexString(returnSpeed,2));
        stringBuilder.append(NettyUtils.intToHexString(accelerationTime,2));
        stringBuilder.append(NettyUtils.intToHexString(decelerationTime,2));
        stringBuilder.append(NettyUtils.intToHexString(acceleration,2));
        stringBuilder.append(NettyUtils.intToHexString(zeroSwitch,1));
        stringBuilder.append(NettyUtils.intToHexString(zero,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString());
        log.info("{}柜-{}：{}", data.getCabinet().desc, CabinetConstants.SettingCommand.STEP.desc,HexUtil.format(stringBuilder.toString()));
        //给B控制板 发送消息
        nettySendService.sendMsg(data.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void setServo(ServoSettingData data) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = data.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SERVO.num;
        //第几个步进电机
        int mode =data.getMode();
        //设置所有参数
        int status =0;
        //单圈脉冲
        BigInteger pulse = data.getPulse();
        //单圈距离
        BigInteger distance = data.getDistance();
        //最大运行距离
        BigInteger maxDistance = data.getMaxDistance();
        //速度(%)
        BigInteger speed = data.getSpeed();
        //回原速度(%)
        BigInteger returnSpeed = data.getReturnSpeed();
        //加速度时间
        BigInteger accelerationTime = data.getAccelerationTime();
        //减速度时间
        BigInteger decelerationTime = data.getDecelerationTime();
        //加加速度
        BigInteger acceleration = data.getAcceleration();
        //原点信号开关
        int zeroSwitch = data.getZeroSwitch();
        //原点方向
        int zero = data.getZero();
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_SERVO.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(NettyUtils.intToHexString(pulse,4));
        stringBuilder.append(NettyUtils.intToHexString(distance,4));
        stringBuilder.append(NettyUtils.intToHexString(maxDistance,4));
        stringBuilder.append(NettyUtils.intToHexString(speed,2));
        stringBuilder.append(NettyUtils.intToHexString(returnSpeed,2));
        stringBuilder.append(NettyUtils.intToHexString(accelerationTime,2));
        stringBuilder.append(NettyUtils.intToHexString(decelerationTime,2));
        stringBuilder.append(NettyUtils.intToHexString(acceleration,2));
        stringBuilder.append(NettyUtils.intToHexString(zeroSwitch,1));
        stringBuilder.append(NettyUtils.intToHexString(zero,1));
        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", data.getCabinet().desc, CabinetConstants.SettingCommand.SERVO.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(data.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void setTime(TimeSettingRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.TIME.num;
        //第几个伺服电机
        int mode =request.getMode().num;

        int status =request.getStatus() ;

        BigInteger timeLong = request.getTimeLong();
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_TIME.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(NettyUtils.intToHexString(timeLong,2));
        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getCabinet().desc, CabinetConstants.SettingCommand.TIME.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void setPrivate(PrivateSettingData request) {

        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.SET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.PRIVATE.num;
        //第几个柜
        int mode =request.getMode().num;
        //具体第几个参数
        int status =request.getStatus() ;
        StringBuilder stringBuilder;
        if(request.getMode().num.equals(CabinetConstants.SettingTimeCabinetMode.CABINET_A.num)){
            stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_PRIVATE_A.dataLength);
        }else if(request.getMode().num.equals(CabinetConstants.SettingTimeCabinetMode.CABINET_B.num)){
            stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_PRIVATE_B.dataLength);
        }else {
            stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SETTING_PRIVATE_C.dataLength);
        }
        //获取请求头、数据长度

        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));


        if(request.getMode().num.equals(CabinetConstants.SettingTimeCabinetMode.CABINET_A.num)){
            int zero = request.getZero();
            BigInteger distance = request.getDistance();
            stringBuilder.append(NettyUtils.intToHexString(zero,1));
            stringBuilder.append(NettyUtils.intToHexString(distance,4));
        }else if (request.getMode().num.equals(CabinetConstants.SettingTimeCabinetMode.CABINET_B.num)){
            BigInteger distance = request.getDistance();
            stringBuilder.append(NettyUtils.intToHexString(distance,2));
        }else {
            BigInteger distance = request.getDistance();
            stringBuilder.append(NettyUtils.intToHexString(distance,4));
        }

        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getCabinet().desc, CabinetConstants.SettingCommand.PRIVATE.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);

    }

    @Override
    public void getCabinet(WorkSettingRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SETTING.num;
        int mode = CabinetConstants.SettingMode.WORK.num;
        int status = 0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.SettingMode.WORK.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getIpPort(IPSettingRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SETTING.num;
        int mode = CabinetConstants.SettingMode.IP.num;
        int status = 0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.SettingMode.IP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getVersion(CabinetConstants.Cabinet workMode) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = workMode.num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SETTING.num;
        int mode = CabinetConstants.SettingMode.VERSION.num;
        int status = 0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", workMode.desc, CabinetConstants.SettingMode.WORK.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(workMode,bytes,frameNumberNow);
    }

    @Override
    public void getStep(StepSettingData data) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = data.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.STEP.num;
        //第几个步进电机
        int mode =data.getMode();
        //设置所有参数
        int status =0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", data.getWorkMode().desc, CabinetConstants.SettingCommand.STEP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(data.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getServo(ServoSettingData data) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = data.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.SERVO.num;
        //第几个步进电机
        int mode =data.getMode();
        //设置所有参数
        int status =0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", data.getWorkMode().desc, CabinetConstants.SettingCommand.SERVO.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(data.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getTime(TimeSettingRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.TIME.num;
        //第几个伺服电机
        int mode =request.getMode().num;

        int status =request.getStatus() ;

        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.SettingCommand.TIME.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getPrivate(PrivateSettingData request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = request.getWorkMode().num;
        //设置系统参数
        int type = CabinetConstants.CabinetSettingType.GET_SETTING.num;
        //参数类型
        int command = CabinetConstants.SettingCommand.PRIVATE.num;

        //第几个柜子
        int mode =request.getMode().num;

        int status =request.getStatus() ;

        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.QUETY.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        stringBuilder.append(CRC16Modbus.calculateCRC(stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.SettingCommand.PRIVATE.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void getAllSys(CabinetConstants.Cabinet workMode) {
        //获取仓柜
        WorkSettingRequest workSettingRequest =new WorkSettingRequest();
        workSettingRequest.setWorkMode(workMode);
        getCabinet(workSettingRequest);
        VacUntil.sleep(100);
        //获取ip端口号
        IPSettingRequest ipSettingRequest = new IPSettingRequest();
        ipSettingRequest.setWorkMode(workMode);
        ipSettingRequest.setCabinet(workMode);
        getIpPort(ipSettingRequest);
        VacUntil.sleep(100);

        getVersion(workMode);
        VacUntil.sleep(100);

        //获取步进电机
        for(int i=1; i<7;i++){
            StepSettingData stepSettingData = new StepSettingData();
            stepSettingData.setWorkMode(workMode);
            stepSettingData.setCabinet(workMode);
            stepSettingData.setMode(i);
            getStep(stepSettingData);
            VacUntil.sleep(100);
        }

        //获取伺服电机
        for(int i=1; i<11;i++){
            //获取伺服
            ServoSettingData data = new ServoSettingData();
            data.setWorkMode(workMode);
            data.setCabinet(workMode);
            data.setMode(i);
            getServo(data);
            VacUntil.sleep(100);
        }

        //A柜相关参数
        for(int i=1; i<6;i++) {
            TimeSettingRequest timeSettingRequest = new TimeSettingRequest();
            timeSettingRequest.setWorkMode(workMode);
            timeSettingRequest.setCabinet(workMode);
            timeSettingRequest.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_A);
            timeSettingRequest.setStatus(i);
            getTime(timeSettingRequest);
            VacUntil.sleep(100);
        }

        //B柜相关参数
        for(int i=1; i<4;i++) {
            TimeSettingRequest timeSettingRequest = new TimeSettingRequest();
            timeSettingRequest.setWorkMode(workMode);
            timeSettingRequest.setCabinet(workMode);
            timeSettingRequest.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_B);
            timeSettingRequest.setStatus(i);
            getTime(timeSettingRequest);
            VacUntil.sleep(100);
        }


        //C柜相关参数
        for(int i=1; i<3;i++) {
            TimeSettingRequest timeSettingRequest = new TimeSettingRequest();
            timeSettingRequest.setWorkMode(workMode);
            timeSettingRequest.setCabinet(workMode);
            timeSettingRequest.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_C);
            timeSettingRequest.setStatus(i);
            getTime(timeSettingRequest);
            VacUntil.sleep(100);
        }

        //A私有参数
        PrivateSettingData privateSettingData = new PrivateSettingData();
        privateSettingData.setWorkMode(workMode);
        privateSettingData.setCabinet(workMode);
        privateSettingData.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_A);
        privateSettingData.setStatus(1);
        getPrivate(privateSettingData);

        //B私有参数
        for(int i=1;i<=4;i++){
            privateSettingData.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_B);
            privateSettingData.setStatus(i);
            getPrivate(privateSettingData);
        }

        //C私有参数
        //C柜步进电机参数
        for(int i=1;i<=12;i++){
            privateSettingData.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_C);
            privateSettingData.setStatus(i);
            getPrivate(privateSettingData);
        }

        //C柜伺服电机距离参数
        for(int i=17;i<=28;i++){
            privateSettingData.setMode(CabinetConstants.SettingTimeCabinetMode.CABINET_C);
            privateSettingData.setStatus(i);
            getPrivate(privateSettingData);
        }



        privateSettingData.setStatus(240);
        getPrivate(privateSettingData);
    }

    @Override
    public CabinetSysResponse getAllSysList(CabinetConstants.Cabinet workMode) {
        List<VacMachineSys> vacMachineSysList = vacMachineSysMapper.selectList(new LambdaQueryWrapper<VacMachineSys>()
                .eq(VacMachineSys::getWorkMode,workMode.num)
                .eq(VacMachineSys::getDeleted,0));

        CabinetSysResponse cabinetSysResponse = new CabinetSysResponse();
        for (VacMachineSys vacMachineSys:vacMachineSysList){
            if(vacMachineSys.getIp()!=null){
                cabinetSysResponse.setIp(vacMachineSys.getIp());
                cabinetSysResponse.setPort(vacMachineSys.getPort());
            }else if(vacMachineSys.getVersion()!=null){
                cabinetSysResponse.setVersion(vacMachineSys.getVersion());

            }else if(vacMachineSys.getCommand()==2){
               List<VacMachineSys> stepList =  cabinetSysResponse.getStepList();
                if (stepList == null) {
                    stepList = new ArrayList<>();
                }
               stepList.add(vacMachineSys);
               cabinetSysResponse.setStepList(stepList);
            }else if(vacMachineSys.getCommand()==3){
                List<VacMachineSys> servoList =  cabinetSysResponse.getServoList();
                if (servoList == null) {
                    servoList = new ArrayList<>();
                }
                servoList.add(vacMachineSys);
                cabinetSysResponse.setServoList(servoList);

            }else if(vacMachineSys.getCommand()==4) {
                List<VacMachineSys> timeList = cabinetSysResponse.getTimeList();
                if (timeList == null) {
                    timeList = new ArrayList<>();
                }
                timeList.add(vacMachineSys);
                cabinetSysResponse.setTimeList(timeList);

            }else if(vacMachineSys.getCommand()==5){
                List<VacMachineSys> privateList = cabinetSysResponse.getPrivateList();
                if (privateList == null) {
                    privateList = new ArrayList<>();
                }
                privateList.add(vacMachineSys);
                cabinetSysResponse.setPrivateList(privateList);
            }
        }

        log.info(String.valueOf(cabinetSysResponse));
        return cabinetSysResponse;

    }


}
