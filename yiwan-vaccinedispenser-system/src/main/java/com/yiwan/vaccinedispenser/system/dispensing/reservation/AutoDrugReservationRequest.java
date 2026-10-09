package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 逐盒预留请求。数据库真实库存和最大容量均由选仓调用方在同一流程中提供。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugReservationRequest {

    /** 可选；为空时自动生成UUID。重试若要幂等，调用方应复用首次返回的ID。 */
    private String reservationId;
    private String sessionId;
    private Long machineId;
    private String boxNo;
    private String productNo;
    private String productName;
    private String batchNo;
    private Date expiredAt;
    private String supervisedCode;
    private Integer databaseCount;
    private Integer maxCapacity;
}
