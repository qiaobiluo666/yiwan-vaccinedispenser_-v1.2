package com.yiwan.vaccinedispenser.system.sys.service.netty.impl;

import cn.hutool.core.util.HexUtil;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.config.FrameNumberConfig;
import com.yiwan.vaccinedispenser.system.netty.msg.NettySendService;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetAService;
import com.yiwan.vaccinedispenser.system.until.CRC16Modbus;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/2/28 15:59
 */
@Service
@Slf4j
public class CabinetAServiceImpl implements CabinetAService {

    @Autowired
    private NettySendService nettySendService;
    @Override
    public void dropCommand(DropRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_A.num;
        // 0x05-输入输出控制数据透传
        int type = CabinetConstants.CabinetAType.DROP.num;

        int command = request.getCommand();
        int mode = request.getMode().num;
        int status = 0;


        StringBuilder stringBuilder;
        if(mode==4){
            stringBuilder= NettyUtils.toHandler(CabinetConstants.CabinetDataLength.DROP_AUTO.dataLength);
        }else {
            stringBuilder= NettyUtils.toHandler(CabinetConstants.CabinetDataLength.DROP.dataLength);

        }
        //获取请求头、数据长度

        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        //0x0A
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        //0x01-输入输出控制数据透传
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        if(mode==4){
            stringBuilder.append(NettyUtils.intToHexString(request.getIoNum(),1));
            stringBuilder.append(NettyUtils.intToHexString(request.getTimes(),2));
        }else {
            //IO拓展板的16进制转化协议
            StringBuilder ioExpand = NettyUtils.IOExpand(request.getIoList());
            //IO拓展版协议
            stringBuilder.append(ioExpand);
        }

        //CRC校验
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_A.desc, CabinetConstants.CabinetAType.DROP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给A控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);

    }

    @Override
    public void ledCommand(LedRequest request) {

        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_A.num;
        int type = CabinetConstants.CabinetAType.LED.num;
        int command = request.getCommand();
        int mode = request.getMode().num;
        int status = request.getStatus().num;
        int ledNum = request.getLedNum();
        //获取请求头、数据长度
        StringBuilder stringBuilder = NettyUtils.toHandler(CabinetConstants.CabinetDataLength.LED.dataLength);
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        //0x0A
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        //0x01-输入输出控制数据透传
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));
        //led灯 位置
        stringBuilder.append(NettyUtils.intToHexString(ledNum,2));
        //CRC校验
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));
        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());
        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_A.desc, CabinetConstants.CabinetAType.LED.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);




    }

    @Override
    public void servo(CabinetAServoRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_A.num;
        // 0x02 皮带伺服
        int type = CabinetConstants.CabinetAType.SERVO.num;
        //指令
        int command = request.getCommand().num;
        //模式
        int mode = request.getMode();
        //状态
        int status =request.getStatus().num;

        StringBuilder stringBuilder = new StringBuilder();
        if(command==1 || command==5 ){
            //获取请求头、数据长度
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SERVO_POSITION.dataLength)) ;
        }else if(command==2) {
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SERVO_SPEED.dataLength)) ;
        }else {
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SERVO_ZERO.dataLength)) ;
        }
        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));

        //位置模式
        if(command==1 || command==5){
            int distance = request.getDistance();
            //运动距离
            stringBuilder.append(NettyUtils.intToHexString(distance,4));

        }else if(command==2) {

            int speed = request.getSpeed();
            //运动距离
            stringBuilder.append(NettyUtils.intToHexString(speed,2));

        }

        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());

        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_A.desc, CabinetConstants.CabinetAType.SERVO.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void step(CabinetAStepRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();

        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_A.num;

        // 0x04 步进
        int type = CabinetConstants.CabinetAType.STEP.num;

        //指令
        int command = request.getCommand().num;

        //模式 0x01角度位置
        int mode = request.getMode().num;

        int status = request.getStatus().num;
        StringBuilder stringBuilder = new StringBuilder();
        if(command==1){
            //获取请求头、数据长度
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.STEP_POSITION.dataLength)) ;
        }else if(command==2) {
            //获取请求头、数据长度
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.STEP_SPEED.dataLength)) ;
        }else {
            //获取请求头、数据长度
            stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.STEP_ZERO.dataLength)) ;
        }

        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));

        //位置模式
        if(command==1){
            int distance = request.getDistance();
            //运动距离
            stringBuilder.append(NettyUtils.intToHexString(distance,4));

        }else if(command==2) {
            int speed = request.getSpeed();
            //运动速度
            stringBuilder.append(NettyUtils.intToHexString(speed,2));
        }

        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());

        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.CabinetAType.STEP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void outPut(OutPutRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();

        // 选择什么柜子
        int cabinet = request.getCabinet().num;

        // 0x06-输出控制
        int type = CabinetConstants.CabinetAType.OUTPUT.num;

        // 输出 不输出
        int command = request.getCommand().num;

        //模式
        int mode = request.getMode();
        int status = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.OUTPUT.dataLength)) ;
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

        log.info("{}柜-{}：{}", request.getWorkMode().name, CabinetConstants.CabinetAType.OUTPUT.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给AB控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }

    @Override
    public void intPut(InPutRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();

        // 选择什么柜子
        int cabinet = request.getCabinet().num;

        // 0x05-输入检测
        int type = CabinetConstants.CabinetAType.INPUT.num;

        // 输出 不输出0x00 查询
        int command = request.getCommand().num;
        int mode =request.getMode();
        int status =0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NettyUtils.toHandler(CabinetConstants.CabinetDataLength.INPUT.dataLength)) ;
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

//        log.info("{}柜-{}：{}", request.getWorkMode().name, CabinetConstants.CabinetAType.INPUT.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给AB控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);

    }

    @Override
    public void getDistance(CabinetAGetDistanceRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_A.num;
        // 0x01 自动上药
        int type = CabinetConstants.CabinetAType.DISTANCE.num;
        //指令
        int command = request.getCommand().num;
        //模式
        int mode = request.getMode();
        //状态
        int status = 0;
        //获取请求头、数据长度
        StringBuilder stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.DISTANCE.dataLength);
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

        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_A.desc, CabinetConstants.CabinetAType.DISTANCE.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }


}
