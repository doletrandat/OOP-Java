package com.ptit.cvrecruiter.scoring;

import com.ptit.cvrecruiter.ai.AiAnalysis;
import com.ptit.cvrecruiter.ai.AiAnalyzer;
import com.ptit.cvrecruiter.ai.AiResult;
import com.ptit.cvrecruiter.matching.RuleBasedKeywordMatcher;
import com.ptit.cvrecruiter.model.AiStatus;
import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.model.EvidenceSource;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.MatchReport;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillMatch;
import com.ptit.cvrecruiter.model.SkillWeight;
import com.ptit.cvrecruiter.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringServiceTest {

    private static final AiAnalyzer NO_AI = new AiAnalyzer() {
        @Override
        public boolean isConfigured() {
            return false;
        }

        @Override
        public AiResult analyze(JobPosting job, String cvText) {
            return AiResult.notConfigured();
        }
    };

    private final JobPosting job = new JobPosting("Backend", "IT", "JD",
            List.of(new Skill("Java", SkillWeight.REQUIRED),
                    new Skill("SQL", SkillWeight.REQUIRED),
                    new Skill("Docker", SkillWeight.PREFERRED)));

    @Test
    void keywordOnlyScoreUsesWeights() {
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), NO_AI, 0.4);

        MatchReport report = service.score(job, "Tôi có kinh nghiệm Java và SQL.");

        // Java (2) + SQL (2) trên tổng 5 điểm trọng số = 80
        assertEquals(80.0, report.getKeywordScore());
        assertEquals(80.0, report.getFinalScore());
        assertEquals(AiStatus.NOT_CONFIGURED, report.getAiStatus());
        assertEquals(-1, report.getAiScore());
        assertEquals(2, report.countByStatus(com.ptit.cvrecruiter.model.MatchStatus.MATCHED));
    }

    @Test
    void blendsAiScoreWithConfiguredWeight() {
        AiAnalysis analysis = JsonUtil.gson().fromJson("""
                {"aiScore": 50, "summary": "Khá phù hợp",
                 "highlights": ["Có kinh nghiệm Java"],
                 "concerns": ["Chưa rõ Docker"],
                 "skills": [{"skill": "Docker", "status": "MATCHED",
                             "evidence": "Đóng gói ứng dụng bằng Docker", "note": "Có đề cập"}]}
                """, AiAnalysis.class);
        AiAnalyzer ai = new AiAnalyzer() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public AiResult analyze(JobPosting job, String cvText) {
                return AiResult.ok(analysis);
            }
        };
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), ai, 0.4);

        MatchReport report = service.score(job, "Tôi có kinh nghiệm Java và SQL.");

        assertEquals(80.0, report.getKeywordScore());
        assertEquals(50.0, report.getAiScore());
        // 80 * 0.6 + 50 * 0.4 = 68
        assertEquals(68.0, report.getFinalScore());
        assertEquals(AiStatus.OK, report.getAiStatus());
        assertEquals("Khá phù hợp", report.getAiSummary());

        SkillMatch docker = report.getSkillMatches().stream()
                .filter(s -> s.getSkillName().equals("Docker")).findFirst().orElseThrow();
        assertEquals(EvidenceSource.AI, docker.getSource());
        assertEquals("Đóng gói ứng dụng bằng Docker", docker.getEvidence());

        SkillMatch java = report.getSkillMatches().stream()
                .filter(s -> s.getSkillName().equals("Java")).findFirst().orElseThrow();
        assertEquals(EvidenceSource.KEYWORD, java.getSource());
    }

    @Test
    void clampsOutOfRangeAiScore() {
        AiAnalysis analysis = JsonUtil.gson().fromJson(
                "{\"aiScore\": 250, \"summary\": \"\", \"skills\": []}", AiAnalysis.class);
        AiAnalyzer ai = new AiAnalyzer() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public AiResult analyze(JobPosting job, String cvText) {
                return AiResult.ok(analysis);
            }
        };
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), ai, 0.4);

        MatchReport report = service.score(job, "Java SQL Docker");

        assertEquals(100.0, report.getAiScore());
        assertEquals(100.0, report.getFinalScore());
    }

    @Test
    void criteriaAreEvaluatedAndBlendedIntoRuleScore() {
        job.setCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2)));
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), NO_AI, 0.4, 0.35);

        MatchReport report = service.score(job,
                "Tôi có 3 năm kinh nghiệm Java và SQL. Tốt nghiệp Đại học Bách khoa.");

        assertEquals(2, report.getCriteriaResults().size());
        assertEquals(100.0, report.getCriteriaScore());
        assertEquals(2, report.countCriteriaMet());
        // 80 * 0.65 + 100 * 0.35 = 87
        assertEquals(87.0, report.getFinalScore());
    }

    @Test
    void partialCriteriaLowerTheFinalScore() {
        job.setCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 3),
                new Criterion("Chứng chỉ", CriterionType.CERTIFICATION, "AWS Certified", 1)));
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), NO_AI, 0.4, 0.35);

        // Kinh nghiệm 1.5/2 năm → PARTIAL (0.5), chứng chỉ không có → NOT_MET (0)
        // criteriaScore = (3 * 0.5 + 1 * 0) / 4 * 100 = 37.5
        MatchReport report = service.score(job,
                "Lập trình Java, SQL. Có 1,5 năm kinh nghiệm làm backend.");

        assertEquals(37.5, report.getCriteriaScore());
        assertEquals(0, report.countCriteriaMet());
        // 80 * 0.65 + 37.5 * 0.35 = 65.1 (làm tròn 1 chữ số)
        assertEquals(65.1, report.getFinalScore());
    }

    @Test
    void jobWithoutCriteriaKeepsKeywordOnlyBehaviour() {
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), NO_AI, 0.4, 0.35);

        MatchReport report = service.score(job, "Tôi có 3 năm kinh nghiệm Java và SQL.");

        assertEquals(-1, report.getCriteriaScore());
        assertEquals(0, report.getCriteriaResults().size());
        assertEquals(80.0, report.getFinalScore());
    }

    @Test
    void aiCanUpgradeCriterionWhenRuleFindsNoEvidence() {
        job.setCriteria(List.of(
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1)));
        AiAnalysis analysis = JsonUtil.gson().fromJson("""
                {"aiScore": 70, "summary": "Phù hợp",
                 "dimensions": [{"name": "Ngoại ngữ", "score": 85,
                                 "comment": "Làm việc tại Mỹ 4 năm"}],
                 "skills": [],
                 "criteria": [{"name": "Tiếng Anh", "type": "LANGUAGE", "status": "MET",
                               "actual": "làm việc tại Mỹ 4 năm",
                               "evidence": "Senior developer, New York, USA (2021-2025)",
                               "note": "CV viết bằng tiếng Anh, làm việc tại Mỹ",
                               "inferred": true}]}
                """, AiAnalysis.class);
        AiAnalyzer ai = new AiAnalyzer() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public AiResult analyze(JobPosting job, String cvText) {
                return AiResult.ok(analysis);
            }
        };
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), ai, 0.4, 0.35);

        MatchReport report = service.score(job,
                "Java SQL. Senior developer, New York, USA (2021-2025).");

        assertEquals(100.0, report.getCriteriaScore());
        assertEquals(1, report.countCriteriaMet());
        assertEquals("AI", report.getCriteriaResults().get(0).getSource());
        assertTrue(report.getCriteriaResults().get(0).isInferred());
        assertEquals(1, report.getAiDimensions().size());
        // Điểm quy tắc 80*0.65 + 100*0.35 = 87; tổng 87*0.6 + 70*0.4 = 80.2
        assertEquals(80.2, report.getFinalScore());
    }

    @Test
    void aiWeightIsClampedToValidRange() {
        ScoringService service = new ScoringService(new RuleBasedKeywordMatcher(), NO_AI, 5.0);
        MatchReport report = service.score(job, "Java SQL");
        // Trọng số AI bị kẹp về 1 nhưng vì AI không cấu hình, điểm cuối vẫn là điểm từ khoá
        // (Java + SQL = 4/5 trọng số)
        assertEquals(80.0, report.getFinalScore());
    }
}
