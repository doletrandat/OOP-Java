package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.model.CriterionType;

import java.util.Locale;

/** Nhận định của AI về một tiêu chí bổ sung của vị trí. */
public class CriterionVerdict {

    private String name = "";
    private String type = "";
    private String status = "UNKNOWN";   // MET | PARTIAL | NOT_MET | UNKNOWN
    private String actual = "";          // giá trị AI đọc/suy ra từ CV
    private String evidence = "";        // câu trích nguyên văn (nếu có)
    private String note = "";
    private boolean inferred;            // true = AI suy luận từ dữ kiện gián tiếp

    public CriterionVerdict() {
        // Gson cần constructor rỗng
    }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type == null ? "" : type; }
    public void setType(String type) { this.type = type; }

    public CriterionStatus toStatus() {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "MET", "PASS", "PASSED" -> CriterionStatus.MET;
            case "PARTIAL", "PARTIALLY_MET" -> CriterionStatus.PARTIAL;
            case "NOT_MET", "FAIL", "FAILED" -> CriterionStatus.NOT_MET;
            default -> CriterionStatus.UNKNOWN;
        };
    }

    public CriterionType toType() {
        return CriterionType.from(getType());
    }

    public String getActual() { return actual == null ? "" : actual; }
    public String getEvidence() { return evidence == null ? "" : evidence; }
    public String getNote() { return note == null ? "" : note; }

    public boolean isInferred() { return inferred; }
    public void setInferred(boolean inferred) { this.inferred = inferred; }
}
