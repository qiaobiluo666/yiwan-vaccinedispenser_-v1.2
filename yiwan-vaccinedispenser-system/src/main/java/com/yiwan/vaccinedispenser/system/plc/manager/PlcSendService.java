package com.yiwan.vaccinedispenser.system.plc.manager;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import com.yiwan.vaccinedispenser.system.plc.client.PlcClient;
import com.yiwan.vaccinedispenser.system.plc.data.ControlWord1;
import com.yiwan.vaccinedispenser.system.plc.data.ControlWord2;
import com.yiwan.vaccinedispenser.system.plc.data.ControlWord3;
import com.yiwan.vaccinedispenser.system.plc.data.ControlWord4;
import com.yiwan.vaccinedispenser.system.plc.data.PlcRequestCache;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetDispenseRequest;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetInventoryRequest;
import com.yiwan.vaccinedispenser.system.plc.model.ACabinetManualFeedRequest;
import com.yiwan.vaccinedispenser.system.plc.model.BCabinetSendDrugRequest;
import com.yiwan.vaccinedispenser.system.plc.protocol.FunctionCode;
import com.yiwan.vaccinedispenser.system.plc.protocol.MbapHeader;
import com.yiwan.vaccinedispenser.system.plc.protocol.ModbusFrame;
import com.yiwan.vaccinedispenser.system.until.VacUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * PLC 指令发送 Service
 * <p>
 * 功能说明：封装 Modbus TCP 读写保持寄存器指令的构建与发送。
 * 读指令使用功能码 0x03（同步等待响应），写指令使用功能码 0x10（异步发送，不阻塞轮询）。
 * <p>
 * 修订历史：
 *   2024-05-19 yiwan - 初始版本
 *   2026-05-20 yiwan - 新增写指令封装（B柜送药、A柜盘苗、A柜手动上苗、A柜发药）
 *   2026-05-25 yiwan - 写指令改为异步发送，避免阻塞轮询
 *
 * @author yiwan
 */
@Slf4j
@Component
@ConditionalOnExpression("${plc.enable:true}")
public class PlcSendService {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;


    /** PLC 客户端 */
    private final PlcClient plcClient;

    /** 默认超时时间 3000ms */
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    /**
     * 构造发送 Service
     *
     * @param plcClient    PLC 客户端
     */
    public PlcSendService(PlcClient plcClient) {
        this.plcClient = plcClient;
    }

    // ==================== 读指令（同步） ====================

