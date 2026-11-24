package cvrecruiter.web.dto;

import cvrecruiter.model.AiStatus;
import cvrecruiter.model.Application;
import cvrecruiter.model.HrDecision;
import cvrecruiter.model.MatchReport;
import cvrecruiter.model.MatchStatus;

/**
 * Một dòng trong bảng ứng viên. Các trường điểm là null (không xuất hiện trong JSON)
 * khi hồ sơ chưa được chấm.
 */
public record ApplicationSummaryView(
        String id, String candidateName, String email, String phone, String submittedAt,
        HrDecision hrDecision, String hrNote, String decidedAt,
        Double finalScore, Double keywordScore, Double aiScore, AiStatus aiStatus,
        Long matchedCount, Integer totalSkills,
        Double criteriaScore, Long criteriaMet, Integer criteriaTotal) {

    public static ApplicationSummaryView of(Application app) {
        MatchReport r = app.getReport();
        boolean scored = r != null;
        return new ApplicationSummaryView(
                app.getId(), app.getCandidateName(), app.getEmail(), app.getPhone(), app.getSubmittedAt(),
                app.getHrDecision(), app.getHrNote(), app.getDecidedAt(),
                scored ? r.getFinalScore() : null,
                scored ? r.getKeywordScore() : null,
                scored ? r.getAiScore() : null,
                scored ? r.getAiStatus() : null,
                scored ? r.countByStatus(MatchStatus.MATCHED) : null,
                scored ? r.getSkillMatches().size() : null,
                scored ? r.getCriteriaScore() : null,
                scored ? r.countCriteriaMet() : null,
                scored ? r.getCriteriaResults().size() : null);
    }
}
