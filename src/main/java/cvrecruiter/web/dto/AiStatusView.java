package cvrecruiter.web.dto;

import cvrecruiter.config.AiSettings;
import cvrecruiter.config.ScoringWeights;

/** Trạng thái AI và trọng số chấm điểm hiển thị trên giao diện. Không bao giờ chứa API key. */
public record AiStatusView(boolean configured, String displayName, String model,
                           double aiWeight, double criteriaWeight, double keywordWeight) {

    public static AiStatusView of(AiSettings ai, ScoringWeights weights) {
        return new AiStatusView(ai.isConfigured(), ai.displayName(), ai.model(),
                weights.ai(), weights.criteria(), Math.round(weights.rules() * 100) / 100.0);
    }
}
