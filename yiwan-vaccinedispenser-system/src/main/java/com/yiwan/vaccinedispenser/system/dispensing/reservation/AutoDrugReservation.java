package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个药盒的Redis预留记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugReservation {

    /** 每盒唯一ID；同一次调用重试时必须复用该ID。 */
    private String reservationId;

    /** 一次自动上药运行的会话ID，用于重启后识别残留。 */
    private String sessionId;

    private Long machineId;
    private String boxNo;
    private String productNo;
    private String productName;
    private String batchNo;

    /** 有效期毫秒时间戳；null与空值按同一种身份处理。 */
    private Long expiredAtEpochMillis;

    private String supervisedCode;

    /** 本盒预留时读取到的数据库真实库存，只用于诊断。 */
    private Integer databaseCountAtReserve;

    /** 此仓位本次采用的安全最大容量。 */
    private Integer maxCapacity;

    private AutoDrugReservationState state;
    private String stateReason;
    private Long createdAtEpochMillis;
    private Long updatedAtEpochMillis;
}
