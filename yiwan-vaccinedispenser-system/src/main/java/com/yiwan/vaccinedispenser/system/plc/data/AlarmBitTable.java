package com.yiwan.vaccinedispenser.system.plc.data;

import com.yiwan.vaccinedispenser.core.common.SettingConstants;
import lombok.Getter;

import static com.yiwan.vaccinedispenser.core.common.SettingConstants.MachineException.SEND;

/**
 * PLC 报警位定义表
 * <p>
 * 每个枚举项包含：地址、位序号、报警名、所属柜、异常码
 *
 * @author slh
 */
@Getter
public enum AlarmBitTable {

    // ==================== A柜报警字1 (210) - 伺服/步进/驱动报警 → SERVO ====================
    A_ALARM1_BELT1(210, 0, "A柜_皮带1伺服报警", "A", SettingConstants.MachineException.BELT, 400),
    A_ALARM1_BELT2(210, 1, "A柜_皮带2伺服报警", "A", SettingConstants.MachineException.BELT, 401),
    A_ALARM1_BELT3(210, 2, "A柜_皮带3伺服报警", "A", SettingConstants.MachineException.BELT, 402),
    A_ALARM1_BELT4(210, 3, "A柜_皮带4伺服报警", "A", SettingConstants.MachineException.BELT, 403),
    A_ALARM1_BELT5(210, 4, "A柜_皮带5伺服报警", "A", SettingConstants.MachineException.BELT, 404),
    A_ALARM1_SMALL_BELT(210, 5, "A柜_小皮带伺服报警", "A", SettingConstants.MachineException.SENDDRUG, 405),
    A_ALARM1_LIFT(210, 6, "A柜_抬升升降伺服报警", "A", SettingConstants.MachineException.SENDDRUG, 406),
    A_ALARM1_CLAMP_STEP(210, 7, "A柜_夹药步进报警", "A", SEND, 407),
    A_ALARM1_CLAMP_FLAP(210, 8, "A柜_夹药挡片报警", "A", SEND, 408),
    A_ALARM1_FEED_X(210, 9, "A柜_进药机械手X轴报警", "A", SEND, 409),
    A_ALARM1_FEED_Z(210, 10, "A柜_进药机械手Z轴报警", "A", SEND, 410),
    A_ALARM1_DISP_X(210, 11, "A柜_出药机械手X轴报警", "A", SettingConstants.MachineException.SENDDRUG, 411),
    A_ALARM1_DISP_Z(210, 12, "A柜_出药机械手Z轴报警", "A", SettingConstants.MachineException.SENDDRUG, 412),
    A_ALARM1_DISP_STEP(210, 13, "A柜_出药机械手伸出步进报警", "A", SettingConstants.MachineException.SENDDRUG, 413),

    // ==================== A柜报警字2 (211) - 通信超时→IO 信号超时→BELT/SEND ====================
    A_ALARM2_BELT_WAIT(211, 0, "A柜_小皮带等苗超时", "A", SettingConstants.MachineException.IO),
    A_ALARM2_BELT_OUT(211, 1, "A柜_小皮带出苗超时", "A", SettingConstants.MachineException.SENDWARING),
    A_ALARM2_FEED_WAIT(211, 2, "A柜_进药机械手等苗信号超时", "A", SEND),
    A_ALARM2_FEED_OUT(211, 3, "A柜_进药机械手出苗信号超时", "A", SEND),
    A_ALARM2_DISP_WAIT(211, 4, "A柜_出药机械手等苗信号超时", "A", SettingConstants.MachineException.SENDWARING),
    A_ALARM2_DISP_OUT(211, 5, "A柜_出药机械手出苗信号超时", "A", SettingConstants.MachineException.SENDWARING),
    A_ALARM2_IO_TIMEOUT(211, 6, "A柜_IO板通信超时", "A", SettingConstants.MachineException.IO),
    A_ALARM2_LED_TIMEOUT(211, 7, "A柜_灯板通信超时", "A", SettingConstants.MachineException.SENDWARING),
    A_ALARM2_SERVO_INIT(211, 8, "A柜_伺服初始化未完成", "A", SettingConstants.MachineException.SENDDRUG),

