package cvrecruiter.criteria;

import cvrecruiter.model.CriterionResult;
import cvrecruiter.model.CriterionStatus;
import cvrecruiter.util.TextNormalizer;

import java.util.List;

/**
 * Trình độ đào tạo theo thang chuẩn (THPT → Tiến sĩ). Yêu cầu không thuộc thang
 * (ví dụ "Chuyên ngành CNTT") thì chuyển cho quy tắc dự phòng.
 */
class EducationRule implements CriterionRule {

    /** Trình độ theo thứ tự từ cao xuống thấp. */
    private static final List<EducationLevel> LEVELS = List.of(
            new EducationLevel(5, "Tiến sĩ", "tien si", "phd", "ph.d", "doctorate", "doctor of philosophy"),
            new EducationLevel(4, "Thạc sĩ", "thac si", "master", "mba", "thac si quan tri kinh doanh"),
            new EducationLevel(3, "Đại học", "dai hoc", "cu nhan", "bachelor", "university", "ky su", "engineer"),
            new EducationLevel(2, "Cao đẳng", "cao dang", "college", "associate degree", "associate"),
            new EducationLevel(1, "Trung cấp / THPT", "trung cap", "trung hoc", "high school", "thpt", "pho thong"));

    private final CriterionRule fallback;

    EducationRule(CriterionRule fallback) {
        this.fallback = fallback;
    }

    @Override
    public CriterionResult evaluate(CriterionResult result, String cvText) {
        EducationLevel required = detect(result.getRequirement());
        if (required == null) {
            return fallback.evaluate(result, cvText);
        }

        // Bỏ qua các câu "đang học liên thông Đại học", "dự kiến tốt nghiệp": chưa có bằng đó
        String completed = String.join("\n", TextNormalizer.splitSentences(cvText).stream()
                .filter(s -> CvText.qualify(s) == CriterionStatus.MET)
                .toList());
        EducationLevel found = detect(completed);
        if (found == null) {
            return result.unknown("CV không nêu trình độ đào tạo.");
        }
        return result.conclude(
                found.rank() >= required.rank() ? CriterionStatus.MET : CriterionStatus.NOT_MET,
                found.label(),
                CvText.findEvidence(completed, found.keywords()),
                "");
    }

    private static EducationLevel detect(String text) {
        String normalized = TextNormalizer.normalize(text);
        for (EducationLevel level : LEVELS) {
            for (String keyword : level.keywords()) {
                if (CvText.containsWord(normalized, keyword)) return level;
            }
        }
        return null;
    }

    /** Một bậc trình độ: rank càng cao càng yêu cầu cao hơn. */
    private record EducationLevel(int rank, String label, List<String> keywords) {
        EducationLevel(int rank, String label, String... keywords) {
            this(rank, label, List.of(keywords));
        }
    }
}
