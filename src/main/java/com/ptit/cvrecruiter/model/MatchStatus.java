package com.ptit.cvrecruiter.model;

/** Kết quả so khớp một kỹ năng giữa JD và CV. */
public enum MatchStatus {
    MATCHED,    // tìm thấy bằng chứng trong CV
    UNCERTAIN,  // AI nghi ngờ / bằng chứng gián tiếp
    MISSING     // không thấy bằng chứng
}
