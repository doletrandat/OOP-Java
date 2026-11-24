package cvrecruiter.common;

/**
 * Dữ liệu không hợp lệ (thiếu tên vị trí, email sai định dạng...). Tầng web trả về HTTP 400.
 * Kế thừa {@link IllegalArgumentException} để các nơi đang bắt lỗi đối số vẫn hoạt động.
 */
public class ValidationException extends IllegalArgumentException {

    public ValidationException(String message) {
        super(message);
    }

    /** Ném lỗi nếu điều kiện sai, giúp viết kiểm tra bất biến gọn trong đối tượng. */
    public static void require(boolean condition, String message) {
        if (!condition) throw new ValidationException(message);
    }
}
