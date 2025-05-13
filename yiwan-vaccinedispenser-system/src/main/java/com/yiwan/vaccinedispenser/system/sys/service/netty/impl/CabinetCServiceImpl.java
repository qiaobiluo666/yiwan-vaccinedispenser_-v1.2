package com.yiwan.vaccinedispenser.system.sys.service.netty.impl;

import cn.hutool.core.util.HexUtil;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.config.FrameNumberConfig;
import com.yiwan.vaccinedispenser.system.netty.msg.NettySendService;
import com.yiwan.vaccinedispenser.system.sys.data.request.netty.*;
import com.yiwan.vaccinedispenser.system.sys.service.netty.CabinetCService;
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
public class CabinetCServiceImpl implements CabinetCService {

    @Autowired
    private NettySendService nettySendService;


    @Override
    public void sendDrug(CabinetCSendDrugRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_C.num;
        // 0x01 自动出药
        int type = CabinetConstants.CabinetCType.SEND_DRUG.num;
        //指令
        int command = request.getCommand().num;
        //模式
        int mode =request.getMode()!=null ? request.getMode() :0;

        int status =request.getStatus()!=null ? request.getStatus().num :0;

        StringBuilder stringBuilder;
        if(command==1){
            //获取请求头、数据长度
             stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.SEND.dataLength);
        }else if((command==2) ){
             stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.FIND.dataLength);
        }else{
            stringBuilder =NettyUtils.toHandler(CabinetConstants.CabinetDataLength.BLOCK.dataLength);
        }

        stringBuilder.append(NettyUtils.intToHexString(frameNumberNow,2));
        stringBuilder.append(NettyUtils.intToHexString(cabinet,1));
        stringBuilder.append(NettyUtils.intToHexString(type,1));
        stringBuilder.append(NettyUtils.intToHexString(command,1));
        stringBuilder.append(NettyUtils.intToHexString(mode,1));
        stringBuilder.append(NettyUtils.intToHexString(status,1));

        if(command==1){
            //几号工作台
            stringBuilder.append(NettyUtils.intToHexString( request.getWorkNum(),1));
        }
        stringBuilder.append(CRC16Modbus.calculateCRC( stringBuilder.substring(8)));
        //包尾
        stringBuilder.append(Integer.toHexString(CabinetConstants.SUFFIX_INSTRUCTION));

        //stringBuilder 转化为byte
        byte[] bytes = HexUtil.decodeHex(stringBuilder.toString().toUpperCase());

        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_C.desc, CabinetConstants.CabinetCType.SEND_DRUG.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);
    }





    /**
     * @param request
     * 扫码伺服
     */
    @Override
    public void servo(CabinetCServoRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();
        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_C.num;
        // 0x03 伺服
        int type = CabinetConstants.CabinetCType.SERVO.num;
        //指令
        int command = request.getCommand().num;
        //模式
        int mode = request.getMode();

        int status = request.getStatus().num;
        StringBuilder stringBuilder = new StringBuilder();
        if(command==1){
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
        if(command==1){
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
        log.info("{}柜-{}：{}", CabinetConstants.Cabinet.CAB_C.desc, CabinetConstants.CabinetCType.SERVO.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给C控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);

    }

    @Override
    public void step(CabinetCStepRequest request) {
        //当前帧号
        Integer frameNumberNow = FrameNumberConfig.getFrameNumber();

        // 选择什么柜子
        int cabinet = CabinetConstants.Cabinet.CAB_C.num;

        // 0x04 步进
        int type = CabinetConstants.CabinetCType.STEP.num;

        //指令
        int command = request.getCommand().num;

        //模式 0x01角度位置
        int mode = request.getMode();

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

        log.info("{}柜-{}：{}", request.getWorkMode().desc, CabinetConstants.CabinetCType.STEP.desc,HexUtil.format(stringBuilder.toString().toUpperCase()));
        //给B控制板 发送消息
        nettySendService.sendMsg(request.getWorkMode(),bytes,frameNumberNow);

    }


}
