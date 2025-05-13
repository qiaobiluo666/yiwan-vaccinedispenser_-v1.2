package com.yiwan.vaccinedispenser.core.common.emun;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author slh
 * @date 2024/2/20 0020 14:03
 */
public class CabinetConstants {


    //包头前缀  0xA5
    public static final Integer PREFIX_FIRST = 0xA5;

    //包头前缀  0x5A
    public static final Integer PREFIX_SECOND = 0x5A;

    //包尾  0x55
    public static final Integer SUFFIX_INSTRUCTION = 0x55;



    /**
     * 柜体选择枚举
     * */
    public enum Cabinet {
        CAB_A(0x0A,10, "A", "0x0A A柜"),

        CAB_B(0x0B,11 ,"B","0x0B B柜"),
        CAB_C(0x0C,12 ,"C","0x0C C柜"),;


        Cabinet(Integer code,Integer num, String name, String desc) {
            this.code = code;
            this.num = num;
            this.name = name;
            this.desc = desc;
        }

        public final Integer code;
        public final Integer num;
        public final String name;
        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Cabinet codeOf(Integer num) {
            for (Cabinet type : Cabinet.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }










    /**
     * 指令的数据长度（10进制）
     */
    public enum CabinetDataLength {
        //IO
        DROP(14, "0x01-输入输出控制数据透传"),

        DROP_AUTO(12, "0x01-输入输出控制数据透传"),
        //灯板
        LED(11,"0x02-led灯板"),

        APPLY(9,"B柜自动上药数据长度"),

        DISTANCE(9,"B柜获取距离传感器长度"),

        SEND(10,"C柜发药数据长度"),
        FIND(9,"C柜读取皮带状态数据长度"),

        BLOCK(9,"C柜挡片控制数据长度"),
        SERVO_POSITION(13,  "伺服位置模式数据长度"),

        SERVO_SPEED(11,  "伺服速度模式数据长度"),


        SERVO_ZERO(9,  "伺服原点模式和暂停模式数据长度"),

        STEP_POSITION(13, "步进位置模式数据长度"),

        STEP_SPEED(11, "步进速度模式数据长度"),


        STEP_ZERO(9,"步进原点模式和暂停模式数据长度"),
        OUTPUT(9, "输出控制数据长度"),

        INPUT(9, "输入检测数据长度"),

        SETTING_WORK(10, "设置系统参数-工作模式数据长度"),

        SETTING_IP(15, "设置系统参数-IP数据长度"),

        SETTING_SERVO(33, "设置系统参数-伺服数据长度"),

        SETTING_STEP(33, "设置系统参数-步进数据长度"),

        SETTING_TIME(11,"设置系统参数-时间设置"),

        SETTING_PRIVATE_A(14,"设置系统参数-私有参数设置-A"),

        SETTING_PRIVATE_B(11,"设置系统参数-私有参数设置-B"),

        SETTING_PRIVATE_C(13,"设置系统参数-私有参数设置-C"),

        QUETY(9,"获取系统参数数据长度"),

        HEART(6,"心跳包数据长度");

        CabinetDataLength(int dataLength, String desc) {
            this.dataLength = dataLength;
            this.desc = desc;
        }

        public final Integer dataLength;
        public final String desc;
    }

    /**
     * A柜 指令枚举
     */
    public enum CabinetAType {

        DROP(0x01,1, "0x01-掉药"),
        LED(0x02,2,"0x01-灯板"),
        SERVO(0x03, 3, "0x03 伺服"),

        STEP(0x04, 4,"0x04-步进"),
        OUTPUT(0x05 ,5,"0x05 输出控制"),
        INPUT(0x06,6, "0x06 输入检测"),

        DISTANCE(0x07,7, "0x07 距离传感器"),

        REPORT(0xFF,255,"0xFF 主动上报");

        CabinetAType(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;



        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAType codeOf(Integer num) {
            for (CabinetAType type : CabinetAType.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * B柜 指令枚举
     */
    public enum CabinetBType {

        APPLY(0x01, 1,"0x01 -药指令"),
        DISTANCE(0x02,2,"0x02 -距离传感器控制"),
        SERVO(0x03, 3, "0x03-伺服控制指令"),
        STEP(0x04, 4,"0x04-步进"),
        OUTPUT(0x05, 5,"0x05 输出控制"),
        INPUT(0x06,6, "6 输入检测"),
        REPORT(0xFF,255,"0xFF 主动上报");

        CabinetBType(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;


        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBType codeOf(Integer num) {
            for (CabinetBType type : CabinetBType.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }


    /**
     * C柜 指令枚举
     */
    public enum CabinetCType {
        SEND_DRUG(0x01, 1, "0x01-药指令"),
        SERVO(0x03, 3, "0x03-伺服控制指令"),
        STEP(0x04, 4,"0x04-步进"),
        OUTPUT(0x05, 5,"0x05 输出控制"),
        INPUT(0x06,6, "6 输入检测"),

        REPORT(0xFF,255,"0xFF 主动上报");
        CabinetCType(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;


        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBType codeOf(Integer num) {
            for (CabinetBType type : CabinetBType.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }


    /**
     * 系统参数 指令枚举
     */
    public enum CabinetSettingType {

        SET_SETTING(0x80,128, "0x80 -设置参数指令"),
        GET_SETTING(0x81, 129, "0x81 -获取参数指令"),
        HEART(0xAA,170, "0xAA -获取心跳包");

        CabinetSettingType(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetSettingType codeOf(Integer num) {
            for (CabinetSettingType type : CabinetSettingType.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }


    /**
     * A柜
     */
    ///0x06-输出控制 具体指令
    public enum OutPutCommand {
        OUTPUT(0x01,1, "0x01 输出"),

        NOT_OUTPUT(0x02, 2,"0x02 不输出");

        OutPutCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static OutPutCommand codeOf(Integer num) {
            for (OutPutCommand type : OutPutCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }

    //0x07-输入检测具体指令
    public enum InPutCommand {

        QUERY(0x00,0,"0x00 查询");

        InPutCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;


        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static InPutCommand codeOf(Integer num) {
            for (InPutCommand type : InPutCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }



    //IO拓展板的状态
    public enum IOMode {

        OUTPUT(0x01, 1,"0x01 输出"),
        NOT_OUTPUT(0x02 , 2,"0x02 不输出"),
        QUERY(0x03,3,"0x03 查询"),

        AUTO(0x04,4,"0x04 自动");

        IOMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static IOMode codeOf(Integer num) {
            for (IOMode type : IOMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }


   // LED拓展板的状态
    public enum LedMode {

        OUTPUT(0x01, 1,"0x01 输出"),
        NOT_OUTPUT(0x02 , 2,"0x02 不输出"),
        QUERY(0x03,3,"0x03 查询");

        LedMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static LedMode codeOf(Integer num) {
            for (LedMode type : LedMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //led 灯的状态
    public enum LedStatus{

        RED(0x01 ,1,"01 红灯"),
        GREEN(0x02 ,2, "02 绿灯"),
        BLUE(0x03,3,"03 蓝灯");

        LedStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }
        public final Integer code;

        public final Integer num;

        public final String desc;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static LedStatus codeOf(Integer num) {
            for (LedStatus type : LedStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //A柜伺服命令
    public enum CabinetAServoCommand {

        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),

        PAUSE(0x04,4,"0x04 暂停模式"),

        RELATIVE(0x05,5,"0x05 相对位置模式");

        CabinetAServoCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAServoCommand codeOf(Integer num) {
            for (CabinetAServoCommand type : CabinetAServoCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜伺服 模式
    public enum CabinetAServoMode {
        APPLY_SERVO_X(0x07, 7,"0x05 -上药伺服X"),

        APPLY_SERVO_Z(0x08, 8,"0x06 -上药伺服Z");

        CabinetAServoMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAServoMode codeOf(Integer num) {
            for (CabinetAServoMode type : CabinetAServoMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }




    //A柜伺服状态
    public enum CabinetAServoStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转"),
        REVERSAL(0x02 ,2,"0x02：反转"),

        SEND(0x10,16,"0x10:送药命令 (将药品送到边缘)"),

        STOP(0x20,32,"0x20 掉药命令 （将在边缘的药掉下) "),
        BELT_STOP(0x30,48,"0x30 停药命令 （升降台停药） ");

        CabinetAServoStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAServoStatus codeOf(Integer num) {
            for (CabinetAServoStatus type : CabinetAServoStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }



    //A柜步进 命令
    public enum CabinetAStepCommand {
        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),
        PAUSE(0x04,4,"0x04 暂停模式");



        CabinetAStepCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAStepCommand codeOf(Integer num) {
            for (CabinetAStepCommand type : CabinetAStepCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //A柜步进状态选择
    public enum CabinetAStepStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转    位置模式为 夹住"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetAStepStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAStepStatus codeOf(Integer num) {
            for (CabinetAStepStatus type : CabinetAStepStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //A柜步进子命令
    public enum CabinetAStepMode {
        CLAMP(0x01, 1,"0x01-夹爪步进"),

        BLOCK(0x02,2,"0x02 挡片步进");




        CabinetAStepMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAStepMode codeOf(Integer num) {
            for (CabinetAStepMode type : CabinetAStepMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }


    /**
     * A柜获取距离传感器距离命令
     */
    //A柜自动上药指令
    public enum CabinetAGetDistanceCommand {

        GET(0x01, 1,"0x01 - 读距离");

        CabinetAGetDistanceCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetAGetDistanceCommand codeOf(Integer num) {
            for (CabinetAGetDistanceCommand type : CabinetAGetDistanceCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }






    /**
     * B柜
     */
    //B柜自动上药指令
    public enum CabinetBApplyCommand {

        AUTO(0x01, 1,"0x01:自动上药"),

        TABLE(0x02 ,2,"0x02：平台滑台");

        CabinetBApplyCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBApplyCommand codeOf(Integer num) {
            for (CabinetBApplyCommand type : CabinetBApplyCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜自动上药状态
    public enum CabinetBApplyMode {
        START(0x01, 1,"0x01：启动"),

        STOP(0x02 ,2,"0x02：停止");

        CabinetBApplyMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBApplyMode codeOf(Integer num) {
            for (CabinetBApplyMode type : CabinetBApplyMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

   //B柜自动上药状态
    public enum CabinetBApplyStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetBApplyStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBApplyStatus codeOf(Integer num) {
            for (CabinetBApplyStatus type : CabinetBApplyStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }


    /**
     * B柜获取距离传感器距离命令
     */
    //B柜自动上药指令
    public enum CabinetBGetDistanceCommand {

        GET(0x01, 1,"0x01 - 读距离");

        CabinetBGetDistanceCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBGetDistanceCommand codeOf(Integer num) {
            for (CabinetBGetDistanceCommand type : CabinetBGetDistanceCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }














    //B柜伺服 命令
    public enum CabinetBServoCommand {
        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),
        PAUSE(0x04,4,"0x04 暂停模式"),
        RELATIVE(0x05,5,"0x05 相对位置模式");


        CabinetBServoCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBServoCommand codeOf(Integer num) {
            for (CabinetBServoCommand type : CabinetBServoCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

     //B柜伺服 模式
    public enum CabinetBServoMode {
        SCAN_SERVO_X(0x01, 1,"0x01-扫码伺服X"),
        SCAN_SERVO_Y(0x02, 2,"0x02 -扫码伺服Y"),
        SCAN_SERVO_Z(0x03, 3,"0x03 -扫码伺服Z"),

        APPLY_SERVO_X(0x05, 5,"0x05 -上药伺服X"),

        APPLY_SERVO_Z(0x06, 6,"0x06 -上药伺服Z");

        CabinetBServoMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBServoMode codeOf(Integer num) {
            for (CabinetBServoMode type : CabinetBServoMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜伺服状态选择
    public enum CabinetBServoStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转 或 获取当前位置"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetBServoStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBServoStatus codeOf(Integer num) {
            for (CabinetBServoStatus type : CabinetBServoStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

   //B柜步进 命令
    public enum CabinetBStepCommand {
        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),
        PAUSE(0x04,4,"0x04 暂停模式");



        CabinetBStepCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBStepCommand codeOf(Integer num) {
            for (CabinetBStepCommand type : CabinetBStepCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜步进状态选择
    public enum CabinetBStepStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetBStepStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBStepStatus codeOf(Integer num) {
            for (CabinetBStepStatus type : CabinetBStepStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜步进子命令
    public enum CabinetBStepMode {
        ROTATE(0x01, 1,"0x01-旋转步进"),

        CLAMP(0x02,2,"0x02 夹爪步进"),

        VIBRATE(0x03,3,"0x03 震动步进");


        CabinetBStepMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetBStepMode codeOf(Integer num) {
            for (CabinetBStepMode type : CabinetBStepMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜步进 命令
    public enum CabinetCStepCommand {
        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),
        PAUSE(0x04,4,"0x04 暂停模式");



        CabinetCStepCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCStepCommand codeOf(Integer num) {
            for (CabinetCStepCommand type : CabinetCStepCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜步进状态选择
    public enum CabinetCStepStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetCStepStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCStepStatus codeOf(Integer num) {
            for (CabinetCStepStatus type : CabinetCStepStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜步进子命令
    public enum CabinetCStepMode {
        ROTATE(0x01, 1,"0x01-旋转步进"),

        CLAMP(0x02,2,"0x02 夹爪步进"),

        VIBRATE(0x03,3,"0x03 震动步进");


        CabinetCStepMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCStepMode codeOf(Integer num) {
            for (CabinetCStepMode type : CabinetCStepMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }





    /**
     * C柜
     */
    //拨片伺服 命令
    public enum CabinetCServoCommand {

        POSITION(0x01, 1,"0x01 位置模式"),

        SPEED(0x02,2,"0x02 速度模式"),

        ZERO(0x03,3,"0x03 原点模式"),
        PAUSE(0x04,4,"0x04 暂停模式");

        CabinetCServoCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCServoCommand codeOf(Integer num) {
            for (CabinetCServoCommand type : CabinetCServoCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜伺服状态选择
    public enum CabinetCServoStatus {
        ZERO(0x00, 0,"0x00：无"),
        COROTATION(0x01, 1,"0x01：正转"),
        REVERSAL(0x02 ,2,"0x02：反转");

        CabinetCServoStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCServoStatus codeOf(Integer num) {
            for (CabinetCServoStatus type : CabinetCServoStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜出药命令
    public enum CabinetCSendDrugCommand {
        SEND(0x01, 1,"0x01 出药"),

        FIND(0x02,2,"0x02 读皮带状态"),

        BLOCK(0x03,3,"0x03 挡片控制"),

        RESET(0x04,4,"0x04 - 读复位按钮状态");



        CabinetCSendDrugCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCSendDrugCommand codeOf(Integer num) {
            for (CabinetCSendDrugCommand type : CabinetCSendDrugCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜挡片子命令
    public enum CabinetCSendDrugBlockStatus {
        OPEN(0x01, 1,"0x01 打开"),

        CLOSE(0x02,2,"0x02 关闭"),

        QUERY(0x03,3,"0x03 查询");



        CabinetCSendDrugBlockStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CabinetCSendDrugBlockStatus codeOf(Integer num) {
            for (CabinetCSendDrugBlockStatus type : CabinetCSendDrugBlockStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }










    /**
     * 设置系统参数
     */
    //系统参数的命令
    public enum SettingCommand{
        SETTING(0x01,1, "0x01 -系统参数"),
        STEP(0x02,2, "0x02-步进电机参数"),
        SERVO(0x03, 3,"0x03-伺服电机参数"),
        TIME(0x04, 4,"0x04-时间参数"),
        PRIVATE(0x05, 5,"0x05-私有参数");

        SettingCommand(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }
        public final Integer code;
        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingCommand codeOf(Integer num) {
            for (SettingCommand type : SettingCommand.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }
   //设置参数指令子命令
    public enum SettingMode {

        WORK(0x01,1, "0x01-工作模式"),

        IP(0x02,2,"0x02 -IP地址和端口"),
       VERSION(0x03,3,"0x03-版本号");

       SettingMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingMode codeOf(Integer num) {
            for (SettingMode type : SettingMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //步进电机系统参数
    public enum SettingStepStatus{

        ALL(0x00,0, "0x00-所有参数"),

        PULSE(0x01,1, "0x01-单圈脉冲"),

        DISTANCE(0x02, 2,"0x02-单圈距离-0.01mm"),

        MAX_DISTANCE(0x03,3, "0x03-最大运行距离-0.01mm"),
        SPEED(0x04,4, "0x04-速度(%)"),
        RETURN_SPEED(0x05,5, "0x05-回原速度(%)"),
        ACCELERATION_TIME(0x06,6, "0x06-加速度时间-ms"),

        ACCELERATION(0x08,8, "0x08-加加速度"),

        ZERO(0x0A,10,"0x0A 原点方向");

        SettingStepStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;

        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingStepStatus codeOf(Integer num) {
            for (SettingStepStatus type : SettingStepStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //伺服电机系统参数
    public enum SettingServoStatus{
        ALL(0x00,0, "0x00-所有参数"),
        PULSE(0x01,1, "0x01-单圈脉冲"),
        DISTANCE(0x02, 2,"0x02-单圈距离-0.01mm"),
        MAX_DISTANCE(0x03,3, "0x03-最大运行距离-0.01mm"),
        SPEED(0x04,4, "0x04-速度(%)"),
        RETURN_SPEED(0x05,5, "0x05-回原速度(%)"),
        ACCELERATION_TIME(0x06,6, "0x06-加速度时间-ms"),
        DECELERATION_TIME(0x07,7, "0x07-减速时间-ms"),
        ACCELERATION(0x08,8, "0x08-加加速度"),
        ZERO(0x0A,10,"0x0A 原点方向");

        SettingServoStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;
        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingServoStatus codeOf(Integer num) {
            for (SettingServoStatus type : SettingServoStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }







    //时间参数的模式
    public enum SettingTimeCabinetMode{
        CABINET_A(0x01,1, "0x01-A柜相关参数"),
        CABINET_B(0x02,2, "0x02-B柜相关参数"),
        CABINET_C(0x03, 3,"0x03-C柜相关参数");

        SettingTimeCabinetMode(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }
        public final Integer code;
        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingTimeCabinetMode codeOf(Integer num) {
            for (SettingTimeCabinetMode type : SettingTimeCabinetMode.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }



    //A柜时间相关参数
    public enum SettingTimeCabinetAStatus{
        IO_OVER_TIME(0x01,1, "0x01-IO拓展板超时时间-10ms"),
        LED_OVER_TIME(0x02,2, "0x02-灯板超时时间-10ms"),
        DROP_SENSOR_OVER_TIME(0x03, 3,"0x03-药匣掉药传感器超时时间-10ms"),
        BELT_SENSOR_OVER_TIME(0x04,4,"0x04-皮带传感器超时时间-10ms"),
        GRATING_SENSOR_OVER_TIME(0x05,5,"0x05-光栅传感器超时时间-10ms");

        SettingTimeCabinetAStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }
        public final Integer code;
        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingTimeCabinetAStatus codeOf(Integer num) {
            for (SettingTimeCabinetAStatus type : SettingTimeCabinetAStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //B柜时间相关参数
    public enum SettingTimeCabinetBStatus{
        AUTO_DRUG_CHECK_OVER_TIME(0x01,1, "0x01-自动上药超时时间-10ms"),
        TABLE_CHECK_OVER_TIME(0x02,2, "0x02-检测平台检测超时时间-10ms"),
        TABLE_RUN_OVER_TIME(0x03, 3,"0x03 - 检测平台继续执行时间-10ms");

        SettingTimeCabinetBStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;
        public final Integer num;

        public final String desc;

        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingTimeCabinetBStatus codeOf(Integer num) {
            for (SettingTimeCabinetBStatus type : SettingTimeCabinetBStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }

    }

    //C柜时间相关参数
    public enum SettingTimeCabinetCStatus{
        CHECK_SENSOR_OVER_TIME(0x01,1, "0x01-感应药到传感器超时时间 "),
        DROP_SENSOR_OVER_TIME(0x02,2, "0x02-感应药走传感器超时时间-10ms");

        SettingTimeCabinetCStatus(int code,Integer num, String desc) {
            this.code = code;
            this.num = num;
            this.desc = desc;
        }

        public final Integer code;
        public final Integer num;

        public final String desc;
        //反序列化
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static SettingTimeCabinetCStatus codeOf(Integer num) {
            for (SettingTimeCabinetCStatus type : SettingTimeCabinetCStatus.values()) {
                if (type.num.equals(num)) {
                    return type;
                }
            }
            return null;
        }
    }

    //传感器状态
    public enum SensorStatus{
        RESET("00", "传感器不触发"),

        NORMAL("01","传感器触发正常"),


        TIMOUT("02","传感器超时");

        SensorStatus(String code, String desc) {
            this.code = code;

            this.desc = desc;
        }
        public final String code;

        public final String desc;

    }

}
