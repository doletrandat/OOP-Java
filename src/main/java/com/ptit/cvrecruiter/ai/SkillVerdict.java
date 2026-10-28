package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.MatchStatus;

/** Nhận định của AI về một kỹ năng trong CV. */
public class SkillVerdict {

    private String skill;
    private String status;     // MATCHED | UNCERTAIN | MISSING (chuỗi trả về từ Gemini)
    private String evidence;   // câu trích dẫn nguyên văn từ CV
    private String note;       // giải thích ngắn

    public SkillVerdict() {
        // Gson cần constructor rỗng
    }

    public String getSkill() {
        return skill == null ? "" : skill;
    }

    public String getStatus() {
        return status == null ? "MISSING" : status.trim().toUpperCase();
    }

    public String getEvidence() {
        return evidence == null ? "" : evidence;
    }

    public String getNote() {
        return note == null ? "" : note;
    }

    public MatchStatus toMatchStatus() {
        return switch (getStatus()) {
            case "MATCHED" -> MatchStatus.MATCHED;
            case "UNCERTAIN" -> MatchStatus.UNCERTAIN;
            default -> MatchStatus.MISSING;
        };
    }
}
