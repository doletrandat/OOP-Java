package com.ptit.cvrecruiter.parsing;

import java.nio.file.Path;

/**
 * Kết xuất file CV để xem trên trình duyệt. Trình duyệt nào cũng xem được ảnh PNG,
 * kể cả khi chặn PDF nhúng.
 */
public interface CvRenderer {

    int pageCount(Path cvFile);

    /**
     * @param pageNumber trang tính từ 1
     * @return ảnh PNG của trang
     * @throws IllegalArgumentException nếu trang không tồn tại
     */
    byte[] renderPage(Path cvFile, int pageNumber);
}
