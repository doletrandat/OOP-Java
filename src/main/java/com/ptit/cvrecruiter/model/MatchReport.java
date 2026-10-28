package com.ptit.cvrecruiter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Báo cáo chấm điểm một CV so với một JD.
 * Điểm quy tắc = kết hợp điểm kỹ năng và điểm tiêu chí bổ sung (nếu có).
 * Điểm cuối = (1 - aiWeight) * điểm quy tắc + aiWeight * điểm AI.
 */
public class MatchReport {

    private double keywordScore;      // 0..100
    private double criteriaScore = -1; // 0..100, -1 nếu job không có tiêu chí bổ sung
    private double aiScore = -1;      // 0..100, -1 nếu không có AI
    private double finalScore;        // 0..100
    private AiStatus aiStatus = AiStatus.NOT_CONFIGURED;
    private String aiError = "";      // thông báo lỗi nếu aiStatus = ERROR
    private String aiSummary = "";
    private List<String> aiHighlights = new ArrayList<>();
    private List<String> aiConcerns = new ArrayList<>();
    private List<SkillMatch> skillMatches = new ArrayList<>();
    private List<CriterionResult> criteriaResults = new ArrayList<>();
    private List<ScoreDimension> aiDimensions = new ArrayList<>();
    private String scoredAt = "";

    public MatchReport() {
        // Gson cần constructor rỗng
    }

    public double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(double keywordScore) { this.keywordScore = keywordScore; }

    public double getCriteriaScore() { return criteriaScore; }
    public void setCriteriaScore(double criteriaScore) { this.criteriaScore = criteriaScore; }

    public List<CriterionResult> getCriteriaResults() {
        return criteriaResults == null ? new ArrayList<>() : criteriaResults;
    }
    public void setCriteriaResults(List<CriterionResult> criteriaResults) {
        this.criteriaResults = criteriaResults;
    }

    /** Điểm từng nhóm tiêu chí do AI đánh giá (kỹ năng, kinh nghiệm, trình độ, ngoại ngữ...). */
    public List<ScoreDimension> getAiDimensions() {
        return aiDimensions == null ? new ArrayList<>() : aiDimensions;
    }
    public void setAiDimensions(List<ScoreDimension> aiDimensions) {
        this.aiDimensions = aiDimensions;
    }

    /** Số tiêu chí bổ sung đạt yêu cầu (chỉ tính trạng thái MET). */
    public long countCriteriaMet() {
        return getCriteriaResults().stream()
                .filter(c -> c.getStatus() == CriterionStatus.MET)
                .count();
    }

    public double getAiScore() { return aiScore; }
    public void setAiScore(double aiScore) { this.aiScore = aiScore; }

    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

    public AiStatus getAiStatus() { return aiStatus == null ? AiStatus.NOT_CONFIGURED : aiStatus; }
    public void setAiStatus(AiStatus aiStatus) { this.aiStatus = aiStatus; }

    public String getAiError() { return aiError == null ? "" : aiError; }
    public void setAiError(String aiError) { this.aiError = aiError; }

    public String getAiSummary() { return aiSummary == null ? "" : aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public List<String> getAiHighlights() { return aiHighlights == null ? new ArrayList<>() : aiHighlights; }
    public void setAiHighlights(List<String> aiHighlights) { this.aiHighlights = aiHighlights; }

    public List<String> getAiConcerns() { return aiConcerns == null ? new ArrayList<>() : aiConcerns; }
    public void setAiConcerns(List<String> aiConcerns) { this.aiConcerns = aiConcerns; }

    public List<SkillMatch> getSkillMatches() { return skillMatches == null ? new ArrayList<>() : skillMatches; }
    public void setSkillMatches(List<SkillMatch> skillMatches) { this.skillMatches = skillMatches; }

    public String getScoredAt() { return scoredAt == null ? "" : scoredAt; }
    public void setScoredAt(String scoredAt) { this.scoredAt = scoredAt; }

    public long countByStatus(MatchStatus status) {
        return getSkillMatches().stream().filter(s -> s.getStatus() == status).count();
    }
}
