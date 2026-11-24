package com.ptit.cvrecruiter.web;

import com.sun.net.httpserver.HttpExchange;

import java.util.Optional;

/**
 * Gắn {@link AuthManager} với HTTP: token đăng nhập được giao diện gửi trong header
 * {@code X-Auth-Token} ở mỗi request.
 */
public class SessionAuthenticator {

    private static final String TOKEN_HEADER = "X-Auth-Token";

    private final AuthManager auth;

    public SessionAuthenticator(AuthManager auth) {
        this.auth = auth;
    }

    public Optional<String> login(String password) {
        return auth.login(password);
    }

    public void logout(HttpExchange ex) {
        auth.logout(tokenOf(ex));
    }

    /** Ném {@link AuthException} (Router trả 401) nếu request chưa đăng nhập. */
    public void requireAuth(HttpExchange ex) {
        if (!auth.isValid(tokenOf(ex))) throw new AuthException();
    }

    private static String tokenOf(HttpExchange ex) {
        return ex.getRequestHeaders().getFirst(TOKEN_HEADER);
    }
}
