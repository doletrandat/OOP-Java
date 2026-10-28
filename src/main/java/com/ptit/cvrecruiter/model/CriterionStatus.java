package com.ptit.cvrecruiter.model;

/** Mức đạt của một tiêu chí đánh giá bổ sung. */
public enum CriterionStatus {
    MET,        // Đáp ứng đủ
    PARTIAL,    // Đáp ứng một phần
    NOT_MET,    // Không đáp ứng
    UNKNOWN;    // CV không nêu thông tin để kết luận

    /** Hệ số quy đổi sang điểm 0..1. */
    public double value() {
        return switch (this) {
            case MET -> 1.0;
            case PARTIAL -> 0.5;
            default -> 0.0;
        };
    }

    public String label() {
        return switch (this) {
            case MET -> "Đạt";
            case PARTIAL -> "Một phần";
            case NOT_MET -> "Chưa đạt";
            case UNKNOWN -> "Chưa rõ";
        };
    }
}
