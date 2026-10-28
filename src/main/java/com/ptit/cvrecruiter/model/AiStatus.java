package com.ptit.cvrecruiter.model;

/** Trạng thái của tiêu chí chấm điểm bằng AI trong một lượt chấm. */
public enum AiStatus {
    OK,          // đã phân tích bằng Gemini
    NOT_CONFIGURED, // chưa có GEMINI_API_KEY
    ERROR        // có key nhưng gọi API lỗi
}
