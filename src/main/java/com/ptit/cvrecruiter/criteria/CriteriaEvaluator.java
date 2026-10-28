package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.matching.SynonymDictionary;
import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đối chiếu các tiêu chí bổ sung của JD (kinh nghiệm, trình độ, ngoại ngữ,
 * chứng chỉ, tiêu chí tự do) với văn bản CV.
 *
 * Nguyên tắc: chỉ kết luận dựa trên nội dung có thật trong CV và luôn trả về
 * câu trích dẫn để HR kiểm chứng. Không đủ thông tin thì báo "Chưa rõ"
 * thay vì đoán.
 */
public class CriteriaEvaluator {

    private static final Pattern YEARS_IN_CV = Pattern.compile(
            "(\\d{1,2})(?:[.,](\\d))?\\s*\\+?\\s*(?:nam|years?|yrs?|year)\\b");
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
    private static final int MAX_EVIDENCE_CHARS = 240;

    /** Trình độ theo thứ tự từ cao xuống thấp: rank, các cách diễn đạt. */
    private static final List<EducationLevel> EDUCATION_LEVELS = List.of(
            new EducationLevel(5, "Tiến sĩ", "tien si", "phd", "ph.d", "doctorate", "doctor of philosophy"),
            new EducationLevel(4, "Thạc sĩ", "thac si", "master", "mba", "thac si quan tri kinh doanh"),
            new EducationLevel(3, "Đại học", "dai hoc", "cu nhan", "bachelor", "university", "ky su", "engineer"),
            new EducationLevel(2, "Cao đẳng", "cao dang", "college", "associate degree", "associate"),
            new EducationLevel(1, "Trung cấp / THPT", "trung cap", "trung hoc", "high school", "thpt", "pho thong"));

    private final SynonymDictionary dictionary;

    public CriteriaEvaluator() {
        this(new SynonymDictionary());
    }

