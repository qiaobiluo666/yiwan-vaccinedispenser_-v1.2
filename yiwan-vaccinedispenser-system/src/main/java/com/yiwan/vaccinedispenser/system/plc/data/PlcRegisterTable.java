package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * PLC寄存器元数据表 (仅保留有意义的地址)
 * 每个地址对应: 参数名、中文语义、数据长度(寄存器个数)
 * 
 * @author yiwan
 */
@Getter
public enum PlcRegisterTable {

    STATUS_WORD_1(200, "statusWord1", "状态字1", 1),
    STATUS_WORD_2(201, "statusWord2", "状态字2", 1),
    STATUS_WORD_3(202, "statusWord3", "状态字3", 1),
    STATUS_WORD_4(203, "statusWord4", "状态字4", 1),
    A_RETURN_WORKBENCH(61, "aReturnWorkbench", "A柜退苗工作台", 1),
    A_RETURN_LAYER(62, "aReturnLayer", "A柜退苗层号", 1),
    A_RETURN_INDEX(63, "aReturnIndex", "A柜退苗序号", 1),
    A_RETURN_TIMES(64, "aReturnTimes", "A柜退苗次数", 1),
    A_ALARM_1(210, "aAlarm1", "A柜报警字1", 1),
    A_ALARM_2(211, "aAlarm2", "A柜报警字2", 1),
    A_ALARM_3(212, "aAlarm3", "A柜报警字3", 1),
    A_ALARM_4(213, "aAlarm4", "A柜报警字4", 1),
    A_ALARM_5(214, "aAlarm5", "A柜报警字5", 1),
    B_ALARM_1(215, "bAlarm1", "B柜报警字1", 1),
    B_ALARM_2(216, "bAlarm2", "B柜报警字2", 1),
    B_ALARM_3(217, "bAlarm3", "B柜报警字3", 1),
    B_ALARM_4(218, "bAlarm4", "B柜报警字4", 1),
    B_ALARM_5(219, "bAlarm5", "B柜报警字5", 1),
    C_ALARM_1(220, "cAlarm1", "C柜报警字1", 1),
    C_ALARM_2(221, "cAlarm2", "C柜报警字2", 1),
    C_ALARM_3(222, "cAlarm3", "C柜报警字3", 1),
    C_ALARM_4(223, "cAlarm4", "C柜报警字4", 1),
    C_ALARM_5(224, "cAlarm5", "C柜报警字5", 1),
    C_ALARM_6(225, "cAlarm6", "C柜报警字6", 1),
    C_ALARM_7(226, "cAlarm7", "C柜报警字7", 1),
    B_JUDGE_LENGTH(230, "bJudgeLength", "B柜判断长", 1, "单位(0.1mm)"),
    B_JUDGE_WIDTH(231, "bJudgeWidth", "B柜判断宽", 1, "单位(0.1mm)"),
    B_JUDGE_HEIGHT(232, "bJudgeHeight", "B柜判断厚", 1, "单位(0.1mm)"),
    B_JUDGE_BARCODE(233, "bJudgeBarcode", "B柜判断条码", 25),
    A_PAN_DISTANCE(270, "aPanDistance", "A柜盘苗距离", 1, "单位(1mm)"),
    A_BELT_ALARM_SRC_LAYER(276, "aBeltAlarmSrcLayer", "A柜到小皮带异常来源层", 1, "1-10"),
    A_BELT_ALARM_SRC_INDEX(277, "aBeltAlarmSrcIndex", "A柜到小皮带异常来源序号", 1, "1-24"),
    A_FILL_LAYER(280, "aFillLayer", "A柜进药完成层", 1),
    A_FILL_INDEX(281, "aFillIndex", "A柜进药完成序号", 1),
    A_FILL_BARCODE(282, "aFillBarcode", "A柜进药完成条码", 25),
    C_SEND_SRC_LAYER(310, "cSendSrcLayer", "C柜送药完成来源层", 1),
    C_SEND_SRC_INDEX(311, "cSendIndex", "C柜送药完成来源序号", 1),
    C_SEND_TARGET(312, "cSendTarget", "C柜送药完成目标工作台", 1),
    C_SEND_ACTUAL_TARGET(313, "cSendActualTarget", "C柜送药完成实际送达工作台", 1),

