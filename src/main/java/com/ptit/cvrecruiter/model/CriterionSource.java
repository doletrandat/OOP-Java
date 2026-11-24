package com.ptit.cvrecruiter.model;

/** Nguồn của kết luận một tiêu chí bổ sung. */
public enum CriterionSource {
    /** Chỉ quy tắc đối chiếu chữ trong CV. */
    RULE,
    /** AI bổ sung hoặc sửa lại kết luận của quy tắc. */
    AI,
    /** Quy tắc và AI cùng kết luận. */
    BOTH
}