    // ==================== B柜报警字1 (215) ====================
    B_ALARM1_X(215, 0, "B柜_X伺服报警", "B", SEND, 420),
    B_ALARM1_Y(215, 1, "B柜_Y伺服报警", "B", SEND, 421),
    B_ALARM1_Z(215, 2, "B柜_Z伺服报警", "B", SEND, 422),
    B_ALARM1_ROTATE(215, 3, "B柜_旋转步进报警", "B", SEND, 423),
    B_ALARM1_CONVEY(215, 4, "B柜_输送伺服报警", "B", SEND, 424),
    B_ALARM1_SIDE_CAM(215, 5, "B柜_侧面相机通信超时", "B", SEND),
    B_ALARM1_TOP_CAM(215, 6, "B柜_顶部相机通信超时", "B", SettingConstants.MachineException.SEND),
    B_ALARM1_BOTTOM_CAM(215, 7, "B柜_底部相机通信超时", "B", SettingConstants.MachineException.SEND),
    B_ALARM1_LEFT_RANGE(215, 8, "B柜_左测距信号通信超时", "B", SettingConstants.MachineException.SEND),
    B_ALARM1_RIGHT_RANGE(215, 9, "B柜_右测距信号通信超时", "B", SettingConstants.MachineException.SEND),
    B_ALARM1_THICK_RANGE(215, 10, "B柜_测厚距信号通信超时", "B", SettingConstants.MachineException.SEND),
    B_ALARM1_SCAN_FAIL(215, 11, "B柜_连续扫码失败", "B", SEND),
    B_ALARM1_JUDGE_FAIL(215, 12, "B柜_连续长宽高判断失败", "B", SEND),
    B_ALARM1_START_SIGNAL(215, 13, "B柜_启动异常_进药机械手出有信号", "B", SEND),

    // ==================== B柜报警字2 (216) ====================
    B_ALARM2_SERVO_INIT(216, 0, "B柜_伺服初始化未完成", "B", SettingConstants.MachineException.SEND),

