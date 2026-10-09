package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预留结果及该仓位预留后的pending数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugReservationResult {
    private AutoDrugReservationOutcome outcome;
    private AutoDrugReservation reservation;
    private long pendingCount;
    private String message;

    public boolean isReserved() {
        return outcome == AutoDrugReservationOutcome.RESERVED
                || outcome == AutoDrugReservationOutcome.ALREADY_RESERVED;
    }
}
