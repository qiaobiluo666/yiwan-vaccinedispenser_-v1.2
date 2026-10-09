package com.yiwan.vaccinedispenser.system.dispensing.reservation;

/**
 * Java自动上药Redis会话操作结果。
 */
public enum AutoDrugSessionOutcome {
    STARTED,
    ALREADY_ACTIVE,
    REPLACED_ORPHAN_SESSION,
    CLEARED_STALE_RESERVATIONS,
    FINISHED,
    ALREADY_FINISHED
}
