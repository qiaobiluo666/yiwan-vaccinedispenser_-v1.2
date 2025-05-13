package com.yiwan.vaccinedispenser.core.common.emun;

/**
 * @author zhw
 * @date 2023/6/16
 * @Description
 */
public interface RedisKeyConstant {

    /**
     * 发药处方
     */
    //五层皮带的掉药列表
    String DROP_LIST = "Dispensing:fiveDrop:%s";

    //运输皮带队列
    String BELT_LIST = "Dispensing:beltList";

    //掉药回调队列
    String SEND_LIST = "Dispensing:sendEndList";

    //IP设置
    String IP_SET = "Machine:IPWorkMode";


    /**
     * 机器判断状态
     */
    //发送命令时存储命令的key
    String FRAME_NUMBER_COMMAND= "Machine:frameNumberCommand%s";

    //A柜是否收到00
    String CABINET_A_RECEIVE_MSG = "Machine:cabinetAReceiveMsg";

    //B柜是否收到00
    String CABINET_B_RECEIVE_MSG = "Machine:cabinetBReceiveMsg";

    //C柜是否收到00
    String CABINET_C_RECEIVE_MSG = "Machine:cabinetCReceiveMsg";

    //C柜皮带是否停止
    String CABINET_C_BELT_STOP = "Dispensing:BeltStatus:";

    //C柜挡片状态
    String CABINET_C_BLOCK_STATUS = "Dispensing:BlockStatus:";


    //C柜挡片是否在查询
    String CABINET_C_BLOCK_STATUS_QUERY = "Dispensing:BlockStatusQuery:";

    //A柜电磁阀输出是否完成
    String CABINET_A_IO_OUTPUT_STATUS =  "Machine:IOStatus:%s";

    //A柜是否进入发药队列
    String CABINET_A_DRUG_LIST = "Machine:drugHaveList:%s";


    //A柜皮带上是否有药
    String CABINET_A_BELT_HAVE_DRUG = "Machine:beltHaveDrug:%s";


    //A柜是否可以继续掉药
    String CABINET_A_CAN_DROP_DRUG = "Machine:canDropDrug";

    //A柜皮带上是否有药
    String CABINET_A_GS_BELT_HAVE_DRUG = "Machine:GsBeltHaveDrug";


    //A柜皮带上的药是否已经停在边缘
    String CABINET_A_BELT_STOP_DRUG = "Machine:beltStopDrug:%s";

    //A柜伺服报警功能
    String CABINET_A_SERVO_IS_ERROR ="Machine:cabinetA:servoIsError:%s";




    //扫码步进电机是否运动完成
    String CABINET_B_SCAN_STEP_STATUS = "Machine:scanStepStatus";

    //夹爪步进电机是否运动完成
    String CABINET_A_CLAMP_STEP_STATUS = "Machine:clampStepStatus";

    //挡片步进电机是否运动完成
    String CABINET_A_BLOCK_STEP_STATUS = "Machine:blockStepStatus";


    //疫苗信息
    String CABINET_B_DRUG_MSG = "Machine:autoDrug:drugMsg";

    //是否可以扫码
    String CABINET_B_TEST_SCAN = "Machine:autoDrug:Status:testScan";


    //是否可以继续掉药
    String CABINET_B_TEST_RUN = "Machine:autoDrug:Status:testRun";

    //是否可以开始检测有多个药
    String CABINET_B_TEST_DRUGS_START = "Machine:autoDrug:Status:drugsStarts";

    //检测结果
    String CABINET_B_TEST_DRUGS_RESULT= "Machine:autoDrug:Status:drugsResult";

    String CABINET_B_TEST_DRUGS_RESULT_IS_END = "Machine:autoDrug:Status:drugsResultEnd";

    //是否可以进入掉药区域
    String HANDLE_IS_DROP = "Machine:autoDrug:Status:handleGoToDrop";

    //B柜机械手回原点
    String CABINET_B_INIT = "Machine:autoDrug:Status:cabinetInit";


    //B柜机械手回原点
    String CABINET_B_COUNT= "Machine:autoDrug:Count";




    //B柜机械手回原点
    String CABINET_B_ERROR_COUNT= "Machine:autoDrug:errorCount";


    //C柜 工作台是否还在运动
    String CABINET_C_WORK = "Machine:autoDrug:Status:cabinetCWork";

    //机械手线程是否运动完毕
    String CABINET_A_HANDLE_IS_MOVE_END = "Machine:autoDrug:Status:handleMoveEnd";

    String BELT_SERVO_STATUS = "Machine:sendDrug:ServoMoveStatus:%s";

