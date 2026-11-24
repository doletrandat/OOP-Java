package cvrecruiter.model;

/**
 * Điểm của một nhóm tiêu chí do AI đánh giá (kỹ năng, kinh nghiệm, trình độ,
 * ngoại ngữ, chứng chỉ) kèm nhận xét ngắn. Bất biến.
 */
public class ScoreDimension {

    private String name = "";
    private double score;     // 0..100
    private String comment = "";

    private ScoreDimension() {
        // Gson
    }

    public ScoreDimension(String name, double score, String comment) {
        this.name = name;
        this.score = Math.max(0, Math.min(100, score));
        this.comment = comment;
    }

    public String getName() { return name == null ? "" : name; }

    public double getScore() { return Math.max(0, Math.min(100, score)); }

    public String getComment() { return comment == null ? "" : comment; }
}
