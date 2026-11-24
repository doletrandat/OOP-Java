package cvrecruiter.criteria;

import cvrecruiter.model.CriterionResult;
import cvrecruiter.model.CriterionStatus;
import cvrecruiter.util.TextNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Số năm kinh nghiệm: lấy con số lớn nhất CV nêu và so với yêu cầu tối thiểu. */
class ExperienceRule implements CriterionRule {

    /**
     * "4 năm", "2,5 years", "3+ yrs". Số phải đứng riêng (không phải đuôi của số điện thoại
     * "0978 220 914") và chữ "năm" không được là đầu của email ("nam.buivan@...").
     */
    private static final Pattern YEARS_IN_CV = Pattern.compile(
            "(?<![\\d.,])(\\d{1,2})(?:[.,](\\d))?\\s*\\+?\\s*(?:nam|years?|yrs?|year)(?![a-z0-9@]|\\.[a-z0-9])");

    @Override
    public CriterionResult evaluate(CriterionResult result, String cvText) {
        Matcher required = CvText.NUMBER.matcher(CvText.normalizeWithDecimals(result.getRequirement()));
        if (!required.find()) {
            return result.unknown("Yêu cầu kinh nghiệm phải là một con số (ví dụ: 2).");
        }
        double requiredYears = CvText.parseDouble(required.group(1));

        double foundYears = -1;
        String matchedPhrase = "";
        Matcher m = YEARS_IN_CV.matcher(CvText.normalizeWithDecimals(cvText));
        while (m.find()) {
            double value = CvText.parseDouble(m.group(1) + (m.group(2) == null ? "" : "." + m.group(2)));
            if (value > foundYears) {
                foundYears = value;
                matchedPhrase = m.group();
            }
        }
        if (foundYears < 0) {
            return result.unknown("CV không nêu số năm kinh nghiệm.");
        }

        String actual = CvText.formatNumber(foundYears) + " năm kinh nghiệm";
        String evidence = CvText.findEvidence(cvText, List.of(TextNormalizer.normalize(matchedPhrase)));
        if (foundYears >= requiredYears) {
            return result.conclude(CriterionStatus.MET, actual, evidence, "");
        }
        CriterionStatus status = foundYears >= requiredYears - 0.5 ? CriterionStatus.PARTIAL : CriterionStatus.NOT_MET;
        return result.conclude(status, actual, evidence,
                "Yêu cầu tối thiểu " + CvText.formatNumber(requiredYears) + " năm.");
    }
}
