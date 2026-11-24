package cvrecruiter.scoring;

import cvrecruiter.ai.AiAnalysis;
import cvrecruiter.ai.AiAnalyzer;
import cvrecruiter.ai.AiResult;
import cvrecruiter.ai.SkillVerdict;
import cvrecruiter.config.ScoringWeights;
import cvrecruiter.criteria.CriteriaEvaluator;
import cvrecruiter.criteria.CriteriaMerger;
import cvrecruiter.matching.KeywordHit;
import cvrecruiter.matching.KeywordMatcher;
import cvrecruiter.model.CriterionResult;
import cvrecruiter.model.JobPosting;
import cvrecruiter.model.MatchReport;
import cvrecruiter.model.Skill;
import cvrecruiter.model.SkillMatch;
import cvrecruiter.util.TextNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ chấm điểm tổng hợp, kết hợp ba nguồn đánh giá:
 * 1) Khớp kỹ năng theo từ khoá + từ điển đồng nghĩa (luôn chạy).
 * 2) Tiêu chí bổ sung do HR cấu hình: kinh nghiệm, trình độ, ngoại ngữ, chứng chỉ...
 * 3) Phân tích ngữ nghĩa bằng AI (nếu đã cấu hình).
 * AI chỉ đưa ra điểm số và nhận xét THAM KHẢO, không quyết định tuyển dụng.
 */
public class ScoringService {

    private final KeywordMatcher keywordMatcher;
    private final AiAnalyzer aiAnalyzer;
    private final ScoringWeights weights;
    private final CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator();
    private final CriteriaMerger criteriaMerger = new CriteriaMerger();

    public ScoringService(KeywordMatcher keywordMatcher, AiAnalyzer aiAnalyzer, ScoringWeights weights) {
        this.keywordMatcher = keywordMatcher;
        this.aiAnalyzer = aiAnalyzer;
        this.weights = weights;
    }

    /** Chỉ đổi trọng số AI, giữ trọng số tiêu chí mặc định (thuận tiện cho kiểm thử). */
    public ScoringService(KeywordMatcher keywordMatcher, AiAnalyzer aiAnalyzer, double aiWeight) {
        this(keywordMatcher, aiAnalyzer, new ScoringWeights(aiWeight, ScoringWeights.DEFAULT_CRITERIA));
    }

    public MatchReport score(JobPosting job, String cvText) {
        Map<String, KeywordHit> hits = keywordMatcher.match(job, cvText);
        AiResult aiResult = aiAnalyzer.analyze(job, cvText);
        AiAnalysis ai = aiResult.isSuccess() ? aiResult.getAnalysis() : null;

        // Kỹ năng: ghép từ khoá với AI. Khớp = đủ điểm, chưa chắc = nửa.
        List<SkillMatch> skillMatches = matchSkills(job.getSkills(), hits, ai);
        double skillScore = skillScore(skillMatches);

        // Tiêu chí: quy tắc chạy trước, AI bổ sung phần quy tắc không đọc được ngữ cảnh.
        List<CriterionResult> criteriaResults = new ArrayList<>(criteriaEvaluator.evaluate(job.getCriteria(), cvText));
        if (ai != null) criteriaMerger.merge(criteriaResults, ai.getCriteria());
        double criteriaScore = criteriaScore(criteriaResults);

        // Điểm quy tắc: chỉ trộn tiêu chí bổ sung khi vị trí thực sự có tiêu chí
        double ruleScore = criteriaScore < 0
                ? skillScore
                : skillScore * (1 - weights.criteria()) + criteriaScore * weights.criteria();

        MatchReport.Builder report = MatchReport.builder()
                .skills(skillMatches, skillScore)
                .criteria(criteriaResults, criteriaScore);
        if (ai == null) {
            return report.aiUnavailable(aiResult.getStatus(), aiResult.getError())
                    .finalScore(ruleScore)
                    .build();
        }
        double aiScore = clamp(ai.getAiScore());
        return report.ai(aiScore, ai.getSummary(), ai.getHighlights(), ai.getConcerns(), ai.getDimensions())
                .finalScore(ruleScore * weights.rules() + aiScore * weights.ai())
                .build();
    }

    /** Ghép kết quả từ khoá và AI cho từng kỹ năng; kỹ năng bắt buộc đứng trước. */
    private List<SkillMatch> matchSkills(List<Skill> skills, Map<String, KeywordHit> hits, AiAnalysis ai) {
        List<SkillMatch> result = new ArrayList<>();
        for (Skill skill : skills) {
            SkillMatch match = new SkillMatch(skill);
            KeywordHit hit = hits.get(skill.getName());
            if (hit != null) match.foundByKeyword(hit.evidence(), hit.occurrences());
            if (ai != null) {
                ai.getSkills().stream()
                        .filter(v -> sameSkill(v.getSkill(), skill.getName()))
                        .findFirst()
                        .ifPresent(v -> applyVerdict(match, v));
            }
            result.add(match);
        }
        result.sort(Comparator.comparingInt((SkillMatch m) -> m.getWeight().getScore()).reversed());
        return result;
    }

    private static void applyVerdict(SkillMatch match, SkillVerdict verdict) {
        match.applyAiVerdict(verdict.toMatchStatus(), verdict.getEvidence(), verdict.getNote());
    }

    /** Tổng trọng số đạt / tổng trọng số, nhân 100. */
    private static double skillScore(List<SkillMatch> matches) {
        double total = matches.stream().mapToDouble(m -> m.getWeight().getScore()).sum();
        double earned = matches.stream().mapToDouble(SkillMatch::earnedWeight).sum();
        return total == 0 ? 0 : earned / total * 100;
    }

    /** Như điểm kỹ năng nhưng theo trọng số tiêu chí. -1 nếu vị trí không có tiêu chí. */
    private static double criteriaScore(List<CriterionResult> results) {
        if (results.isEmpty()) return MatchReport.NOT_AVAILABLE;
        double total = results.stream().mapToDouble(CriterionResult::getWeight).sum();
        double earned = results.stream().mapToDouble(r -> r.getWeight() * r.scoreValue()).sum();
        return total == 0 ? 0 : earned / total * 100;
    }

    /** So tên kỹ năng không phân biệt dấu/hoa thường ("React" == "react"). */
    private static boolean sameSkill(String a, String b) {
        return TextNormalizer.normalize(a).equals(TextNormalizer.normalize(b));
    }

    /** AI có thể trả về số ngoài 0..100. */
    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
