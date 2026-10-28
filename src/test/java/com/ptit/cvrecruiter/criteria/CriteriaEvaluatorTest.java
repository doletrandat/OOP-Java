package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.model.CriterionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriteriaEvaluatorTest {

    private final CriteriaEvaluator evaluator = new CriteriaEvaluator();

    @Test
    void readsYearsOfExperienceFromVietnameseCv() {
        CriterionResult result = evaluate(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "3", 2),
                "Có 2 năm kinh nghiệm phát triển backend Java.");

        assertEquals(CriterionStatus.NOT_MET, result.getStatus());
        assertEquals("2 năm kinh nghiệm", result.getActual());
        assertTrue(result.getEvidence().contains("2 năm kinh nghiệm"));
    }

    @Test
    void readsYearsOfExperienceFromEnglishCv() {
        CriterionResult result = evaluate(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                "Backend developer with 3+ years of experience in Java.");

        assertEquals(CriterionStatus.MET, result.getStatus());
    }

    @Test
    void reportsUnknownWhenCvDoesNotMentionExperience() {
        CriterionResult result = evaluate(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                "Kỹ năng: Java, SQL, Git.");

        assertEquals(CriterionStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void comparesEducationLevels() {
        String cv = "Tốt nghiệp Đại học Bách khoa Hà Nội, khoa Công nghệ thông tin.";

        CriterionResult enough = evaluate(
                new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2), cv);
        assertEquals(CriterionStatus.MET, enough.getStatus());
        assertEquals("Đại học", enough.getActual());

        CriterionResult higher = evaluate(
                new Criterion("Trình độ", CriterionType.EDUCATION, "Thạc sĩ", 2), cv);
        assertEquals(CriterionStatus.NOT_MET, higher.getStatus());
        assertEquals("Đại học", higher.getActual());
    }

    @Test
    void comparesLanguageCertificates() {
        String cv = "Chứng chỉ: TOEIC 780, IELTS 6.5.";

        assertEquals(CriterionStatus.MET, evaluate(
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1), cv).getStatus());
        assertEquals(CriterionStatus.NOT_MET, evaluate(
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 900", 1), cv).getStatus());
        assertEquals(CriterionStatus.MET, evaluate(
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "IELTS 6.0", 1), cv).getStatus());
    }

    @Test
    void matchesCertificationKeywords() {
        CriterionResult result = evaluate(
                new Criterion("Chứng chỉ", CriterionType.CERTIFICATION, "AWS Certified, Oracle OCP", 2),
                "AWS Certified Solutions Architect Associate (2024).");

        assertEquals(CriterionStatus.MET, result.getStatus());
        assertTrue(result.getEvidence().toLowerCase().contains("aws certified"));
    }

    @Test
    void missingCertificationIsNotMet() {
        CriterionResult result = evaluate(
                new Criterion("Chứng chỉ", CriterionType.CERTIFICATION, "AWS Certified", 2),
                "Kỹ năng: Java, Spring Boot, MySQL.");

        assertEquals(CriterionStatus.NOT_MET, result.getStatus());
    }

    @Test
    void emptyRequirementIsUnknown() {
        CriterionResult result = evaluate(
                new Criterion("Ghi chú", CriterionType.OTHER, "", 1), "Bất kỳ nội dung.");

        assertEquals(CriterionStatus.UNKNOWN, result.getStatus());
    }

    private CriterionResult evaluate(Criterion criterion, String cvText) {
        List<CriterionResult> results = evaluator.evaluate(List.of(criterion), cvText);
        assertEquals(1, results.size());
        return results.get(0);
    }
}
