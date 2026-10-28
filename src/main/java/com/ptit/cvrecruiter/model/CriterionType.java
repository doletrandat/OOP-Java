package com.ptit.cvrecruiter.model;

import java.util.Locale;

/**
 * Loại tiêu chí đánh giá bổ sung mà HR có thể cấu hình cho từng vị trí.
 * Mỗi loại có cách đối chiếu khác nhau với nội dung CV.
 */
public enum CriterionType {
    /** Số năm kinh nghiệm tối thiểu, yêu cầu nhập số (ví dụ "2"). */
    EXPERIENCE,
    /** Trình độ đào tạo tối thiểu (Trung cấp / Cao đẳng / Đại học / Thạc sĩ / Tiến sĩ). */
    EDUCATION,
    /** Chứng chỉ ngoại ngữ kèm mức tối thiểu (ví dụ "TOEIC 600", "IELTS 6.5"). */
    LANGUAGE,
    /** Chứng chỉ nghề nghiệp, khớp theo từ khoá (ví dụ "AWS Certified, Oracle OCP"). */
    CERTIFICATION,
    /** Tiêu chí tự do khác, khớp theo từ khoá. */
    OTHER;

    public static CriterionType from(String raw) {
        if (raw == null || raw.isBlank()) return OTHER;
        String v = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(v);
        } catch (IllegalArgumentException e) {
            return switch (v) {
                case "KINH NGHIEM", "KINH_NGHIEM", "EXPERIENCE_YEARS", "YEARS" -> EXPERIENCE;
                case "TRINH DO", "TRINH_DO", "EDUCATION_LEVEL", "DEGREE" -> EDUCATION;
                case "NGOAI NGU", "NGOAI_NGU", "LANGUAGE_SCORE" -> LANGUAGE;
                case "CHUNG CHI", "CHUNG_CHI", "CERTIFICATE" -> CERTIFICATION;
                default -> OTHER;
            };
        }
    }

    public String label() {
        return switch (this) {
            case EXPERIENCE -> "Số năm kinh nghiệm";
            case EDUCATION -> "Trình độ đào tạo";
            case LANGUAGE -> "Chứng chỉ ngoại ngữ";
            case CERTIFICATION -> "Chứng chỉ nghề nghiệp";
            case OTHER -> "Tiêu chí khác";
        };
    }

    /** Gợi ý định dạng giá trị yêu cầu, hiển thị trong form của HR. */
    public String requirementHint() {
        return switch (this) {
            case EXPERIENCE -> "VD: 2 (năm tối thiểu)";
            case EDUCATION -> "VD: Đại học";
            case LANGUAGE -> "VD: TOEIC 600 hoặc IELTS 6.5";
            case CERTIFICATION -> "VD: AWS Certified, Oracle OCP";
            case OTHER -> "VD: Có kinh nghiệm làm việc nhóm";
        };
    }
}
