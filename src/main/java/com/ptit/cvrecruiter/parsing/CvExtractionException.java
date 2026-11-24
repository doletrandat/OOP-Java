package com.ptit.cvrecruiter.parsing;

import com.ptit.cvrecruiter.common.ValidationException;

/** Không trích xuất được văn bản từ CV (file hỏng, PDF dạng ảnh scan...). Trả về HTTP 400. */
public class CvExtractionException extends ValidationException {

    public CvExtractionException(String message) {
        super(message);
    }

    public CvExtractionException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
