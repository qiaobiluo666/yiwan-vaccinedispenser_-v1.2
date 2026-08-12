package com.yiwan.vaccinedispenser.system.plc.data;

import lombok.Getter;

/**
 * 控制字2 位定义 (地址1)
 * <p>
 * 每位对应一个结果反馈，上位机写入对应位后PLC处理完成自动清0
 *
 * @author yiwan
 */
@Getter
public enum ControlWord2 {

    B_JUDGE_QUALIFIED(0, "bJudgeQualified", "B柜_长宽厚合格", 0x0001, "PLC收到后由PLC写0"),
    B_JUDGE_UNQUALIFIED(1, "bJudgeUnqualified", "B柜_长宽厚不合格", 0x0002, "PLC收到后由PLC写0"),
    B_SEND_QUALIFIED(2, "bSendQualified", "B柜_送药合格", 0x0004, "PLC收到后由PLC写0"),
    B_SEND_UNQUALIFIED(3, "bSendUnqualified", "B柜_送药不合格", 0x0008, "PLC收到后由PLC写0"),
    B_JUDGE_TEST(4, "bJudgeTest", "B柜_测试长宽高", 0x0010, "PLC测试完成后请求判断长宽高，上位机收到后给合格或者不合格，PLC再清数据");

    /** 位序号 (0~15) */
    private final int bit;
    /** 参数名 */
    private final String paramName;
    /** 中文语义 */
    private final String chineseDesc;
    /** 十六进制掩码 */
    private final int mask;
    /** 备注 */
    private final String remark;

    ControlWord2(int bit, String paramName, String chineseDesc, int mask, String remark) {
        this.bit = bit;
        this.paramName = paramName;
        this.chineseDesc = chineseDesc;
        this.mask = mask;
        this.remark = remark;
    }
}
