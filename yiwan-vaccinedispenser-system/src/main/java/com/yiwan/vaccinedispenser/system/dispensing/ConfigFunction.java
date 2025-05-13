package com.yiwan.vaccinedispenser.system.dispensing;

import com.alibaba.fastjson.JSON;
import com.yiwan.vaccinedispenser.system.domain.model.system.SysConfig;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigData;
import com.yiwan.vaccinedispenser.system.sys.data.ConfigSetting;
import com.yiwan.vaccinedispenser.system.sys.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;





@Component
@Slf4j
public class ConfigFunction {

    @Autowired
    private SysConfigService sysConfigService;


    //获取自动上药的系统参数
    public ConfigData getAutoDrugConfigData(){
        ConfigData configData = new ConfigData();
        List<SysConfig> sysConfigList = sysConfigService.getAutoDrugConfigData();
        //如果数据库有 那就使用数据库的
        for(SysConfig sysConfig: sysConfigList){
            switch (sysConfig.getConfigType()){


                //激光传感器到中心 的偏移量
                case "SENSOR_LEFT_DISTANCE"->configData.setLeftConstants(Integer.parseInt(sysConfig.getConfigValue()));
                case "SENSOR_RIGHT_DISTANCE"-> configData.setRightConstants(Integer.parseInt(sysConfig.getConfigValue()));
                case "SENSOR_HIGH_DISTANCE"->configData.setHeightConstants(Integer.parseInt(sysConfig.getConfigValue()));

                //激光传感器原始距离
                case "TABLE_ANGLE"->configData.setTableAngle(Double.parseDouble(sysConfig.getConfigValue()));
                case "TABLE_X"-> configData.setTableX(Integer.parseInt(sysConfig.getConfigValue()));
                case "TABLE_Y" ->configData.setTableY(Integer.parseInt(sysConfig.getConfigValue()));
                case "TABLE_Z" ->configData.setTableZ(Integer.parseInt(sysConfig.getConfigValue()));

                //上方扫码机位置
                case "ABOVE_SCAN_X"-> configData.setAboveScanX(Integer.parseInt(sysConfig.getConfigValue()));
                case "ABOVE_SCAN_Y" ->configData.setAboveScanY(Integer.parseInt(sysConfig.getConfigValue()));
                case "ABOVE_SCAN_Z" ->configData.setAboveScanZ(Integer.parseInt(sysConfig.getConfigValue()));


                //侧方扫码相机位置
                case "SIDE_SCAN_X"-> configData.setSideScanX(Integer.parseInt(sysConfig.getConfigValue()));
                case "SIDE_SCAN_Y" ->configData.setSideScanY(Integer.parseInt(sysConfig.getConfigValue()));
                case "SIDE_SCAN_Z" ->configData.setSideScanZ(Integer.parseInt(sysConfig.getConfigValue()));

                //下方扫码相机位置
                case "BELOW_SCAN_X"-> configData.setBelowScanX(Integer.parseInt(sysConfig.getConfigValue()));
                case "BELOW_SCAN_Y" ->configData.setBelowScanY(Integer.parseInt(sysConfig.getConfigValue()));
                case "BELOW_SCAN_Z" ->configData.setBelowScanZ(Integer.parseInt(sysConfig.getConfigValue()));

                //掉药位置
                case "DROP_X"-> configData.setDropX(Integer.parseInt(sysConfig.getConfigValue()));
                case "DROP_Y" ->configData.setDropY(Integer.parseInt(sysConfig.getConfigValue()));
                case "DROP_Z" ->configData.setDropZ(Integer.parseInt(sysConfig.getConfigValue()));
                case "DROP_X_ADD"->configData.setDropXAdd(sysConfig.getConfigValue());

                //废料区域位置
                case "WASTE_X"-> configData.setWasteX(Integer.parseInt(sysConfig.getConfigValue()));
                case "WASTE_Y" ->configData.setWasteY(Integer.parseInt(sysConfig.getConfigValue()));
                case "WASTE_Z" ->configData.setWasteZ(Integer.parseInt(sysConfig.getConfigValue()));


                //激光测距的位置
                case "SCAN_SERVO_X"-> configData.setSensorDistanceX(Integer.parseInt(sysConfig.getConfigValue()));
                case "SCAN_SERVO_Y"-> configData.setSensorDistanceY(Integer.parseInt(sysConfig.getConfigValue()));
                case "SCAN_SERVO_Z"-> configData.setSensorDistanceZ(Integer.parseInt(sysConfig.getConfigValue()));

                //机械手总宽度
                case "HAND_LEN"-> configData.setHandLen(Integer.parseInt(sysConfig.getConfigValue()));
                case "HAND_GAP" ->configData.setGap(Integer.parseInt(sysConfig.getConfigValue()));
                case "HAND_EARLY" ->configData.setEarly(Integer.parseInt(sysConfig.getConfigValue()));

                //机械手回原位XZ
                case "HAND_INIT_X"-> configData.setHandInitX(Integer.parseInt(sysConfig.getConfigValue()));
                case "HAND_INIT_Z"-> configData.setHandInitZ(Integer.parseInt(sysConfig.getConfigValue()));

                //手动上药机械手就绪位置XZ
                case "HAND_DRUG_X"-> configData.setHandDrugX(Integer.parseInt(sysConfig.getConfigValue()));
                case "HAND_DRUG_Z"-> configData.setHandDrugZ(Integer.parseInt(sysConfig.getConfigValue()));

                //10层板板长
                case "LINE_LONG" -> configData.setLineLong(Integer.parseInt(sysConfig.getConfigValue()));

                //右方步进电机旋转的角度
                case  "RIGHT_ANGLE" -> configData.setRightAngle(Integer.parseInt(sysConfig.getConfigValue()));
                //左方步进电机旋转的角度
                case  "LEFT_ANGLE" -> configData.setLeftAngle(Integer.parseInt(sysConfig.getConfigValue()));

            }
        }



        return configData;
    }

