package com.ptit.cvrecruiter.common;

/** Không tìm thấy đối tượng được yêu cầu (vị trí, hồ sơ...). Tầng web trả về HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
