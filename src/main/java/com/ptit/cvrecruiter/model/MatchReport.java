package com.ptit.cvrecruiter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Báo cáo chấm điểm một CV so với một vị trí. Được tạo một lần qua {@link Builder}
 * rồi không đổi nữa (chấm lại thì tạo báo cáo mới).
 *
 * <p>Điểm quy tắc = ghép điểm kỹ năng với điểm tiêu chí bổ sung (nếu có).
 * Điểm tổng = (1 - trọng số AI) × điểm quy tắc + trọng số AI × điểm AI.
 */
public class MatchReport {

    public static final double NOT_AVAILABLE = -1;

    private double keywordScore;                 // 0..100, điểm kỹ năng
    private double criteriaScore = NOT_AVAILABLE; // 0..100, -1 nếu vị trí không có tiêu chí
    private double aiScore = NOT_AVAILABLE;       // 0..100, -1 nếu không có AI
    private double finalScore;                    // 0..100
    private AiStatus aiStatus = AiStatus.NOT_CONFIGURED;
    private String aiError = "";
    private String aiSummary = "";
    private List<String> aiHighlights = new ArrayList<>();
    private List<String> aiConcerns = new ArrayList<>();
    private List<SkillMatch> skillMatches = new ArrayList<>();
    private List<CriterionResult> criteriaResults = new ArrayList<>();
    private List<ScoreDimension> aiDimensions = new ArrayList<>();
    private String scoredAt = "";

    private MatchReport() {
        // Gson và Builder
    }

    public static Builder builder() {
        return new Builder();
    }

    public long countByStatus(MatchStatus status) {
        return getSkillMatches().stream().filter(s -> s.getStatus() == status).count();
    }

    /** Số tiêu chí bổ sung đạt yêu cầu (chỉ tính MET). */
    public long countCriteriaMet() {
        return getCriteriaResults().stream().filter(c -> c.getStatus() == CriterionStatus.MET).count();
    }

    public double getKeywordScore() { return keywordScore; }

    public double getCriteriaScore() { return criteriaScore; }

    public double getAiScore() { return aiScore; }

    public double getFinalScore() { return finalScore; }

    public AiStatus getAiStatus() { return aiStatus == null ? AiStatus.NOT_CONFIGURED : aiStatus; }

    public String getAiError() { return aiError == null ? "" : aiError; }

    public String getAiSummary() { return aiSummary == null ? "" : aiSummary; }

    public List<String> getAiHighlights() { return aiHighlights == null ? List.of() : List.copyOf(aiHighlights); }

    public List<String> getAiConcerns() { return aiConcerns == null ? List.of() : List.copyOf(aiConcerns); }

    public List<SkillMatch> getSkillMatches() { return skillMatches == null ? List.of() : List.copyOf(skillMatches); }

    public List<CriterionResult> getCriteriaResults() {
        return criteriaResults == null ? List.of() : List.copyOf(criteriaResults);
    }

    /** Điểm từng nhóm tiêu chí do AI đánh giá. */
    public List<ScoreDimension> getAiDimensions() { return aiDimensions == null ? List.of() : List.copyOf(aiDimensions); }

    public String getScoredAt() { return scoredAt == null ? "" : scoredAt; }

    /** Dựng báo cáo từng phần; điểm được làm tròn một chữ số thập phân và giới hạn trong 0..100. */
    public static final class Builder {

        private final MatchReport report = new MatchReport();

        private Builder() {
            report.scoredAt = Instant.now().toString();
        }

        public Builder skills(List<SkillMatch> matches, double score) {
            report.skillMatches = new ArrayList<>(matches);
            report.keywordScore = round(score);
            return this;
        }

        public Builder criteria(List<CriterionResult> results, double score) {
            report.criteriaResults = new ArrayList<>(results);
            report.criteriaScore = score < 0 ? NOT_AVAILABLE : round(score);
            return this;
        }

        public Builder ai(double score, String summary, List<String> highlights, List<String> concerns,
                          List<ScoreDimension> dimensions) {
            report.aiStatus = AiStatus.OK;
            report.aiScore = round(score);
            report.aiSummary = summary;
            report.aiHighlights = new ArrayList<>(highlights);
            report.aiConcerns = new ArrayList<>(concerns);
            report.aiDimensions = new ArrayList<>(dimensions);
            return this;
        }

        public Builder aiUnavailable(AiStatus status, String error) {
            report.aiStatus = status;
            report.aiError = error == null ? "" : error;
            report.aiScore = NOT_AVAILABLE;
            return this;
        }

        public Builder finalScore(double score) {
            report.finalScore = round(score);
            return this;
        }

        public MatchReport build() {
            return report;
        }

        private static double round(double v) {
            return Math.round(Math.max(0, Math.min(100, v)) * 10.0) / 10.0;
        }
    }
}
