package com.yiwan.vaccinedispenser.system.dispensing.reservation;

/**
 * Java自动上药逐盒预留状态。
 *
 * <p>UNCERTAIN和COMMIT_FAILED都是安全阻断状态，仍计入仓位pending数量，
 * 只能在人工确认或可靠对账后显式解除。</p>
 */
public enum AutoDrugReservationState {

    /** 已选择仓位，药盒尚未确认进入A柜机械手。 */
    RESERVED,

    /** 药盒已经进入A柜机械手或正在送往仓位。 */
    IN_TRANSIT,

    /** 药盒物理位置无法确认，必须保留占位并报警。 */
    UNCERTAIN,

    /** 药盒已确认进入仓位，但数据库记账失败，必须保留占位并报警。 */
    COMMIT_FAILED
}
