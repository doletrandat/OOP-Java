package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Các thao tác đọc văn bản CV dùng chung cho các quy tắc tiêu chí. */
final class CvText {

    static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
    private static final int MAX_EVIDENCE_CHARS = 240;

    /** Cụm từ cho thấy ứng viên CHƯA có (đang chờ cấp, chưa được cấp...). */
    private static final List<String> NOT_YET = List.of(
            "dang cho", "cho cap", "chua co", "chua duoc", "chua cap", "not yet", "pending", "awaiting");

    /** Cụm từ cho thấy ứng viên đang trong quá trình (đang học, sắp thi...). */
    private static final List<String> IN_PROGRESS = List.of(
            "dang hoc", "dang theo hoc", "dang on", "sap thi", "se thi", "du kien", "in progress",
            "currently studying", "studying for", "expected");

    private CvText() {
    }

    /** Tìm câu trong CV chứa một trong các cụm từ cần trích dẫn. */
    static String findEvidence(String cvText, List<String> needles) {
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
    static boolean containsWord(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) return false;
        Pattern p = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(needle) + "(?![a-z0-9])");
        return p.matcher(haystack).find();
    }

    /** Câu trích có cho thấy ứng viên chưa có, hoặc đang trong quá trình lấy chứng chỉ không. */
    static CriterionStatus qualify(String evidence) {
        String normalized = TextNormalizer.normalize(evidence);
        for (String marker : NOT_YET) {
            if (containsWord(normalized, marker)) return CriterionStatus.NOT_MET;
        }
        for (String marker : IN_PROGRESS) {
            if (containsWord(normalized, marker)) return CriterionStatus.PARTIAL;
        }
        return CriterionStatus.MET;
    }

    /**
     * Tách yêu cầu thành các phương án ("AWS Certified, Oracle OCP" hoặc "TOEIC hoặc IELTS"),
     * giữ nguyên cách viết của HR (có dấu) để hiển thị. Phải tách trên văn bản gốc vì bước
     * chuẩn hoá biến dấu phẩy thành khoảng trắng.
     */
    static List<String> splitAlternatives(String requirement) {
        return Arrays.stream(requirement.split("(?i)[,;/]|\\bhoặc\\b|\\bhoac\\b|\\bor\\b"))
                .map(String::trim)
                .filter(s -> !TextNormalizer.normalize(s).isBlank())
                .toList();
    }

    static double parseDouble(String raw) {
        return Double.parseDouble(raw.replace(',', '.'));
    }

    /**
     * Chuẩn hoá văn bản nhưng giữ dấu thập phân kiểu Việt Nam:
     * "1,5 năm" phải thành "1.5 nam" thay vì "1 5 nam".
     */
    static String normalizeWithDecimals(String input) {
        String text = input == null ? "" : input.replaceAll("(\\d),(\\d)", "$1.$2");
        return TextNormalizer.normalize(text);
    }

    /** "3.0" → "3", "2.5" → "2.5". */
    static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
