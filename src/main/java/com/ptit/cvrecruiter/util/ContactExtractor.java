package com.ptit.cvrecruiter.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đoán họ tên, email và số điện thoại từ văn bản CV để HR không phải gõ tay
 * khi tải nhiều CV cùng lúc. Kết quả chỉ là gợi ý; HR sửa lại được trên giao diện.
 */
public final class ContactExtractor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    /** Số Việt Nam: 0xxxxxxxxx hoặc +84xxxxxxxxx, cho phép dấu cách, chấm, gạch giữa các chữ số. */
    private static final Pattern PHONE = Pattern.compile("(?<![\\d+])(?:\\+84|84|0)(?:[\\s.-]?\\d){9}(?!\\d)");
    private static final Pattern NAME_WORD = Pattern.compile("\\p{L}+");

    /** Những dòng tiêu đề thường gặp ở đầu CV, không phải tên người. */
    private static final Set<String> HEADINGS = Set.of(
            "curriculum vitae", "cv", "resume", "résumé", "so yeu ly lich", "thong tin ca nhan",
            "personal information", "profile", "summary", "contact", "lien he", "muc tieu nghe nghiep",
            "objective", "career objective", "ho so ung vien");

    private ContactExtractor() {
    }

    public static String email(String text) {
        if (text == null) return "";
        Matcher m = EMAIL.matcher(text);
        return m.find() ? m.group() : "";
    }

    public static String phone(String text) {
        if (text == null) return "";
        Matcher m = PHONE.matcher(text);
        if (!m.find()) return "";
        String digits = m.group().replaceAll("[^\\d+]", "");
        if (digits.startsWith("84")) digits = "+" + digits;
        return digits;
    }

    /**
     * Tên thường là một trong vài dòng đầu: 2 đến 5 từ, chỉ có chữ cái.
     * Không tìm được thì lấy từ tên file (bỏ "cv", gạch dưới, gạch ngang).
     */
    public static String name(String text, String fileName) {
        if (text != null) {
            int checked = 0;
            for (String raw : text.split("\\R")) {
                String line = raw.strip();
                if (line.isEmpty()) continue;
                if (++checked > 8) break;
                String candidate = line.replaceAll("^(?i)(họ và tên|họ tên|name|full name)\\s*[:：]\\s*", "");
                if (looksLikeName(candidate)) return toTitleCase(candidate);
            }
        }
        return fromFileName(fileName);
    }

    private static boolean looksLikeName(String line) {
        if (line.length() < 4 || line.length() > 40) return false;
        if (HEADINGS.contains(TextNormalizer.normalize(line))) return false;
        String[] words = line.split("\\s+");
        if (words.length < 2 || words.length > 5) return false;
        for (String w : words) {
            if (!NAME_WORD.matcher(w).matches()) return false;
        }
        return true;
    }

    private static String fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "Ứng viên";
        String base = fileName.replaceAll("(?i)\\.pdf$", "")
                .replaceAll("[_\\-.]+", " ")
                .replaceAll("(?i)\\b(cv|resume)\\b", " ")
                .replaceAll("\\s+", " ")
                .strip();
        return base.isEmpty() ? "Ứng viên" : toTitleCase(base);
    }

    private static String toTitleCase(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.strip().split("\\s+")) {
            if (w.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            String lower = w.toLowerCase(Locale.ROOT);
            out.append(lower.substring(0, 1).toUpperCase(Locale.ROOT)).append(lower.substring(1));
        }
        return out.toString();
    }
}
