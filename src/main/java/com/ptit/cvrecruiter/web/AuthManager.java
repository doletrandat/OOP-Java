package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.config.AppConfig;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xác thực đơn giản cho trang quản trị: một mật khẩu chung,
 * đăng nhập thành công nhận token nhớ trong bộ nhớ server.
 */
public class AuthManager {

    private final String adminPassword;
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();

    public AuthManager(AppConfig config) {
        this.adminPassword = config.adminPassword;
    }

    public Optional<String> login(String password) {
        if (password != null && password.equals(adminPassword)) {
            String token = UUID.randomUUID().toString();
            tokens.add(token);
            return Optional.of(token);
        }
        return Optional.empty();
    }

    public boolean isValid(String token) {
        return token != null && !token.isBlank() && tokens.contains(token);
    }

    public void logout(String token) {
        tokens.remove(token);
    }
}
