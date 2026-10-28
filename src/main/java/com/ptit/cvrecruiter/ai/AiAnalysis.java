package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.ScoreDimension;

import java.util.ArrayList;
import java.util.List;

/** Kết quả phân tích chi tiết của AI cho một CV so với một JD. */
public class AiAnalysis {

    private double aiScore;
    private String summary = "";
    private List<String> highlights = new ArrayList<>();
    private List<String> concerns = new ArrayList<>();
    private List<SkillVerdict> skills = new ArrayList<>();
    private List<CriterionVerdict> criteria = new ArrayList<>();
    private List<ScoreDimension> dimensions = new ArrayList<>();

    public AiAnalysis() {
        // Gson cần constructor rỗng
    }

    public double getAiScore() {
        return aiScore;
    }

    public void setAiScore(double aiScore) {
        this.aiScore = Math.max(0, Math.min(100, aiScore));
    }

    public String getSummary() {
        return summary == null ? "" : summary;
    }

    public List<String> getHighlights() {
        return highlights == null ? new ArrayList<>() : highlights;
    }

    public List<String> getConcerns() {
        return concerns == null ? new ArrayList<>() : concerns;
    }

    public List<SkillVerdict> getSkills() {
        return skills == null ? new ArrayList<>() : skills;
    }

    public List<CriterionVerdict> getCriteria() {
        return criteria == null ? new ArrayList<>() : criteria;
    }

    public List<ScoreDimension> getDimensions() {
        return dimensions == null ? new ArrayList<>() : dimensions;
    }
}
