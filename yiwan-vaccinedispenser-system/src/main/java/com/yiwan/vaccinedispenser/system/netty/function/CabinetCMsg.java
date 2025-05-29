package com.yiwan.vaccinedispenser.system.netty.function;

import cn.gov.zcy.open.sdk.http.ResponseResult;
import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.core.common.CommandEnums;
import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import com.yiwan.vaccinedispenser.core.common.emun.CabinetConstants;
import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.core.websocket.WebsocketService;
import com.yiwan.vaccinedispenser.system.dispensing.ConfigFunction;
import com.yiwan.vaccinedispenser.system.dispensing.DispensingFunction;
import com.yiwan.vaccinedispenser.system.dispensing.SendDrugThreadManager;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.data.RedisDrugListData;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacMachineExceptionService;
import com.yiwan.vaccinedispenser.system.until.NettyUtils;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/11 11:12
 */
@Slf4j
@Component
public class CabinetCMsg {
    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;
    @Autowired
    private CabinetSysMsg cabinetSysMsg;
    @Autowired
    private SendDrugThreadManager sendDrugThreadManager;


    @Autowired
    private ZcyFunction zcyFunction;

    @Autowired
    private DispensingFunction dispensingFunction;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private VacMachineExceptionService vacMachineExceptionService;

    @Value("${app.isOpen}")
    private  String isOpen;

    @Autowired
    private ConfigFunction configFunction;

    /**
     *
     * @param bytesStr
     * C柜接收信息
     */

