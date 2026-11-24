package cvrecruiter.criteria;

import cvrecruiter.matching.SynonymDictionary;
import cvrecruiter.model.Criterion;
import cvrecruiter.model.CriterionResult;
import cvrecruiter.model.CriterionType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Đối chiếu các tiêu chí bổ sung của JD (kinh nghiệm, trình độ, ngoại ngữ,
 * chứng chỉ, tiêu chí tự do) với văn bản CV.
 *
 * <p>Nguyên tắc: chỉ kết luận dựa trên nội dung có thật trong CV và luôn trả về
 * câu trích dẫn để HR kiểm chứng. Không đủ thông tin thì báo "Chưa rõ" thay vì đoán.
 * Mỗi loại tiêu chí do một {@link CriterionRule} riêng xử lý.
 */
public class CriteriaEvaluator {

    private final Map<CriterionType, CriterionRule> rules = new EnumMap<>(CriterionType.class);

    public CriteriaEvaluator() {
        this(new SynonymDictionary());
    }

    public CriteriaEvaluator(SynonymDictionary dictionary) {
        CriterionRule keywords = new KeywordRule(dictionary);
        rules.put(CriterionType.EXPERIENCE, new ExperienceRule());
        rules.put(CriterionType.EDUCATION, new EducationRule(keywords));
        rules.put(CriterionType.LANGUAGE, new LanguageRule(keywords));
        rules.put(CriterionType.CERTIFICATION, keywords);
        rules.put(CriterionType.OTHER, keywords);
    }

    public List<CriterionResult> evaluate(List<Criterion> criteria, String cvText) {
        if (criteria == null || criteria.isEmpty()) return List.of();
        String text = cvText == null ? "" : cvText;
        return criteria.stream().map(c -> evaluateOne(c, text)).toList();
    }

    private CriterionResult evaluateOne(Criterion criterion, String cvText) {
        CriterionResult result = new CriterionResult(criterion);
        if (criterion.getRequirement().isBlank()) {
            return result.unknown("HR chưa nhập yêu cầu cho tiêu chí này.");
        }
        return rules.get(criterion.getType()).evaluate(result, cvText);
    }
}
