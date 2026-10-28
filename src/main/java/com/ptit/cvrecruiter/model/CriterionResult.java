package com.ptit.cvrecruiter.model;

/**
 * Kết quả đối chiếu một tiêu chí bổ sung với nội dung CV.
 * Giữ lại giá trị thực tế tìm thấy và câu trích dẫn để HR kiểm chứng.
 */
public class CriterionResult {

    private String name = "";
    private CriterionType type = CriterionType.OTHER;
    private String requirement = "";
    private int weight = 2;
    private CriterionStatus status = CriterionStatus.UNKNOWN;
    private String actual = "";      // giá trị hệ thống đọc được từ CV
    private String evidence = "";    // câu trích dẫn nguyên văn
    private String note = "";        // giải thích ngắn
    /** RULE = chỉ đối chiếu quy tắc; AI = AI bổ sung/suy luận; BOTH = cả hai cùng xác nhận. */
    private String source = "RULE";
    /** true nếu kết luận dựa trên suy luận gián tiếp (ví dụ làm việc ở Mỹ ⇒ biết tiếng Anh). */
    private boolean inferred;

    public CriterionResult() {
        // Gson cần constructor rỗng
    }

    public CriterionResult(Criterion criterion) {
        this.name = criterion.getName();
        this.type = criterion.getType();
        this.requirement = criterion.getRequirement();
        this.weight = criterion.getWeight();
    }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public CriterionType getType() { return type == null ? CriterionType.OTHER : type; }
    public void setType(CriterionType type) { this.type = type; }

    public String getRequirement() { return requirement == null ? "" : requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public int getWeight() { return weight < 1 || weight > 3 ? 2 : weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public CriterionStatus getStatus() { return status == null ? CriterionStatus.UNKNOWN : status; }
    public void setStatus(CriterionStatus status) { this.status = status; }

    public String getActual() { return actual == null ? "" : actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getEvidence() { return evidence == null ? "" : evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getNote() { return note == null ? "" : note; }
    public void setNote(String note) { this.note = note; }

    public String getSource() { return source == null ? "RULE" : source; }
    public void setSource(String source) { this.source = source; }

    public boolean isInferred() { return inferred; }
    public void setInferred(boolean inferred) { this.inferred = inferred; }

    /** Hệ số đóng góp của tiêu chí này vào điểm tiêu chí. */
    public double scoreValue() {
        return getStatus().value();
    }
}