        public  void receiveMsgCabinetC(String[] bytesStr) throws Exception {

        ConfigSetting configSetting = configFunction.getSettingConfigData();

        int address = Integer.parseInt(bytesStr[9], 16);
        log.info("收到C柜{}指令:{}", CabinetConstants.CabinetCType.SEND_DRUG.desc, NettyUtils.StringListToString(bytesStr));

        switch (bytesStr[7]) {
            case "01" ->  {
                switch (bytesStr[8]){
                    //发药指令
                    case "01"->{
                        switch (bytesStr[12]){
                            case "01" ->{
                                valueOperations.set(RedisKeyConstant.CABINET_C_WORK,"true");
                                log.info("=====================================================第{}工作台发药结束======================================",address);
                                //拿到redis 发药函数，顺利完成
                                String drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
                                if (drugStr!=null){
                                    log.info("去除一条信息");
                                    RedisDrugListData drugListData = JSON.parseObject(drugStr,RedisDrugListData.class);
                                    log.info("第{}个工作台 出药成功信息：{}",address,drugListData);

                                    Map<String, Object> commandData = new HashMap<>();
                                    commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_END.getCode());
                                    commandData.put("data", drugListData);
                                    websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
                                    listOps.leftPop(RedisKeyConstant.SEND_LIST);

                                    if("true".equals(configSetting.getZcySend())){
//                                    if("true".equals(isOpen)){
                                        zcyFunction.sendResult(drugListData,"1");
                                    }

                                }else {
                                    valueOperations.set(RedisKeyConstant.DRUG_RUN_START,"false");
                                }

//                                //如果设备有挡片
//                                if("true".equals(configSetting.getCBlank())){
//                                    drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
//                                    if (drugStr==null){
//                                        //送药数据清空完毕 关闭挡片
//                                        dispensingFunction.moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.CLOSE);
//                                    }
//                                }

                            }

                            case "02" ->{
                                valueOperations.set(RedisKeyConstant.CABINET_C_WORK,"true");
                                log.error("=====================================================第{}工作台发药失败======================================",address);
                                //报错处理 发药异常 重新发药
                                String drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
                                if (drugStr!=null){
                                    RedisDrugListData drugListData = JSON.parseObject(drugStr,RedisDrugListData.class);
                                    String msg = null;
                                    String zcyMsg = "机器异常！";
                                    switch (bytesStr[13]){
                                        case "0A"->{
                                            msg= "第"+address+"个工作台 出药超时,发苗信息："+JSON.toJSONString(drugListData);
                                            zcyMsg = "机器异常，传送轨道超时";
                                        }
                                        case "20"->{
                                            msg= "第"+address+"个工作台 复位按钮按下,发苗信息："+JSON.toJSONString(drugListData);
                                            zcyMsg = "机器异常，请先复位复位按钮按钮";
                                        }

                                        case "22"->{
                                            msg= "第"+address+"个工作台 复位按钮按下,斜皮带还有苗,发苗信息："+JSON.toJSONString(drugListData);
                                            zcyMsg = "机器异常，复位按钮按下，斜皮带还有苗";
                                        }
                                    }

                                    log.error(msg);
                                    Map<String, Object> commandData = new HashMap<>();
                                    commandData.put("code", CommandEnums.DEVICE_STATUS_SEND_DRUG_LIST_ERROR.getCode());
                                    commandData.put("data", drugStr);
                                    websocketService.sendInfo(CommandEnums.SHOW_MSG_WEB.getCode(),commandData);
                                    vacMachineExceptionService.dropException(SettingConstants.MachineException.SENDWARING.code,drugListData,msg);
                                    //删除一条数据
                                    listOps.leftPop(RedisKeyConstant.SEND_LIST);

                                    drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
                                    if (drugStr!=null){
                                        valueOperations.set(RedisKeyConstant.DRUG_RUN_START,"false");
                                    }

                                    if("true".equals(configSetting.getZcySend())){
                                        zcyFunction.sendResult(drugListData,"0",zcyMsg);
                                    }

                                }

//                                //如果设备有挡片
//                                if("true".equals(configSetting.getCBlank())){
//                                    drugStr = listOps.index(RedisKeyConstant.SEND_LIST,0);
//                                    String beltStr = listOps.index(RedisKeyConstant.BELT_LIST,0);
//                                    if (drugStr==null&& beltStr ==null){
//                                        //送药数据清空完毕 关闭挡片
//                                        log.info("送药数据清空完毕，关闭挡片");
//                                        dispensingFunction.moveBlock(CabinetConstants.CabinetCSendDrugBlockStatus.CLOSE);
//                                    }
//                                }
                            }
                        }
                    }
                    //查询皮带状态指令
                    case "02"->{
                        //检测斜坡皮带是否停止
                        switch (bytesStr[11]){
                            case "01"->{
                                log.info("C柜:皮带---停止");
                                valueOperations.set(RedisKeyConstant.CABINET_C_BELT_STOP,"true");
                            }

                            case "02"->{
                                log.info("C柜皮带---运动");
                                valueOperations.set(RedisKeyConstant.CABINET_C_BELT_STOP,"false");
                            }
                        }
                    }

                    //挡片指令
                    case "03"->{
                        //检测
                        switch (bytesStr[10]){
                            //打开
                            case "01"->{
                                switch (bytesStr[11]){
                                    //打开成功
                                    case "01"->{
                                        log.info("C柜:挡片---开启成功");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"open");
                                    }
                                    //打开失败
                                    case  "02"->{
                                        log.info("C柜:挡片---开启失败");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"error");
                                    }

                                }

                            }


                            //关闭
                            case "02"->{
                                switch (bytesStr[11]){
                                    //关闭成功
                                    case "01"->{
                                        log.info("C柜:挡片---关闭成功");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"close");
                                    }

                                    //打开失败
                                    case  "02"->{
                                        log.info("C柜:挡片---关闭失败");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"error");
                                    }

                                }
                            }

                            //查询
                            case "03"->{

                                switch (bytesStr[11]){
                                    case "01"->{
                                        log.info("C柜:挡片---开启状态");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"open");
                                    }
                                    case "02"->{
                                        log.info("C柜:挡片---关闭状态");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"close");
                                    }

                                    case "03"->{
                                        log.info("C柜:挡片---运动状态");
                                        valueOperations.set(RedisKeyConstant.CABINET_C_BLOCK_STATUS,"running");
                                    }
                                }

                            }

                        }
                    }

                    //复位状态按钮
                    case  "04"->{
                        //几号接种台
                        int workNum = Integer.parseInt(bytesStr[9], 16);

                        //检测复位按钮是否按下
                        switch (bytesStr[11]){
                            //0x01 - 复位按钮按下
                            case "01"->{
                                switch (bytesStr[12]){
                                    case "01"->{
                                        valueOperations.set(String.format(RedisKeyConstant.CABINET_C_RESET,workNum),"true");
                                    }
                                }

                            }
                            //0x02 - 复位按钮未按下
                            case "02"->{
                                switch (bytesStr[12]){
                                    case "01"->{
                                        valueOperations.set(String.format(RedisKeyConstant.CABINET_C_RESET,workNum),"false");
                                    }
                                }
                            }

                        }



                    }


                }

            }

            //输入检测
            case "06"->{
                log.info("收到C柜{}:{}",CabinetConstants.CabinetCType.INPUT.desc,NettyUtils.StringListToString(bytesStr));
                //查询所有传感器状态
                if ("00".equals(bytesStr[9])) {
                    List<Integer> sensorList = NettyUtils.allInPut(bytesStr);
                    for (Integer integer : sensorList) {
                        if (integer == 1) {
                            valueOperations.set(RedisKeyConstant.sensor.SENSOR_CABINET_C, sensorList.toString());
                        } else {
                            valueOperations.set(RedisKeyConstant.sensor.SENSOR_CABINET_C, sensorList.toString());
                        }
                    }
                }
            }

            //设置系统参数
            case "80" -> {
                log.info("收到C柜{}:{}",CabinetConstants.CabinetSettingType.SET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
            }

            //获取系统参数
            case "81" -> {
                log.info("收到C柜{}:{}",CabinetConstants.CabinetSettingType.GET_SETTING.desc,NettyUtils.StringListToString(bytesStr));
                cabinetSysMsg.receiveMsgCabinetSys(CabinetConstants.Cabinet.CAB_C,bytesStr);
            }
        }
    }

}
