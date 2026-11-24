package cvrecruiter.model;

/**
 * Kết quả đối chiếu một tiêu chí bổ sung với nội dung CV.
 * Giữ lại giá trị thực tế tìm thấy và câu trích dẫn để HR kiểm chứng.
 */
public class CriterionResult {

    private String name = "";
    private CriterionType type = CriterionType.OTHER;
    private String requirement = "";
    private int weight = Criterion.DEFAULT_WEIGHT;
    private CriterionStatus status = CriterionStatus.UNKNOWN;
    private String actual = "";      // giá trị hệ thống đọc được từ CV
    private String evidence = "";    // câu trích dẫn nguyên văn
    private String note = "";        // giải thích ngắn
    private CriterionSource source = CriterionSource.RULE;
    /** true nếu kết luận dựa trên suy luận gián tiếp (ví dụ làm việc ở Mỹ nên biết tiếng Anh). */
    private boolean inferred;

    private CriterionResult() {
        // Gson
    }

    public CriterionResult(Criterion criterion) {
        this.name = criterion.getName();
        this.type = criterion.getType();
        this.requirement = criterion.getRequirement();
        this.weight = criterion.getWeight();
    }

    /** Quy tắc kết luận tiêu chí. Trả về chính nó để viết gọn trong các quy tắc. */
    public CriterionResult conclude(CriterionStatus status, String actual, String evidence, String note) {
        this.status = status;
        this.actual = actual == null ? "" : actual;
        this.evidence = evidence == null ? "" : evidence;
        this.note = note == null ? "" : note;
        this.source = CriterionSource.RULE;
        return this;
    }

    /** Không đủ dữ kiện để kết luận. */
    public CriterionResult unknown(String note) {
        return conclude(CriterionStatus.UNKNOWN, "", "", note);
    }

    /** AI đồng ý với kết luận của quy tắc. */
    public void confirmByAi(String aiNote) {
        this.source = CriterionSource.BOTH;
        if (getNote().isBlank() && aiNote != null) this.note = aiNote;
    }

    /** AI thay kết luận của quy tắc (nâng lên hoặc hạ xuống), có ghi chú giải thích. */
    public void overrideByAi(CriterionStatus status, String actual, String evidence, String note, boolean inferred) {
        this.status = status;
        if (actual != null && !actual.isBlank()) this.actual = actual;
        if (evidence != null && !evidence.isBlank()) this.evidence = evidence;
        this.note = note == null ? "" : note;
        this.source = CriterionSource.AI;
        this.inferred = inferred;
    }

    /** Bổ sung ghi chú mà không đổi kết luận. */
    public void annotate(String note) {
        this.note = note == null ? "" : note;
    }

    /** Hệ số đóng góp vào điểm tiêu chí (0..1). */
    public double scoreValue() {
        return getStatus().value();
    }

    public String getName() { return name == null ? "" : name; }

    public CriterionType getType() { return type == null ? CriterionType.OTHER : type; }

    public String getRequirement() { return requirement == null ? "" : requirement; }

    public int getWeight() { return weight < Criterion.MIN_WEIGHT || weight > Criterion.MAX_WEIGHT ? Criterion.DEFAULT_WEIGHT : weight; }

    public CriterionStatus getStatus() { return status == null ? CriterionStatus.UNKNOWN : status; }

    public String getActual() { return actual == null ? "" : actual; }

    public String getEvidence() { return evidence == null ? "" : evidence; }

    public String getNote() { return note == null ? "" : note; }

    public CriterionSource getSource() { return source == null ? CriterionSource.RULE : source; }

    public boolean isInferred() { return inferred; }
}