    // ==================== C柜报警字1 (220) ====================
    C_ALARM1_SLOPE(220, 0, "C柜_斜坡伺服报警", "C", SettingConstants.MachineException.SENDDRUG, 430),
    C_ALARM1_IN_WAIT(220, 1, "C柜_进苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_ALARM1_OUT_WAIT(220, 2, "C柜_出苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_ALARM1_SERVO_INIT(220, 3, "C柜_伺服初始化未完成", "C", SettingConstants.MachineException.SEND),
    C_ALARM1_SLOPE_STUCK(220, 4, "C柜_斜坡检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_ALARM1_BOTTOM_ABNORMAL(220, 5, "C柜_底部信号异常", "C", SettingConstants.MachineException.SENDWARING),

    // ==================== C柜报警字2~7 (221~226) 工作台1~6 ====================
    C_W1_STEP(221, 0, "C柜工作台1_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 433),
    C_W1_LIFT(221, 2, "C柜工作台1_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 432),
    C_W1_CONVEY(221, 3, "C柜工作台1_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 431),
    C_W1_SERVO_INIT(221, 6, "C柜工作台1_伺服初始化未完成", "C", SettingConstants.MachineException.SENDWARING),
    C_W1_SIGNAL_STUCK(221, 7, "C柜工作台1_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W1_RAMP_TIMEOUT(221, 9, "C柜_去工作台1_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W1_STATION1_TIMEOUT(221, 10, "C柜_去工作台1_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),

    C_W2_STEP(222, 0, "C柜工作台2_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 436),
    C_W2_LIFT(222, 2, "C柜工作台2_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 435),
    C_W2_CONVEY(222, 3, "C柜工作台2_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 434),
    C_W2_SERVO_INIT(222, 6, "C柜工作台2_伺服初始化未完成", "C", SettingConstants.MachineException.SENDWARING),
    C_W2_SIGNAL_STUCK(222, 7, "C柜工作台2_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W2_RAMP_TIMEOUT(222, 9, "C柜_去工作台2_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W2_STATION1_TIMEOUT(222, 10, "C柜_去工作台2_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W2_STATION2_TIMEOUT(222, 11, "C柜_去工作台2_工作台2信号超时", "C", SettingConstants.MachineException.SENDWARING),

    C_W3_STEP(223, 0, "C柜工作台3_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 439),
    C_W3_LIFT(223, 2, "C柜工作台3_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 438),
    C_W3_CONVEY(223, 3, "C柜工作台3_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 437),
    C_W3_IN_WAIT(223, 4, "C柜工作台3_进苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_OUT_WAIT(223, 5, "C柜工作台3_出苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_SERVO_INIT(223, 6, "C柜工作台3_伺服初始化未完成", "C", SettingConstants.MachineException.SEND),
    C_W3_SIGNAL_STUCK(223, 7, "C柜工作台3_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_RAMP_TIMEOUT(223, 9, "C柜_去工作台3_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_STATION1_TIMEOUT(223, 10, "C柜_去工作台3_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_STATION2_TIMEOUT(223, 11, "C柜_去工作台3_工作台2信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W3_STATION3_TIMEOUT(223, 12, "C柜_去工作台3_工作台3信号超时", "C", SettingConstants.MachineException.SENDWARING),

    C_W4_STEP(224, 0, "C柜工作台4_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 442),
    C_W4_LIFT(224, 2, "C柜工作台4_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 441),
    C_W4_CONVEY(224, 3, "C柜工作台4_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 440),
    C_W4_SERVO_INIT(224, 6, "C柜工作台4_伺服初始化未完成", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_SIGNAL_STUCK(224, 7, "C柜工作台4_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_RAMP_TIMEOUT(224, 9, "C柜_去工作台4_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_STATION1_TIMEOUT(224, 10, "C柜_去工作台4_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_STATION2_TIMEOUT(224, 11, "C柜_去工作台4_工作台2信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_STATION3_TIMEOUT(224, 12, "C柜_去工作台4_工作台3信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W4_STATION4_TIMEOUT(224, 13, "C柜_去工作台4_工作台4信号超时", "C", SettingConstants.MachineException.SENDWARING),

    C_W5_STEP(225, 0, "C柜工作台5_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 445),
    C_W5_LIFT(225, 2, "C柜工作台5_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 444),
    C_W5_CONVEY(225, 3, "C柜工作台5_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 443),
    C_W5_SERVO_INIT(225, 6, "C柜工作台5_伺服初始化未完成", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_SIGNAL_STUCK(225, 7, "C柜工作台5_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_RAMP_TIMEOUT(225, 9, "C柜_去工作台5_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_STATION1_TIMEOUT(225, 10, "C柜_去工作台5_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_STATION2_TIMEOUT(225, 11, "C柜_去工作台5_工作台2信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_STATION3_TIMEOUT(225, 12, "C柜_去工作台5_工作台3信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_STATION4_TIMEOUT(225, 13, "C柜_去工作台5_工作台4信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W5_STATION5_TIMEOUT(225, 14, "C柜_去工作台5_工作台5信号超时", "C", SettingConstants.MachineException.SENDWARING),

    C_W6_STEP(226, 0, "C柜工作台6_推送步进报警", "C", SettingConstants.MachineException.SENDWARING, 448),
    C_W6_LIFT(226, 2, "C柜工作台6_升降伺服报警", "C", SettingConstants.MachineException.SENDWARING, 447),
    C_W6_CONVEY(226, 3, "C柜工作台6_输送伺服报警", "C", SettingConstants.MachineException.SENDWARING, 446),
    C_W6_IN_WAIT(226, 4, "C柜工作台6_进苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_OUT_WAIT(226, 5, "C柜工作台6_出苗检测信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_SERVO_INIT(226, 6, "C柜工作台6_伺服初始化未完成", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_SIGNAL_STUCK(226, 7, "C柜工作台6_检测信号常亮", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_RAMP_TIMEOUT(226, 9, "C柜_去工作台6_斜坡信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION1_TIMEOUT(226, 10, "C柜_去工作台6_工作台1信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION2_TIMEOUT(226, 11, "C柜_去工作台6_工作台2信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION3_TIMEOUT(226, 12, "C柜_去工作台6_工作台3信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION4_TIMEOUT(226, 13, "C柜_去工作台6_工作台4信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION5_TIMEOUT(226, 14, "C柜_去工作台6_工作台5信号超时", "C", SettingConstants.MachineException.SENDWARING),
    C_W6_STATION6_TIMEOUT(226, 15, "C柜_去工作台6_工作台6信号超时", "C", SettingConstants.MachineException.SENDWARING);

    private final int address;
    private final int bit;
    private final String alarmName;
    private final String cabinet;
    private final int mask;
    private final Integer exceptionCode;
    /** 对应伺服报警编号寄存器地址（400-448），0表示无对应报警编号 */
    private final int alarmCodeAddr;

    AlarmBitTable(int address, int bit, String alarmName, String cabinet, SettingConstants.MachineException exception) {
        this(address, bit, alarmName, cabinet, exception, 0);
    }

    AlarmBitTable(int address, int bit, String alarmName, String cabinet, SettingConstants.MachineException exception, int alarmCodeAddr) {
        this.address = address;
        this.bit = bit;
        this.alarmName = alarmName;
        this.cabinet = cabinet;
        this.mask = 1 << bit;
        this.exceptionCode = exception.code;
        this.alarmCodeAddr = alarmCodeAddr;
    }
}
