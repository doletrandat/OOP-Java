package com.ptit.cvrecruiter.scoring;

import com.ptit.cvrecruiter.ai.AiAnalyzer;
import com.ptit.cvrecruiter.ai.AiAnalysis;
import com.ptit.cvrecruiter.ai.AiResult;
import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.criteria.CriteriaEvaluator;
import com.ptit.cvrecruiter.criteria.CriteriaMerger;
import com.ptit.cvrecruiter.matching.KeywordHit;
import com.ptit.cvrecruiter.matching.KeywordMatcher;
import com.ptit.cvrecruiter.model.AiStatus;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.EvidenceSource;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.MatchReport;
import com.ptit.cvrecruiter.model.MatchStatus;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillMatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dịch vụ chấm điểm tổng hợp, kết hợp ba nguồn đánh giá:
 * 1) Khớp kỹ năng theo từ khoá + từ điển đồng nghĩa (luôn chạy).
 * 2) Tiêu chí bổ sung do HR cấu hình: kinh nghiệm, trình độ, ngoại ngữ, chứng chỉ...
 * 3) Phân tích ngữ nghĩa bằng Gemini AI (nếu đã cấu hình API key).
 * AI chỉ đưa ra điểm số và nhận xét THAM KHẢO, không quyết định tuyển dụng.
 */
public class ScoringService {

    private static final double DEFAULT_CRITERIA_WEIGHT = 0.35;

    private final KeywordMatcher keywordMatcher;
    private final AiAnalyzer aiAnalyzer;
    private final CriteriaEvaluator criteriaEvaluator;
    private final CriteriaMerger criteriaMerger;
    private final double aiWeight;
    private final double criteriaWeight;

    public ScoringService(KeywordMatcher keywordMatcher, AiAnalyzer aiAnalyzer, AppConfig config) {
        this(keywordMatcher, aiAnalyzer, config.aiWeight, config.criteriaWeight);
    }

    /** Cho phép truyền trực tiếp trọng số AI (thuận tiện cho kiểm thử đơn vị). */
    public ScoringService(KeywordMatcher keywordMatcher, AiAnalyzer aiAnalyzer, double aiWeight) {
        this(keywordMatcher, aiAnalyzer, aiWeight, DEFAULT_CRITERIA_WEIGHT);
    }

    public ScoringService(KeywordMatcher keywordMatcher, AiAnalyzer aiAnalyzer,
                          double aiWeight, double criteriaWeight) {
        this.keywordMatcher = keywordMatcher;
        this.aiAnalyzer = aiAnalyzer;
        this.criteriaEvaluator = new CriteriaEvaluator();
        this.criteriaMerger = new CriteriaMerger();
        this.aiWeight = Math.max(0, Math.min(1, aiWeight));
        this.criteriaWeight = Math.max(0, Math.min(1, criteriaWeight));
    }

    public MatchReport score(JobPosting job, String cvText) {
        Map<String, KeywordHit> hits = keywordMatcher.match(job, cvText);
        AiResult aiResult = aiAnalyzer.analyze(job, cvText);

        double keywordScore = computeKeywordScore(job.getSkills(), hits);

        // Đối chiếu tiêu chí bằng quy tắc trước, sau đó để AI bổ sung
        // những tiêu chí mà quy tắc không tìm thấy bằng chứng (ví dụ suy ra
        // ngoại ngữ từ môi trường làm việc).
        List<CriterionResult> criteriaResults = criteriaEvaluator.evaluate(job.getCriteria(), cvText);
        if (aiResult.isSuccess()) {
            criteriaMerger.merge(criteriaResults, aiResult.getAnalysis().getCriteria());
        }
        double criteriaScore = computeCriteriaScore(criteriaResults);

        MatchReport report = new MatchReport();
        report.setKeywordScore(round1(keywordScore));
        report.setCriteriaScore(criteriaScore < 0 ? -1 : round1(criteriaScore));
        report.setCriteriaResults(criteriaResults);
        report.setScoredAt(Instant.now().toString());

        // Điểm quy tắc: chỉ trộn tiêu chí bổ sung khi vị trí thực sự có tiêu chí
        double ruleScore = criteriaScore < 0
                ? keywordScore
                : keywordScore * (1 - criteriaWeight) + criteriaScore * criteriaWeight;

        if (aiResult.isSuccess()) {
            AiAnalysis ai = aiResult.getAnalysis();
            double aiScore = clamp(ai.getAiScore());
            report.setAiStatus(AiStatus.OK);
            report.setAiScore(round1(aiScore));
            report.setAiSummary(ai.getSummary());
            report.setAiHighlights(ai.getHighlights());
            report.setAiConcerns(ai.getConcerns());
            report.setAiDimensions(ai.getDimensions());
            report.setSkillMatches(merge(job.getSkills(), hits, ai));
            report.setFinalScore(round1(clamp(ruleScore * (1 - aiWeight) + aiScore * aiWeight)));
        } else {
            report.setAiStatus(aiResult.getStatus());
            report.setAiError(aiResult.getError());
            report.setAiScore(-1);
            report.setSkillMatches(merge(job.getSkills(), hits, null));
            report.setFinalScore(round1(clamp(ruleScore)));
        }
        return report;
    }

