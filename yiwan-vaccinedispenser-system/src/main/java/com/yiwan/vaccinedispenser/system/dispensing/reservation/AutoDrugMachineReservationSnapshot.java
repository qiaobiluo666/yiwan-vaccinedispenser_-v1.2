package com.yiwan.vaccinedispenser.system.dispensing.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 某仓位当前所有未结预留的聚合快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDrugMachineReservationSnapshot {
    private Long machineId;
    private String boxNo;
    private String productNo;
    private String productName;
    private String batchNo;
    private Long expiredAtEpochMillis;
    private long pendingCount;
    private int maxCapacity;
    private List<String> reservationIds;
    private Set<AutoDrugReservationState> states;

    public long effectiveCount(int databaseCount) {
        if (databaseCount < 0) {
            throw new IllegalArgumentException("databaseCount不能小于0");
        }
        return databaseCount + pendingCount;
    }

    public boolean hasCapacity(int databaseCount) {
        return effectiveCount(databaseCount) < maxCapacity;
    }
}