    String CABINET_C_RESET = "Machine:cabinetC:isReset:%s";

    String CABINET_B_SERVO_ERROR = "Machine:cabinetB:isServoError";



    String  CABINET_C_BLANK_OPEN_TIME = "Machine:cabinetC:blankIsOpenTime";

    //手动上药状态
    interface handDrugStatus{
        String HAND_START_STATUS = "Machine:handleDrug:start";

        String HAND_LAST_OPERATION_TIME = "Machine:handleDrug:ledTime";

        String HAND_LAST_LED_NUM_STATUS = "Machine:handleDrug:lastLed";

        String HAND_TIMEOUT_LED_STATUS = "Machine:handleDrug:timeOut";

    }





    interface servoGetDistance{
        String CABINET_A = "Machine:Servo:Distance_A";
        String CABINET_B =  "Machine:Servo:Distance_B";

        String CABINET_C =  "Machine:Servo:Distance_C";
    }

    interface sensor{

        //A柜传感器 SensorStatus
        //运输小皮带传感器状态 (光栅传感器) SensorStatus
        String BELT_SENSOR = "Machine:sensor:belt";

        //B柜滑台传感器 true false
        String TABLE_SENSOR = "Machine:sensor:table";


        //B柜滑台传感器 true false
        String TABLE_SENSOR_COUNT = "Machine:sensor:tableCount";


        //B柜机械手 传感器 SensorStatus
        String HAND_SENSOR = "Machine:sensor:hand";





    }


    //自动上药需要的redis 状态
    interface  autoDrug{
        //上药药流程是否启动
        String AUTO_DRUG_START = "Machine:autoDrug:start";

        String AUTO_DRUG_BELT_START = "Machine:autoDrug:beltStart";

        String AUTO_DRUG_BELT_FINISH =  "Machine:autoDrug:beltFinish";

    }







    //机械手是否回原
    interface handInit{
        //X回原
        String HAND_SERVO_X_INIT =  "Machine:autoDrug:handInit:servoX";
        //Z回原
        String HAND_SERVO_Z_INIT =  "Machine:autoDrug:handInit:servoZ";

        //挡片步进回原
        String HAND_STEP_INIT =  "Machine:autoDrug:handInit:step";

    }


    /**
     * 相机状态
     */
    interface cameraStatus{
        String ABOVE = "Machine:Status:camera:above";
        String SIDE = "Machine:Status:camera:side";

        String BELOW = "Machine:Status:camera:below";
    }

    /**
     * 控制板状态
     */

    interface controlStatus{
        String CABINET_A = "Machine:Status:control:cabinetA";
        String CABINET_B = "Machine:Status:control:cabinetB";

        String CABINET_C = "Machine:Status:control:cabinetC";
    }



    /**
     * 扫码的状态
     */
    interface scanCode{
        String ABOVE = "Machine:autoDrug:scanCode:above";

        String LAST_ABOVE = "Machine:autoDrug:scanCode:lastAbove";

        String SIDE = "Machine:autoDrug:scanCode:side";

        String LAST_SIDE = "Machine:autoDrug:scanCode:lastSide";




        String BELOW = "Machine:autoDrug:scanCode:below";

        String LAST_BELOW = "Machine:autoDrug:scanCode:lastBelow";



    }

    interface distanceStart{
        String HIGH =  "Machine:autoDrug:getDistance:highStart";
        String LEFT =  "Machine:autoDrug:getDistance:leftStart";
        String RIGHT =  "Machine:autoDrug:getDistance:rightStart";
        String COUNT = "Machine:autoDrug:getDistance:countStart";
    }


//    A5 5A 0E 00 01 00 0B 02 01 03 00 0E 12 00 00 01 AE F1 55
//    A5 5A 10 00 01 00 0B 02 01 02 00 B8 C1 03 00 01 00 00 30 1C 55
    interface distance{
        String HIGH =  "Machine:autoDrug:getDistance:high";
        String LEFT =  "Machine:autoDrug:getDistance:left";
        String RIGHT =  "Machine:autoDrug:getDistance:right";

        String COUNT = "Machine:autoDrug:getDistance:count";
    }



    //扫码伺服的运动状态
    interface scanServo{
        String X ="Machine:autoDrug:scanServo:X";
        String Y ="Machine:autoDrug:scanServo:Y";
        String Z ="Machine:autoDrug:scanServo:Z";
    }



    //扫码伺服的运动状态
    interface handServo{
        String X ="Machine:autoDrug:handServo:X";
        String Z ="Machine:autoDrug:handServo:Z";
    }





}
