package cvrecruiter.repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ tổng quát theo khoá chuỗi. Tầng nghiệp vụ chỉ phụ thuộc vào interface này,
 * nên có thể đổi từ file JSON sang cơ sở dữ liệu mà không sửa service.
 */
public interface Repository<T> {

    List<T> findAll();

    Optional<T> findById(String id);

    /** Thêm mới hoặc ghi đè đối tượng có cùng khoá. */
    T save(T entity);

    /** @return true nếu có đối tượng bị xoá */
    boolean delete(String id);
}
