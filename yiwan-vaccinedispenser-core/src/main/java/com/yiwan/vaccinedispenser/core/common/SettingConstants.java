package com.yiwan.vaccinedispenser.core.common;

import lombok.Data;

/**
 * @Author : slh
 * @create 2023/5/17
 * @Description 指令的常量
 */
@Data
public class SettingConstants {


    public enum MachineException {
        IO(1, "IO超时"),
        BELT(2,"皮带超时"),
        SERVO(3,"伺服报警"),
        SEND(4,"自动上药报警"),

        HAND(6,"手动上药报警"),


        SENDDRUG(7,"发药报警"),

        SENDWARING(9,"发药机警告！"),


        COUNTWARING(10,"自动盘点报警！"),

        ZCY(8,"政采云报警"),


        OTHER(5,"其他报警");

        MachineException(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        public final Integer code;
        public final String desc;

    }





    /**
     * 逻辑等待时间
     */


    //发送命令最大等待超时时间 3s
    public static final int COMMAND_WAIT_TIMEOUT = 1000;

    //重发命令最多几次
    public static final int COMMAND_WAIT_TIMES = 3;

    //掉药输出等待时间
    public static final int DRUG_INPUT_WAIT_TIME = 1500;

    //等待药品掉落皮带时间
    public static final int DRUG_DROP_WAIT_TIME = 1000;


    //电磁铁开启和关闭的间隔时间
    public static final int IO_DROP_WAIT_TIME = 16;

    //等待药品掉落光栅传感器时间
    public static final int DRUG_BELT_WAIT_TIME = 10000;

    //等待药品掉到运输皮带时间
    public static final int WORK_DRUG_BELT_WAIT_TIME = 10000;

    //查询C柜斜坡是否停止
    public static final int FIND_BELT_STOP_WAIT_TIME = 60000;

    //等待药品滑台反转掉落到皮带时间
    public static final int RETURN_DRUG_TABLE_WAIT_TIME = 4000;


    //侧边扫码相机扫描时长
    public static final int SCAN_STEP_SIDE_WAIT_TIME = 1500;

    //下方扫码相机扫描时长
    public static final int SCAN_STEP_BELOW_WAIT_TIME = 2000;

    //等待读取距离最大时间
    public static final int GET_DISTANCE_WAIT_TIME = 1000;



    //等待药品掉到机械手最大时间
    public static final int     SCAN_SERVO_WAIT_TIME = 10000;

    //等待药品掉到机械手最大时间
    public static final int DRUG_DROP_HAND_WAIT_TIME = 20000;


    //检测机械手上是否有药参数
    public static final int DRUG_HAVE_HAND_WAIT_TIME = 5000;

    //等待药品掉入仓位等待的时间
    public static final int WAIT_DROP_TIME = 30000;

    //等待药品掉入仓位等待的时间
    public static final int WAIT_BLOCK_TIME = 10000;

    //等待扫码步进电机运动最大时间
    public static final int SCAN_STEP_WAIT_TIME = 6000;

    //等待Z轴吸盘吸住药
    public static final int SERVO_Z_WAIT_TIME = 60000;

    //等待扫码步进电机运动最大时间
    public static final int CABINET_INIT_WAIT_TIME = 3000;


    /**
     * 传感器编号、输出位置编号
     */

    //光栅传感器编号
    public static final  int SENSOR_CABINET_A_MOVE_BELT_NUM = 22;

    //运输皮带  伺服控制编号
    public static final  int CABINET_A_MOVE_BELT_TO_C_NUM = 6;

    //运输皮带上下抬升的伺服编号
    public static final  int CABINET_A_MOVE_BELT_TO_RETURN_NUM = 9;

    //B柜滑台传感器信号 编号
    public static final  int SENSOR_CABINET_B_TABLE_NUM = 18;


    //A柜机械手掉药传感器信号 编号
    public static final  int SENSOR_CABINET_A_HAND_NUM = 17;

    //A柜震动电机
    public static final  int CABINET_A_SUCKER_NUM = 4;


    //B柜吸盘输出信号编号
    public static final  int CABINET_B_SUCKER_NUM = 5;

    //B柜吸盘继电器输出信号
    public static final  int CABINET_B_SUCKER_END_NUM = 4;


    //直角跟原点坐标的角度
    public static final double CABINET_B_TABLE_ANGLE = 45.0;

    //直角的点 相对于原点的X坐标
    public static final int CABINET_B_TABLE_X = 6400;
    //直角的点 相对于原点的Y坐标
    public static final int CABINET_B_TABLE_Y = 10000;

    public static final int CABINET_B_TABLE_Z = 23000;

    //A柜仓位最长距离
    public static final int CABINET_A_BOX_LEN= 1120;


    //旋转角度
    public interface AngleDistance{
        //旋转一直角
        int STRAIGHT = 900;

        int RETURN = 1800;

        int ALL = 2700;


        int LEFT_RETURN = 450;


        int RIGHT_RETURN =1400;
    }




}
