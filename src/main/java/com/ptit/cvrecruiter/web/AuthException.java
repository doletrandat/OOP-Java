package com.ptit.cvrecruiter.web;

/** Đánh dấu yêu cầu chưa xác thực — Router sẽ trả về HTTP 401. */
public class AuthException extends RuntimeException {

    public AuthException() {
        super("Chua dang nhap hoac phien lam viec het han.");
    }
}
