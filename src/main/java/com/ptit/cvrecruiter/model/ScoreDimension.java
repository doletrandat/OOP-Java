package com.ptit.cvrecruiter.model;

/**
 * Điểm của một nhóm tiêu chí do AI đánh giá (kỹ năng, kinh nghiệm, trình độ,
 * ngoại ngữ, chứng chỉ, yếu tố khác) kèm nhận xét ngắn.
 */
public class ScoreDimension {

    private String name = "";
    private double score;     // 0..100
    private String comment = "";

    public ScoreDimension() {
        // Gson cần constructor rỗng
    }

    public ScoreDimension(String name, double score, String comment) {
        this.name = name;
        setScore(score);
        this.comment = comment;
    }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = Math.max(0, Math.min(100, score)); }

    public String getComment() { return comment == null ? "" : comment; }
    public void setComment(String comment) { this.comment = comment; }
}
