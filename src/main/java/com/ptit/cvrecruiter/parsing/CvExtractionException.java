package com.ptit.cvrecruiter.parsing;

/** Báo lỗi khi không trích xuất được văn bản từ CV. */
public class CvExtractionException extends RuntimeException {

    public CvExtractionException(String message) {
        super(message);
    }

    public CvExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
