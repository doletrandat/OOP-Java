package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.model.CriterionSource;
import com.ptit.cvrecruiter.ai.CriterionVerdict;
import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.model.CriterionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriteriaMergerTest {

    private final CriteriaEvaluator evaluator = new CriteriaEvaluator();
    private final CriteriaMerger merger = new CriteriaMerger();

    @Test
    void aiUpgradesCriterionWhenRulesFoundNothing() {
        Criterion language = new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1);
        List<CriterionResult> results = evaluator.evaluate(List.of(language),
                "Senior Java Developer, New York, USA (2021-2025). Built microservices for US clients.");
        assertEquals(CriterionStatus.NOT_MET, results.get(0).getStatus());
        assertTrue(results.get(0).getEvidence().isBlank());

        CriterionVerdict verdict = verdict("Tiếng Anh", "MET",
                "làm việc tại Mỹ 4 năm, CV viết bằng tiếng Anh", "", true);
        merger.merge(results, List.of(verdict));

        CriterionResult merged = results.get(0);
        assertEquals(CriterionStatus.MET, merged.getStatus());
        assertEquals(CriterionSource.AI, merged.getSource());
        assertTrue(merged.isInferred());
        assertEquals("làm việc tại Mỹ 4 năm, CV viết bằng tiếng Anh", merged.getActual());
        assertTrue(merged.getNote().startsWith("AI suy luận:"));
    }

    @Test
    void aiCorrectsSelfClaimedExperience() {
        Criterion experience = new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "3", 3);
        List<CriterionResult> results = evaluator.evaluate(List.of(experience),
                "3 năm kinh nghiệm lập trình Java (tính từ năm 2 đại học).\nThực tập sinh Java 06/2025 đến 09/2025.");
        assertEquals(CriterionStatus.MET, results.get(0).getStatus());

        merger.merge(results, List.of(verdict("Kinh nghiệm", "NOT_MET",
                "khoảng 4 tháng thực tập", "Thực tập sinh Java 06/2025 đến 09/2025", false)));

        CriterionResult merged = results.get(0);
        assertEquals(CriterionStatus.NOT_MET, merged.getStatus());
        assertEquals(CriterionSource.AI, merged.getSource());
        assertEquals("khoảng 4 tháng thực tập", merged.getActual());
        // HR vẫn thấy quy tắc đã đọc được gì
        assertTrue(merged.getNote().contains("Quy tắc từ khoá đọc được \"3 năm kinh nghiệm\""));
    }

    @Test
    void aiCountsExperienceFromDateRanges() {
        Criterion experience = new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "3", 3);
        List<CriterionResult> results = evaluator.evaluate(List.of(experience),
                "Senior Backend Engineer 2021 đến nay. Trước đó 1 năm làm freelance.");
        assertEquals(CriterionStatus.NOT_MET, results.get(0).getStatus());

        merger.merge(results, List.of(verdict("Kinh nghiệm", "MET", "khoảng 6 năm", "2021 đến nay", true)));

        assertEquals(CriterionStatus.MET, results.get(0).getStatus());
        assertTrue(results.get(0).isInferred());
    }

    @Test
    void agreementIsMarkedAsBoth() {
        Criterion education = new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2);
        List<CriterionResult> results = evaluator.evaluate(List.of(education), "Cử nhân Kinh tế, 2020.");

        merger.merge(results, List.of(verdict("Trình độ", "MET", "Đại học", "Cử nhân Kinh tế", false)));

        assertEquals(CriterionStatus.MET, results.get(0).getStatus());
        assertEquals(CriterionSource.BOTH, results.get(0).getSource());
    }

    @Test
    void aiNeedsConcreteBasisToDowngrade() {
        Criterion education = new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2);
        List<CriterionResult> results = evaluator.evaluate(List.of(education),
                "Tốt nghiệp Đại học Bách khoa Hà Nội.");
        assertEquals(CriterionStatus.MET, results.get(0).getStatus());

        merger.merge(results, List.of(verdict("Trình độ", "NOT_MET", "", "", false)));

        assertEquals(CriterionStatus.MET, results.get(0).getStatus());
        assertEquals(CriterionSource.RULE, results.get(0).getSource());
    }

    @Test
    void aiCannotOverrideExplicitLowerCertificate() {
        Criterion language = new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 900", 1);
        List<CriterionResult> results = evaluator.evaluate(List.of(language),
                "Chứng chỉ TOEIC 650 (2023).");
        assertEquals(CriterionStatus.NOT_MET, results.get(0).getStatus());
        assertTrue(!results.get(0).getEvidence().isBlank());

        merger.merge(results, List.of(verdict("Tiếng Anh", "MET", "TOEIC 900", "TOEIC 900", false)));

        // Quy tắc đã có bằng chứng điểm thấp hơn → giữ nguyên kết luận
        assertEquals(CriterionStatus.NOT_MET, results.get(0).getStatus());
    }

    @Test
    void ignoresVerdictsForCriteriaNotInJob() {
        Criterion experience = new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2);
        List<CriterionResult> results = evaluator.evaluate(List.of(experience), "Kỹ năng: Java.");
        int before = results.size();

        merger.merge(results, List.of(verdict("Tiêu chí tự bịa", "MET", "abc", "abc", false)));

        assertEquals(before, results.size());
        assertEquals(CriterionStatus.UNKNOWN, results.get(0).getStatus());
    }

    private CriterionVerdict verdict(String name, String status, String actual,
                                     String evidence, boolean inferred) {
        // Dựng qua JSON để giống đúng dữ liệu AI trả về (status không có setter công khai)
        return com.ptit.cvrecruiter.util.JsonUtil.gson().fromJson(
                "{\"name\":\"" + name + "\",\"type\":\"LANGUAGE\",\"status\":\"" + status + "\","
                        + "\"actual\":\"" + actual + "\",\"evidence\":\"" + evidence + "\","
                        + "\"note\":\"Suy ra từ bối cảnh làm việc.\",\"inferred\":" + inferred + "}",
                CriterionVerdict.class);
    }
}
