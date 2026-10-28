package com.ptit.cvrecruiter.parsing;

/**
 * Trừu tượng hoá việc trích xuất văn bản từ file CV.
 * Giúp sau này có thể thay/Thêm bộ đọc cho DOCX, ảnh OCR... mà không
 * phải sửa các tầng trên.
 */
public interface CvTextExtractor {

    /**
     * @param fileName tên file (dùng để nhận diện định dạng)
     * @param bytes    nội dung file
     * @return văn bản trích xuất được
     * @throws CvExtractionException nếu file không đọc được
     */
    String extract(String fileName, byte[] bytes);
}