    /** Điểm tiêu chí: tổng trọng số đạt / tổng trọng số, nhân 100. -1 nếu không có tiêu chí. */
    private double computeCriteriaScore(List<CriterionResult> results) {
        if (results == null || results.isEmpty()) return -1;
        double total = 0;
        double got = 0;
        for (CriterionResult r : results) {
            total += r.getWeight();
            got += r.getWeight() * r.scoreValue();
        }
        return total == 0 ? 0 : got / total * 100;
    }

    /** Điểm từ khoá: tổng trọng số các kỹ năng tìm thấy / tổng trọng số, nhân 100. */
    private double computeKeywordScore(List<Skill> skills, Map<String, KeywordHit> hits) {
        double total = 0;
        double got = 0;
        for (Skill s : skills) {
            double w = s.getWeight().getScore();
            total += w;
            if (hits.containsKey(s.getName())) got += w;
        }
        return total == 0 ? 0 : got / total * 100;
    }

    /**
     * Ghép kết quả hai tiêu chí cho từng kỹ năng, ưu tiên bằng chứng
     * trích dẫn trực tiếp từ CV (của bộ khớp từ khoá).
     */
    private List<SkillMatch> merge(List<Skill> skills, Map<String, KeywordHit> hits, AiAnalysis ai) {
        List<SkillMatch> result = new ArrayList<>();
        for (Skill skill : skills) {
            SkillMatch sm = new SkillMatch(skill.getName(), skill.getWeight());
            KeywordHit hit = hits.get(skill.getName());

            if (hit != null) {
                sm.setEvidence(hit.evidence());
                sm.setOccurrences(hit.occurrences());
                sm.setStatus(MatchStatus.MATCHED);
                sm.setSource(EvidenceSource.KEYWORD);
            }

            if (ai != null) {
                ai.getSkills().stream()
                        .filter(v -> sameSkill(v.getSkill(), skill.getName()))
                        .findFirst()
                        .ifPresent(v -> {
                            sm.setAiNote(v.getNote());
                            if (sm.getEvidence().isEmpty() && !v.getEvidence().isEmpty()) {
                                sm.setEvidence(v.getEvidence());
                            }
                            switch (v.toMatchStatus()) {
                                case MATCHED -> {
                                    if (hit != null) {
                                        sm.setSource(EvidenceSource.BOTH);
                                    } else {
                                        sm.setStatus(MatchStatus.MATCHED);
                                        sm.setSource(EvidenceSource.AI);
                                    }
                                }
                                case UNCERTAIN -> {
                                    if (hit == null) {
                                        sm.setStatus(MatchStatus.UNCERTAIN);
                                        sm.setSource(EvidenceSource.AI);
                                    }
                                }
                                default -> {
                                    if (hit == null) sm.setSource(EvidenceSource.NONE);
                                }
                            }
                        });
            }
            result.add(sm);
        }
        // Kỹ năng bắt buộc hiển thị trước
        result.sort((a, b) -> Integer.compare(b.getWeight().getScore(), a.getWeight().getScore()));
        return result;
    }

    /** So tên kỹ năng không phân biệt dấu/hoa thường ("React" == "react"). */
    private boolean sameSkill(String a, String b) {
        return com.ptit.cvrecruiter.util.TextNormalizer.normalize(a)
                .equals(com.ptit.cvrecruiter.util.TextNormalizer.normalize(b));
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /** Chặn điểm ngoài khoảng 0..100 (AI có thể trả về số bất thường). */
    private double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
