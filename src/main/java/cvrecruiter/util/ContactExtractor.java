package cvrecruiter.util;

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

    /** Nhãn đứng trước tên (đã bỏ dấu, chữ thường). */
    private static final Pattern NAME_LABEL = Pattern.compile("(ho va ten|ho ten|ten|full name|name|candidate)");

    /**
     * Từ gần như không bao giờ có trong tên người, dùng để loại các dòng như
     * "CV xin viec", "My resume", "Tim viec ke toan". Không đưa vào các âm tiết
     * vừa là từ thường vừa là tên (Lâm, Toàn, Nam, Tín...).
     */
    private static final Set<String> NOT_NAME_WORDS = Set.of(
            "cv", "xin", "viec", "tim", "muon", "resume", "curriculum", "vitae", "my", "want", "job",
            "hello", "gioi", "thieu", "thong", "objective", "summary", "profile", "contact", "skills",
            "experience", "education", "developer", "engineer", "manager", "executive", "nhan", "vien",
            "dev", "java", "senior", "junior", "fresher", "intern", "tester", "it", "kn");

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
                if (looksLikeName(line, 2)) return toTitleCase(line);
                // Tên và liên hệ chung một dòng (CV hai cột): "TRẦN QUỐC BẢO 0900 000 001"
                if (checked <= 3) {
                    String lead = LEADING_PART.matcher(line).replaceFirst("").strip();
                    if (!lead.equals(line) && allCapitalized(lead) && looksLikeName(lead, 2)) {
                        return toTitleCase(lead);
                    }
                }
                // Dòng có nhãn, ví dụ "Họ và tên: Nguyễn Văn An" hoặc "ten: Mai"
                int colon = line.indexOf(':');
                if (colon > 0 && NAME_LABEL.matcher(TextNormalizer.normalize(line.substring(0, colon))).matches()) {
                    String value = line.substring(colon + 1).strip();
                    if (looksLikeName(value, 1)) return toTitleCase(value);
                }
                // Một từ viết hoa ở ngay đầu CV, ví dụ "John"
                if (checked <= 3 && Character.isUpperCase(line.charAt(0)) && looksLikeName(line, 1)) {
                    return toTitleCase(line);
                }
            }
            // Tên nằm trong câu: "tôi tên là Quang", "hi, i am tom"
            String head = text.length() > 800 ? text.substring(0, 800) : text;
            Matcher m = NAME_IN_SENTENCE.matcher(head);
            while (m.find()) {
                String value = m.group(1).strip();
                boolean weakIntro = m.group().toLowerCase(Locale.ROOT).matches("(?s)(i am|i'm).*");
                if (looksLikeName(value, 1) && (!weakIntro || isPlainName(value))) return toTitleCase(value);
            }
        }
        return fromFileName(fileName);
    }

    /** Phần đuôi bắt đầu từ chữ số, dấu +, (, @, | hoặc •: thường là liên hệ. */
    private static final Pattern LEADING_PART = Pattern.compile("\\s*[\\d+(@|•·].*$");

    /** Tên ở đầu CV luôn viết hoa chữ cái đầu mỗi từ; câu văn thì không. */
    private static boolean allCapitalized(String s) {
        for (String w : s.split("\\s+")) {
            if (w.isEmpty() || !Character.isUpperCase(w.charAt(0))) return false;
        }
        return true;
    }

    private static final Pattern NAME_IN_SENTENCE = Pattern.compile(
            "(?iu)(?:tôi tên là|tên tôi là|toi ten la|ten toi la|my name is|i am|i'm)\\s+(\\p{L}+(?:\\s+\\p{L}+){0,3}?)(?=\\s*[,.;!\\n]|$)");

    /** Sau "i am" thường là tính từ ("i am good at…"), nên chỉ nhận khi không phải từ thông dụng. */
    private static final Set<String> COMMON_ENGLISH = Set.of(
            "a", "an", "the", "good", "very", "looking", "interested", "hard", "working", "fast", "learner",
            "from", "in", "at", "student", "fresher", "graduate", "currently", "available", "ready", "able", "not");

    private static boolean isPlainName(String value) {
        for (String w : TextNormalizer.normalize(value).split("\\s+")) {
            if (COMMON_ENGLISH.contains(w)) return false;
        }
        return value.split("\\s+").length <= 3;
    }

    private static boolean looksLikeName(String line, int minWords) {
        if (line.length() < 2 || line.length() > 40) return false;
        String normalized = TextNormalizer.normalize(line);
        if (HEADINGS.contains(normalized)) return false;
        String[] words = line.split("\\s+");
        if (words.length < minWords || words.length > 5) return false;
        for (String w : words) {
            if (!NAME_WORD.matcher(w).matches()) return false;
        }
        for (String w : normalized.split("\\s+")) {
            if (NOT_NAME_WORDS.contains(w)) return false;
        }
        return true;
    }

    private static String fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "Ứng viên";
        String base = fileName.replaceAll("(?i)\\.pdf$", "")
                .replaceAll("[_\\-.\\d]+", " ")
                .strip();
        StringBuilder kept = new StringBuilder();
        for (String w : base.split("\\s+")) {
            String n = TextNormalizer.normalize(w);
            if (n.isEmpty() || NOT_NAME_WORDS.contains(n) || n.equals("final") || n.equals("don")) continue;
            if (!kept.isEmpty()) kept.append(' ');
            kept.append(w);
        }
        return kept.isEmpty() ? "Ứng viên" : toTitleCase(kept.toString());
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
