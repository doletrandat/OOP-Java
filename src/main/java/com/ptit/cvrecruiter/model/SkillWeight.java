package com.ptit.cvrecruiter.model;

/**
 * Mức độ quan trọng của kỹ năng trong JD.
 * REQUIRED được tính trọng số gấp đôi PREFERRED khi chấm điểm từ khoá.
 */
public enum SkillWeight {
    REQUIRED(2),
    PREFERRED(1);

    private final int score;

    SkillWeight(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public static SkillWeight from(String value) {
        if (value == null) return PREFERRED;
        try {
            return SkillWeight.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "bắt buộc".equalsIgnoreCase(value.trim()) || "bat buoc".equalsIgnoreCase(value.trim())
                    ? REQUIRED : PREFERRED;
        }
    }
}