    // ========== 报警编号 400~448 ==========
    A_BELT_1_ALARM(400, "aBelt1Alarm", "A柜皮带1伺服报警编号", 1),
    A_BELT_2_ALARM(401, "aBelt2Alarm", "A柜皮带2伺服报警编号", 1),
    A_BELT_3_ALARM(402, "aBelt3Alarm", "A柜皮带3伺服报警编号", 1),
    A_BELT_4_ALARM(403, "aBelt4Alarm", "A柜皮带4伺服报警编号", 1),
    A_BELT_5_ALARM(404, "aBelt5Alarm", "A柜皮带5伺服报警编号", 1),
    A_SMALL_BELT_ALARM(405, "aSmallBeltAlarm", "A柜小皮带伺服报警编号", 1),
    A_LIFT_ALARM(406, "aLiftAlarm", "A柜抬升升降伺服报警编号", 1),
    A_CLAMP_STEP_ALARM(407, "aClampStepAlarm", "A柜夹药步进报警编号", 1),
    A_CLAMP_FLAP_ALARM(408, "aClampFlapAlarm", "A柜夹药挡片报警编号", 1),
    A_FEED_X_ALARM(409, "aFeedXAlarm", "A柜进药机械手X轴报警编号", 1),
    A_FEED_Z_ALARM(410, "aFeedZAlarm", "A柜进药机械手Z轴报警编号", 1),
    A_DISP_X_ALARM(411, "aDispXAlarm", "A柜出药机械手X轴报警编号", 1),
    A_DISP_Z_ALARM(412, "aDispZAlarm", "A柜出药机械手Z轴报警编号", 1),
    A_DISP_STEP_ALARM(413, "aDispStepAlarm", "A柜出药机械手伸出步进报警编号", 1),
    R414(414, "", "", 1),
    R415(415, "", "", 1),
    R416(416, "", "", 1),
    R417(417, "", "", 1),
    R418(418, "", "", 1),
    R419(419, "", "", 1),
    B_X_ALARM(420, "bXAlarm", "B柜X伺服报警编号", 1),
    B_Y_ALARM(421, "bYAlarm", "B柜Y伺服报警编号", 1),
    B_Z_ALARM(422, "bZAlarm", "B柜Z伺服报警编号", 1),
    B_ROTATE_ALARM(423, "bRotateAlarm", "B柜旋转步进报警编号", 1),
    B_CONVEY_ALARM(424, "bConveyAlarm", "B柜输送伺服报警编号", 1),
    R425(425, "", "", 1),
    R426(426, "", "", 1),
    R427(427, "", "", 1),
    R428(428, "", "", 1),
    R429(429, "", "", 1),
    C_SLOPE_ALARM(430, "cSlopeAlarm", "C柜斜坡伺服报警编号", 1),
    C_W1_CONVEY_ALARM(431, "cW1ConveyAlarm", "C柜工位1输送伺服报警编号", 1),
    C_W1_LIFT_ALARM(432, "cW1LiftAlarm", "C柜工位1升降伺服报警编号", 1),
    C_W1_STEP_ALARM(433, "cW1StepAlarm", "C柜工位1步进报警编号", 1),
    C_W2_CONVEY_ALARM(434, "cW2ConveyAlarm", "C柜工位2输送伺服报警编号", 1),
    C_W2_LIFT_ALARM(435, "cW2LiftAlarm", "C柜工位2升降伺服报警编号", 1),
    C_W2_STEP_ALARM(436, "cW2StepAlarm", "C柜工位2步进报警编号", 1),
    C_W3_CONVEY_ALARM(437, "cW3ConveyAlarm", "C柜工位3输送伺服报警编号", 1),
    C_W3_LIFT_ALARM(438, "cW3LiftAlarm", "C柜工位3升降伺服报警编号", 1),
    C_W3_STEP_ALARM(439, "cW3StepAlarm", "C柜工位3步进报警编号", 1),
    C_W4_CONVEY_ALARM(440, "cW4ConveyAlarm", "C柜工位4输送伺服报警编号", 1),
    C_W4_LIFT_ALARM(441, "cW4LiftAlarm", "C柜工位4升降伺服报警编号", 1),
    C_W4_STEP_ALARM(442, "cW4StepAlarm", "C柜工位4步进报警编号", 1),
    C_W5_CONVEY_ALARM(443, "cW5ConveyAlarm", "C柜工位5输送伺服报警编号", 1),
    C_W5_LIFT_ALARM(444, "cW5LiftAlarm", "C柜工位5升降伺服报警编号", 1),
    C_W5_STEP_ALARM(445, "cW5StepAlarm", "C柜工位5步进报警编号", 1),
    C_W6_CONVEY_ALARM(446, "cW6ConveyAlarm", "C柜工位6输送伺服报警编号", 1),
    C_W6_LIFT_ALARM(447, "cW6LiftAlarm", "C柜工位6升降伺服报警编号", 1),
    C_W6_STEP_ALARM(448, "cW6StepAlarm", "C柜工位6步进报警编号", 1);

    /** 查询地址范围起始 */
    public static final int START_ADDRESS = 200;
    /** 全部寄存器范围 320-200+1=121 */
    public static final int REGISTER_COUNT = 121;
    /** 条码寄存器连续数量 */
    public static final int BARCODE_SIZE = 25;

    private final int address;
    private final String paramName;
    private final String chineseDesc;
    private final int dataLength;
    private final String unit;

    PlcRegisterTable(int address, String paramName, String chineseDesc, int dataLength) {
        this.address = address;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.dataLength = dataLength;
        this.unit = "";
    }

    PlcRegisterTable(int address, String paramName, String chineseDesc, int dataLength, String unit) {
        this.address = address;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.dataLength = dataLength;
        this.unit = unit != null ? unit : "";
    }

    public boolean isBarcode() {
        return dataLength == BARCODE_SIZE;
    }

    public boolean hasUnit() {
        return unit != null && !unit.isEmpty();
    }

    public static PlcRegisterTable findByAddress(int address) {
        for (PlcRegisterTable entry : values()) {
            if (entry.address == address) {
                return entry;
            }
        }
        return null;
    }
}