    //获取系统设置的系统参数
    public ConfigSetting getSettingConfigData(){
        ConfigSetting configSetting = new ConfigSetting();
        List<SysConfig> sysConfigList = sysConfigService.getSettingConfigData();
        //如果数据库有 那就使用数据库的
        for(SysConfig sysConfig: sysConfigList){
            switch (sysConfig.getConfigType()){
                //政采云发药模块是否启用
                case "ZCY_SEND"-> configSetting.setZcySend(sysConfig.getConfigValue());
                //政采云自动上药模块是否启用
                case "ZCY_AUTO"-> configSetting.setZcyAuto(sysConfig.getConfigValue());
                //参数为 true 左边坐标点计算为 +x -y 右边坐标点 -x -y 反之为false
                case "B_FIND_X"-> configSetting.setBFindX(sysConfig.getConfigValue());
                //C柜是否有挡片
                case "C_BLANK"-> configSetting.setCBlank(sysConfig.getConfigValue());
                //C柜是否是抬升装置 true 抬升装置 false 不是抬升装置
                case "C_LIFTING"-> configSetting.setCLifting(sysConfig.getConfigValue());

                case "OPEN_BLANK_MORNING_TIME"-> configSetting.setCBlankOpenMorning(sysConfig.getConfigValue());

                case "CLOSE_BLANK_MORNING_TIME"-> configSetting.setCBlankCloseMorning(sysConfig.getConfigValue());

                case "OPEN_BLANK_AFTERNOON_TIME"-> configSetting.setCBlankOpenAfternoon(sysConfig.getConfigValue());

                case "CLOSE_BLANK_AFTERNOON_TIME"-> configSetting.setCBlankCloseAfternoon(sysConfig.getConfigValue());

            }
        }

        return configSetting;
    }

}
