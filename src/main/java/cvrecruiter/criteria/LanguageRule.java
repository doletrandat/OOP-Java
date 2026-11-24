package cvrecruiter.criteria;

import cvrecruiter.model.CriterionResult;
import cvrecruiter.model.CriterionStatus;
import cvrecruiter.util.TextNormalizer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chứng chỉ ngoại ngữ có điểm (TOEIC, IELTS, TOEFL, HSK, JLPT, TOPIK): đọc điểm trong CV
 * và so sánh số. Yêu cầu không nêu bài thi hoặc điểm thì chuyển cho quy tắc dự phòng.
 */
class LanguageRule implements CriterionRule {

    private static final List<String> TESTS = List.of("TOEIC", "IELTS", "TOEFL", "HSK", "JLPT", "TOPIK");
    private static final Pattern JLPT_LEVEL = Pattern.compile("n?\\s*([1-5])");

    private final CriterionRule fallback;

    LanguageRule(CriterionRule fallback) {
        this.fallback = fallback;
    }

    @Override
    public CriterionResult evaluate(CriterionResult result, String cvText) {
        Requirement required = parse(result.getRequirement());
        if (required == null) {
            return fallback.evaluate(result, cvText);
        }

        Double found = findScore(cvText, required.test());
        if (found == null) {
            return result.conclude(CriterionStatus.NOT_MET, "", "",
                    "Không tìm thấy chứng chỉ " + required.test() + " trong CV.");
        }

        String actual = required.test() + " " + CvText.formatNumber(found);
        String evidence = CvText.findEvidence(cvText, List.of(required.test().toLowerCase(Locale.ROOT)));
        if (required.isPassedBy(found)) {
            return result.conclude(CriterionStatus.MET, actual, evidence, "");
        }
        return result.conclude(CriterionStatus.NOT_MET, actual, evidence,
                "Yêu cầu " + required.test() + " " + CvText.formatNumber(required.minScore()) + " trở lên.");
    }

    private static Requirement parse(String requirement) {
        String normalized = TextNormalizer.normalize(requirement);
        String test = TESTS.stream()
                .filter(t -> normalized.contains(t.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(null);
        if (test == null) return null;

        if ("JLPT".equals(test)) {
            Matcher level = JLPT_LEVEL.matcher(normalized);
            return level.find() ? new Requirement(test, Double.parseDouble(level.group(1)), true) : null;
        }
        Matcher number = CvText.NUMBER.matcher(normalized);
        return number.find() ? new Requirement(test, CvText.parseDouble(number.group(1)), false) : null;
    }

    private static Double findScore(String cvText, String test) {
        String lower = test.toLowerCase(Locale.ROOT);
        Pattern pattern = switch (test) {
            case "IELTS" -> Pattern.compile("ielts[^0-9]{0,20}(\\d(?:[.,]\\d)?)");
            case "TOEIC", "TOEFL" -> Pattern.compile(lower + "[^0-9]{0,20}(\\d{2,3})");
            case "HSK", "TOPIK" -> Pattern.compile(lower + "[^0-9]{0,20}(\\d)");
            case "JLPT" -> Pattern.compile("jlpt[^a-z0-9]{0,10}n?\\s*([1-5])");
            default -> null;
        };
        if (pattern == null) return null;
        Matcher m = pattern.matcher(cvText.toLowerCase(Locale.ROOT));
        return m.find() ? CvText.parseDouble(m.group(1)) : null;
    }

    /** JLPT N1 giỏi hơn N2 nên với JLPT số càng nhỏ càng tốt. */
    private record Requirement(String test, double minScore, boolean lowerIsBetter) {
        boolean isPassedBy(double score) {
            return lowerIsBetter ? score <= minScore : score >= minScore;
        }
    }
}
