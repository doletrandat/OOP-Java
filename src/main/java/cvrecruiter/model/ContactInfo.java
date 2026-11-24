package cvrecruiter.model;

import cvrecruiter.common.ValidationException;

import java.util.regex.Pattern;

/**
 * Thông tin liên hệ của ứng viên (đối tượng giá trị). Tự kiểm tra họ tên và định dạng email.
 *
 * @param name  họ tên, bắt buộc
 * @param email có thể rỗng
 * @param phone có thể rỗng
 */
public record ContactInfo(String name, String email, String phone) {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");

    public ContactInfo {
        ValidationException.require(name != null && !name.isBlank(), "Ho ten khong duoc de trong.");
        name = name.trim();
        email = email == null ? "" : email.trim();
        phone = phone == null ? "" : phone.trim();
        ValidationException.require(email.isEmpty() || isValidEmail(email), "Email khong hop le.");
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** Bản sao với các trường mới; null nghĩa là giữ nguyên. */
    public ContactInfo with(String newName, String newEmail, String newPhone) {
        return new ContactInfo(
                newName == null ? name : newName,
                newEmail == null ? email : newEmail,
                newPhone == null ? phone : newPhone);
    }
}