    public CriteriaEvaluator(SynonymDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<CriterionResult> evaluate(List<Criterion> criteria, String cvText) {
        List<CriterionResult> results = new ArrayList<>();
        if (criteria == null || criteria.isEmpty()) return results;
        String text = cvText == null ? "" : cvText;
        for (Criterion criterion : criteria) {
            results.add(evaluateOne(criterion, text));
        }
        return results;
    }

    private CriterionResult evaluateOne(Criterion criterion, String cvText) {
        CriterionResult result = new CriterionResult(criterion);
        if (criterion.getRequirement().isBlank()) {
            result.setStatus(CriterionStatus.UNKNOWN);
            result.setNote("HR chưa nhập yêu cầu cho tiêu chí này.");
            return result;
        }
        return switch (criterion.getType()) {
            case EXPERIENCE -> evaluateExperience(result, cvText);
            case EDUCATION -> evaluateEducation(result, cvText);
            case LANGUAGE -> evaluateLanguage(result, cvText);
            case CERTIFICATION, OTHER -> evaluateKeywords(result, cvText);
        };
    }

    /* ================== Số năm kinh nghiệm ================== */

    private CriterionResult evaluateExperience(CriterionResult result, String cvText) {
        String normalized = normalizeWithDecimals(cvText);
        Matcher required = NUMBER.matcher(normalizeWithDecimals(result.getRequirement()));
        if (!required.find()) {
            result.setStatus(CriterionStatus.UNKNOWN);
            result.setNote("Yêu cầu kinh nghiệm phải là một con số (ví dụ: 2).");
            return result;
        }
        double requiredYears = parseDouble(required.group(1));

        double foundYears = -1;
        String matchedPhrase = "";
        Matcher m = YEARS_IN_CV.matcher(normalized);
        while (m.find()) {
            double value = parseDouble(m.group(1) + (m.group(2) == null ? "" : "." + m.group(2)));
            if (value > foundYears) {
                foundYears = value;
                matchedPhrase = m.group();
            }
        }

        if (foundYears < 0) {
            result.setStatus(CriterionStatus.UNKNOWN);
            result.setNote("CV không nêu số năm kinh nghiệm.");
            return result;
        }

        result.setActual(formatYears(foundYears) + " năm kinh nghiệm");
        result.setEvidence(findEvidence(cvText, List.of(TextNormalizer.normalize(matchedPhrase))));
        if (foundYears >= requiredYears) {
            result.setStatus(CriterionStatus.MET);
        } else {
            result.setStatus(foundYears >= requiredYears - 0.5
                    ? CriterionStatus.PARTIAL : CriterionStatus.NOT_MET);
            result.setNote("Yêu cầu tối thiểu " + formatYears(requiredYears) + " năm.");
        }
        return result;
    }

    /* ================== Trình độ đào tạo ================== */

    private CriterionResult evaluateEducation(CriterionResult result, String cvText) {
        int requiredRank = detectEducationRank(result.getRequirement());
        if (requiredRank == 0) {
            // HR nhập yêu cầu không thuộc thang trình độ chuẩn → đối chiếu theo từ khoá
            return evaluateKeywords(result, cvText);
        }

        EducationLevel found = detectEducation(cvText);
        result.setActual(found == null ? "" : found.label);
        if (found == null) {
            result.setStatus(CriterionStatus.UNKNOWN);
            result.setNote("CV không nêu trình độ đào tạo.");
            return result;
        }

        result.setEvidence(findEvidence(cvText, found.keywords));
        result.setStatus(found.rank >= requiredRank ? CriterionStatus.MET : CriterionStatus.NOT_MET);
        return result;
    }

    private int detectEducationRank(String text) {
        EducationLevel level = detectEducation(text);
        return level == null ? 0 : level.rank;
    }

    private EducationLevel detectEducation(String text) {
        String normalized = TextNormalizer.normalize(text);
        for (EducationLevel level : EDUCATION_LEVELS) {
            for (String keyword : level.keywords) {
                if (containsWord(normalized, keyword)) return level;
            }
        }
        return null;
    }

    /* ================== Chứng chỉ ngoại ngữ ================== */

    private CriterionResult evaluateLanguage(CriterionResult result, String cvText) {
        LanguageRequirement required = parseLanguageRequirement(result.getRequirement());
        if (required == null) {
            return evaluateKeywords(result, cvText);
        }

        Double found = findLanguageScore(cvText, required.test);
        if (found == null) {
            result.setStatus(CriterionStatus.NOT_MET);
            result.setNote("Không tìm thấy chứng chỉ " + required.test + " trong CV.");
            return result;
        }

        result.setActual(required.test + " " + formatScore(found));
        result.setEvidence(findEvidence(cvText, List.of(required.test.toLowerCase(Locale.ROOT))));

        boolean passed = required.lowerIsBetter
                ? found <= required.minScore
                : found >= required.minScore;
        if (passed) {
            result.setStatus(CriterionStatus.MET);
        } else {
            result.setStatus(CriterionStatus.NOT_MET);
            result.setNote("Yêu cầu " + required.test + " "
                    + formatScore(required.minScore) + " trở lên.");
        }
        return result;
    }

    private LanguageRequirement parseLanguageRequirement(String requirement) {
        String normalized = TextNormalizer.normalize(requirement);
        String test = detectTest(normalized);
        if (test == null) return null;

        if ("JLPT".equals(test)) {
            Matcher level = Pattern.compile("n?\\s*([1-5])").matcher(normalized);
            if (!level.find()) return null;
            return new LanguageRequirement(test, Double.parseDouble(level.group(1)), true);
        }

        Matcher number = NUMBER.matcher(normalized);
        if (!number.find()) return null;
        return new LanguageRequirement(test, parseDouble(number.group(1)), false);
    }

    private String detectTest(String normalized) {
        for (String test : List.of("TOEIC", "IELTS", "TOEFL", "HSK", "JLPT", "TOPIK")) {
            if (normalized.contains(test.toLowerCase(Locale.ROOT))) return test;
        }
        return null;
    }

    private Double findLanguageScore(String cvText, String test) {
        String text = cvText.toLowerCase(Locale.ROOT);
        Pattern pattern = switch (test) {
            case "IELTS" -> Pattern.compile("ielts[^0-9]{0,20}(\\d(?:[.,]\\d)?)");
            case "TOEIC", "TOEFL" -> Pattern.compile(
                    test.toLowerCase(Locale.ROOT) + "[^0-9]{0,20}(\\d{2,3})");
            case "HSK", "TOPIK" -> Pattern.compile(
                    test.toLowerCase(Locale.ROOT) + "[^0-9]{0,20}(\\d)");
            case "JLPT" -> Pattern.compile("jlpt[^a-z0-9]{0,10}n?\\s*([1-5])");
            default -> null;
        };
        if (pattern == null) return null;
        Matcher m = pattern.matcher(text);
        return m.find() ? parseDouble(m.group(1)) : null;
    }

    /* ================== Chứng chỉ / tiêu chí theo từ khoá ================== */

    private CriterionResult evaluateKeywords(CriterionResult result, String cvText) {
        String normalizedCv = TextNormalizer.normalize(cvText);
        List<String> keywords = splitKeywords(result.getRequirement());

        for (String keyword : keywords) {
            for (String variant : dictionary.expand(keyword)) {
                if (variant.isBlank()) continue;
                if (containsWord(normalizedCv, variant)) {
                    result.setStatus(CriterionStatus.MET);
                    result.setActual(keyword);
                    result.setEvidence(findEvidence(cvText, List.of(variant)));
                    return result;
                }
            }
        }

        result.setStatus(CriterionStatus.NOT_MET);
        result.setNote("Không tìm thấy trong CV (đã tìm: "
                + TextNormalizer.truncate(result.getRequirement(), 120) + ").");
        return result;
    }

    /**
     * Tách yêu cầu thành các phương án ("AWS Certified, Oracle OCP" hoặc
     * "TOEIC hoặc IELTS"). Phải tách trên văn bản gốc vì bước chuẩn hoá
     * biến dấu phẩy thành khoảng trắng; sau đó mới chuẩn hoá từng phương án.
     */
    private List<String> splitKeywords(String requirement) {
        return Arrays.stream(requirement.split("(?i)[,;/]|\\bhoặc\\b|\\bhoac\\b|\\bor\\b"))
                .map(TextNormalizer::normalize)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /* ================== Tiện ích ================== */

    /** Tìm câu trong CV chứa một trong các cụm từ cần trích dẫn. */
    private String findEvidence(String cvText, List<String> needles) {
        for (String sentence : TextNormalizer.splitSentences(cvText)) {
            String normalized = TextNormalizer.normalize(sentence);
            for (String needle : needles) {
                if (!needle.isBlank() && containsWord(normalized, needle)) {
                    return TextNormalizer.truncate(sentence, MAX_EVIDENCE_CHARS);
                }
            }
        }
        return "";
    }

    /** So khớp theo ranh giới từ để "aws" không khớp trong "laws". */
    private boolean containsWord(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) return false;
        Pattern p = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(needle) + "(?![a-z0-9])");
        return p.matcher(haystack).find();
    }

    private double parseDouble(String raw) {
        return Double.parseDouble(raw.replace(',', '.'));
    }

    /**
     * Chuẩn hoá văn bản nhưng giữ dấu thập phân kiểu Việt Nam:
     * "1,5 năm" phải thành "1.5 nam" thay vì "1 5 nam".
     */
    private String normalizeWithDecimals(String input) {
        String text = input == null ? "" : input.replaceAll("(\\d),(\\d)", "$1.$2");
        return TextNormalizer.normalize(text);
    }

    private String formatYears(double years) {
        return years == Math.rint(years) ? String.valueOf((int) years) : String.valueOf(years);
    }

    private String formatScore(double score) {
        return score == Math.rint(score) ? String.valueOf((int) score) : String.valueOf(score);
    }

    /** Một bậc trình độ: rank càng cao càng yêu cầu cao hơn. */
    private static final class EducationLevel {
        private final int rank;
        private final String label;
        private final List<String> keywords;

        private EducationLevel(int rank, String label, String... keywords) {
            this.rank = rank;
            this.label = label;
            this.keywords = List.of(keywords);
        }
    }

    private record LanguageRequirement(String test, double minScore, boolean lowerIsBetter) {
    }
}
