package com.yiwan.vaccinedispenser.system.dispensing.reservation;

/**
 * 逐盒预留结果。
 */
public enum AutoDrugReservationOutcome {
    RESERVED,
    ALREADY_RESERVED,
    CAPACITY_FULL,
    MACHINE_PRODUCT_CONFLICT,
    RESERVATION_ID_CONFLICT
}
