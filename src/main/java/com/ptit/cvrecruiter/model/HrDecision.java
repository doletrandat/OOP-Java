package com.ptit.cvrecruiter.model;

/** Quyết định của HR sau khi xem điểm — AI không bao giờ tự đặt giá trị này. */
public enum HrDecision {
    NEW,          // Chưa xử lý
    SHORTLISTED,  // Mời phỏng vấn / vào danh sách ngắn
    REJECTED;     // Từ chối

    public static HrDecision from(String raw) {
        if (raw == null) return NEW;
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NEW;
        }
    }
}
