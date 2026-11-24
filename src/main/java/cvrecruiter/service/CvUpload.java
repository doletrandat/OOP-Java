package cvrecruiter.service;

/**
 * Một file CV do HR tải lên. Họ tên, email, số điện thoại có thể để trống:
 * hệ thống tự đọc từ nội dung CV.
 */
public record CvUpload(String fileName, byte[] bytes, String name, String email, String phone) {

    public boolean isEmpty() {
        return fileName == null || bytes == null || bytes.length == 0;
    }
}