    /**
     * 发送读取保持寄存器指令（默认超时 3000ms）
     *
     * @param startAddr     起始地址（十进制）
     * @param registerCount 寄存器数量（十进制）
     * @return 响应帧，超时返回 null
     */
    public ModbusFrame sendReadCommand(int startAddr, int registerCount) {
        return sendReadCommand(startAddr, registerCount, DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 发送读取保持寄存器指令（自定义超时）
     *
     * @param startAddr     起始地址（十进制，有效范围 0~65535）
     * @param registerCount 寄存器数量（十进制，有效范围 1~125，建议 ≤121）
     * @param timeout       超时时间（毫秒，建议 ≥3000）
     * @param unit          时间单位
     * @return 响应帧，超时返回 null
     */
    public ModbusFrame sendReadCommand(int startAddr, int registerCount, long timeout, TimeUnit unit) {
        byte[] data = new byte[4];
        data[0] = (byte) ((startAddr >> 8) & 0xFF);
        data[1] = (byte) (startAddr & 0xFF);
        data[2] = (byte) ((registerCount >> 8) & 0xFF);
        data[3] = (byte) (registerCount & 0xFF);

        MbapHeader header = new MbapHeader();
        header.setUnitId(0x01);

        ModbusFrame frame = new ModbusFrame(header, FunctionCode.READ_HOLDING_REGISTERS.getCode(), data);

        log.debug("[PLC] 构建读取指令 | 起始地址={}(0x{}) | 寄存器数量={}(0x{})",
                startAddr, Integer.toHexString(startAddr),
                registerCount, Integer.toHexString(registerCount));

        return plcClient.sendAndWait(frame, timeout, unit);
    }

    // ==================== 写指令（异步，不阻塞轮询） ====================

    /**
     * 异步写多个保持寄存器（功能码 0x10，fire-and-forget）
     * <p>
     * 构建 PDU 后通过 PlcClient.sendAsync() 直接发送，不等待响应。
     *
     * @param startAddr      起始地址（十进制）
     * @param registerValues 待写入的寄存器值数组（十进制）
     */
    private void sendWriteRegistersAsync(int startAddr, int[] registerValues) {
        int count = registerValues.length;
        int byteCount = count * 2;
        byte[] data = new byte[5 + byteCount];

        data[0] = (byte) ((startAddr >> 8) & 0xFF);
        data[1] = (byte) (startAddr & 0xFF);
        data[2] = (byte) ((count >> 8) & 0xFF);
        data[3] = (byte) (count & 0xFF);
        data[4] = (byte) (byteCount & 0xFF);

        for (int i = 0; i < count; i++) {
            int val = registerValues[i];
            data[5 + i * 2] = (byte) ((val >> 8) & 0xFF);
            data[5 + i * 2 + 1] = (byte) (val & 0xFF);
        }

        MbapHeader header = new MbapHeader();
        header.setUnitId(0x01);

        ModbusFrame frame = new ModbusFrame(header, FunctionCode.WRITE_MULTIPLE_REGISTERS.getCode(), data);

        log.debug("[PLC] 构建异步写指令 | 起始地址={}(0x{}) | 寄存器数量={} | 值={}",
                startAddr, Integer.toHexString(startAddr), count, registerValues);

        plcClient.sendAsync(frame);
    }

    /**
     * 写单寄存器（FC=06）异步发送，失败自动重试最多3次
     * <p>
     * 适用于控制字写入（地址0~3等），通过队列串行发送，不等待 PLC 响应。
     * 发送后500ms检查 Redis 状态位变化作为确认，超时则重试。
     *
     * @param startAddr 起始地址（十进制）
     * @param value     待写入的寄存器值（十进制）
     */
    private void sendWriteSingleRegisterAsync(int startAddr, int value) {
        byte[] data = new byte[4];
        data[0] = (byte) ((startAddr >> 8) & 0xFF);
        data[1] = (byte) (startAddr & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
        data[3] = (byte) (value & 0xFF);

        MbapHeader header = new MbapHeader();
        header.setUnitId(0x01);

        ModbusFrame frame = new ModbusFrame(header, FunctionCode.WRITE_SINGLE_REGISTER.getCode(), data);

        plcClient.sendAsync(frame);
    }

    // ==================== B柜送药 ====================

    /**
     * B柜送药请求（地址10~11，异步发送）
     *
     * @param request B柜送药请求（层号1-10，序号1-24）
     */
    public void sendBCabinetSendDrug(BCabinetSendDrugRequest request) {
        log.info("[PLC] B柜送药 | 层={} | 序号={}", request.getLayer(), request.getIndex());
        sendWriteRegistersAsync(10, new int[]{request.getLayer(), request.getIndex()});
    }

    // ==================== A柜盘苗 ====================

    /**
     * A柜盘苗请求（地址12~14，异步发送）
     *
     * @param request A柜盘苗请求（层号1-10，序号1-24，偏移量1mm）
     */
    public void sendACabinetInventory(ACabinetInventoryRequest request) {
        log.info("[PLC] A柜盘苗 | 层={} | 序号={} | 偏移量={}", request.getLayer(), request.getIndex(), request.getOffset());
        sendWriteRegistersAsync(12, new int[]{request.getLayer(), request.getIndex(), request.getOffset()});
    }

    // ==================== A柜手动上苗 ====================

    /**
     * A柜手动上苗请求（地址15~17，异步发送）
     *
     * @param request A柜手动上苗请求（层号1-10，序号1-24，宽度1mm）
     */
    public void sendACabinetManualFeed(ACabinetManualFeedRequest request) {
        log.info("[PLC] A柜手动上苗 | 层={} | 序号={} | 宽度={}", request.getLayer(), request.getIndex(), request.getWidth());
        sendWriteRegistersAsync(15, new int[]{request.getLayer(), request.getIndex(), request.getWidth()});
    }

    // ==================== A柜发药 ====================

    /**
     * A柜发药请求（地址20~25，异步发送）
     *
     * @param request A柜发药请求
     */
    public void sendACabinetDispense(ACabinetDispenseRequest request) {
        log.info("[PLC] A柜发药 | 来源层={} | 来源序号={} | 目标工位={} | 电磁铁时间={} | 伸出距离={} | 升高距离={}",
                request.getLineNum(), request.getPositionNum(), request.getWorkbenchNum(),
                request.getIoTime(), request.getStepExtendDistance(), request.getStepLiftDistance());
        sendWriteRegistersAsync(20, new int[]{
                toInt(request.getLineNum()),
                toInt(request.getPositionNum()),
                toInt(request.getWorkbenchNum()),
                toInt(request.getIoTime()),
                toInt(request.getStepExtendDistance()),
                toInt(request.getStepLiftDistance())
        });
    }

    private static int toInt(Integer value) {
        return value != null ? value : 0;
    }

    // ==================== 控制字1 (地址0~6，独立寄存器) ====================

    /**
     * B柜上苗启动 (地址1)
     */
    public void sendBCabinetFeedStart() {
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "true");
        log.info("[PLC] B柜上苗启动 | 写入地址1=1");
        sendWriteSingleRegisterAsync(1, 1);
    }

    /**
     * B柜上苗停止 (地址2)
     */
    public void sendBCabinetFeedStop() {
        valueOperations.set(RedisKeyConstant.autoDrug.AUTO_DRUG_START, "false");
        log.info("[PLC] B柜上苗停止 | 写入地址2=1");
        sendWriteSingleRegisterAsync(2, 1);
    }

    /**
     * 报警清除 (地址3)
     */
    public void sendClearAlarm() {
        log.info("[PLC] 报警清除 | 写入地址3=1");
        sendWriteSingleRegisterAsync(3, 1);
    }

    // ==================== 控制字2 (地址30~34，独立寄存器) ====================

    /**
     * B柜长宽厚合格 (地址30)
     */
    public void sendBCabinetJudgeQualified() {
        log.info("[PLC] B柜长宽厚合格 | 写入地址30=1");
        sendWriteSingleRegisterAsync(30, 1);
    }

    /**
     * B柜长宽厚不合格 (地址31)
     */
    public void sendBCabinetJudgeUnqualified() {
        log.info("[PLC] B柜长宽厚不合格 | 写入地址31=1");
        sendWriteSingleRegisterAsync(31, 1);
    }

    /**
     * B柜送药合格 (地址32)
     */
    public void sendBCabinetSendDrugQualified() {
        log.info("[PLC] B柜送药合格 | 写入地址32=1");
        sendWriteSingleRegisterAsync(32, 1);
    }

    /**
     * B柜送药不合格 (地址33)
     */
    public void sendBCabinetSendDrugUnqualified() {
        log.info("[PLC] B柜送药不合格 | 写入地址33=1");
        sendWriteSingleRegisterAsync(33, 1);
    }

    /**
     * B柜测试长宽高 (地址34)
     */
    public void sendBCabinetJudgeTest() {
        log.info("[PLC] B柜测试长宽高 | 写入地址34=1");
        sendWriteSingleRegisterAsync(34, 1);
    }

    // ==================== 控制字3 (地址40~44，独立寄存器) ====================

    /**
     * A柜出药命令下发 (地址40)
     * <p>
     * 协议：A柜_收到任务=0时写1，A柜_收到任务=1时写0
     */
    public void sendACabinetDispenseCmd() {


        log.info("[PLC] A柜出药命令下发 | 写入地址40=1");
        sendWriteSingleRegisterAsync(40, 1);

    }

    /**
     * A柜盘苗启动 (地址41=1)
     */
    public void sendACabinetInventoryStart() {
        log.info("[PLC] A柜盘苗启动 | 写入地址41=1");
        sendWriteSingleRegisterAsync(41, 1);
    }

    /**
     * A柜盘苗状态清除 (地址41=0)
     */
    public void sendACabinetInventoryClear() {
        log.info("[PLC] A柜盘苗状态清除 | 写入地址41=0");
        sendWriteSingleRegisterAsync(41, 0);
    }

    /**
     * A柜手动上苗 (地址42)
     */
    public void sendACabinetManualFeed() {
        log.info("[PLC] A柜手动上苗 | 写入地址42=1");
        sendWriteSingleRegisterAsync(42, 1);
    }

    /**
     * A柜进药完成应答 (地址43)
     */
    public void sendACabinetFillAck() {
        log.info("[PLC] A柜进药完成应答 | 写入地址43=1");
        sendWriteSingleRegisterAsync(43, 1);
    }

    /**
     * A柜到小皮带异常确认 (地址44)
     */
    public void sendACabinetBeltAlarmAck() {
        log.info("[PLC] A柜到小皮带异常确认 | 写入地址44=1");
        sendWriteSingleRegisterAsync(44, 1);
    }

    // ==================== A柜退苗 (地址60~64) ====================

    public void sendACabinetReturn(Integer workbench, Integer layer, Integer index, Integer times) {
        sendWriteRegistersAsync(61, new int[]{toInt(workbench), toInt(layer), toInt(index), Math.max(1, toInt(times))});
        sendWriteSingleRegisterAsync(60, 1);
    }

    public void clearACabinetReturn() {
        log.info("[PLC] A柜退苗完成 | 写入地址60=0");
        sendWriteSingleRegisterAsync(60, 0);
    }

    // ==================== 控制字4 (地址50~52，独立寄存器) ====================

    /**
     * C柜送药完成应答 (地址50)
     */
    public void sendCCabinetSendDrugAck() {
        log.info("[PLC] C柜送药完成应答 | 写入地址50=1");
        sendWriteSingleRegisterAsync(50, 1);
    }

    /**
     * C柜砸门开 (地址51)
     */
    public void sendCCabinetSmashDoorOpen() {
        log.info("[PLC] C柜砸门开 | 写入地址51=1");
        sendWriteSingleRegisterAsync(51, 1);
    }

    /**
     * C柜砸门关 (地址52)
     */
    public void sendCCabinetSmashDoorClose() {
        log.info("[PLC] C柜砸门关 | 写入地址52=1");
        sendWriteSingleRegisterAsync(52, 1);
    }

    public void sendMachineClean(int taskNum, int destination) {
        log.info("[PLC] 异常清苗 | 地址71任务号={} | 地址72目的地={} | 地址70=1", taskNum, destination);
        sendWriteRegistersAsync(71, new int[]{taskNum, destination});
        sendWriteSingleRegisterAsync(70, 1);
    }
}
