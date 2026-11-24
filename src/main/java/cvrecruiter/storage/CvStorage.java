package cvrecruiter.storage;

import java.nio.file.Path;

/** Nơi lưu file CV gốc. Tách khỏi nghiệp vụ để có thể đổi sang kho lưu trữ khác. */
public interface CvStorage {

    /** Lưu file PDF của một hồ sơ thuộc vị trí {@code jobId}; trả về đường dẫn đã lưu. */
    Path store(String jobId, byte[] pdfBytes);

    /** Xoá một file; file không còn thì bỏ qua. */
    void delete(Path file);

    /** Xoá toàn bộ file CV của một vị trí. */
    void deleteJobFolder(String jobId);
}
