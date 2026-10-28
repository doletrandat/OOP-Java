package com.ptit.cvrecruiter.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Chuẩn hoá văn bản để so khớp không phân biệt hoa/thường,
 * dấu tiếng Việt và ký tự phân cách.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Ví dụ: "Cơ Sở Dữ Liệu" -> "co so du lieu", ".NET Core" -> ".net core".
     * Giữ lại # + . _ để không làm hỏng các kỹ năng như C#, C++, .NET.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        s = s.replace('đ', 'd');
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '#' || c == '+' || c == '.' || c == '_' || c == ' ') {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /** Chia văn bản CV thành các câu/dòng để dùng làm bằng chứng trích dẫn. */
    public static java.util.List<String> splitSentences(String text) {
        java.util.List<String> sentences = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) return sentences;
        for (String line : text.split("\\r?\\n")) {
            for (String part : line.split("(?<=[.!?;•])\\s+")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /** Cắt câu trích dẫn cho gọn khi hiển thị. */
    public static String truncate(String s, int max) {
        if (s == null) return "";
        String clean = s.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }
}
