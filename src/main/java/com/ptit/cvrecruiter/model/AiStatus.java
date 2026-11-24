package com.ptit.cvrecruiter.model;

/** Trạng thái của tiêu chí chấm điểm bằng AI trong một lượt chấm. */
public enum AiStatus {
    OK,             // AI đã phân tích xong
    NOT_CONFIGURED, // chưa có AI_API_KEY
    ERROR           // có khoá nhưng gọi API lỗi
}
