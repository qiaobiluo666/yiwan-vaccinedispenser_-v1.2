package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 幂等释放结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugReleaseResult {
    private AutoDrugReleaseOutcome outcome;
    private String reservationId;
    private Long machineId;
    private long pendingCount;
    private String message;
}
