package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话开始/结束结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugSessionResult {
    private AutoDrugSessionOutcome outcome;
    private String sessionId;
    private long residualCount;
    private String message;

    public boolean isUsable() {
        return outcome == AutoDrugSessionOutcome.STARTED
                || outcome == AutoDrugSessionOutcome.ALREADY_ACTIVE
                || outcome == AutoDrugSessionOutcome.REPLACED_ORPHAN_SESSION
                || outcome == AutoDrugSessionOutcome.CLEARED_STALE_RESERVATIONS;
    }
}
