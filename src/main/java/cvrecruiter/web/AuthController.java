package cvrecruiter.web;

import cvrecruiter.web.dto.LoginRequest;

import java.util.Map;
import java.util.Optional;

/** Đăng nhập / đăng xuất trang quản trị. */
public class AuthController implements Controller {

    private final SessionAuthenticator sessions;

    public AuthController(SessionAuthenticator sessions) {
        this.sessions = sessions;
    }

    @Override
    public void register(Router router) {
        router.add("POST", "/api/auth/login", (ex, params) -> {
            LoginRequest body = Http.readJson(ex, LoginRequest.class);
            Optional<String> token = sessions.login(body.password());
            if (token.isEmpty()) {
                Http.sendError(ex, 401, "Mat khau khong dung.");
                return;
            }
            Http.sendJson(ex, 200, Map.of("token", token.get()));
        });

        router.add("POST", "/api/auth/logout", (ex, params) -> {
            sessions.logout(ex);
            Http.sendJson(ex, 200, Map.of("ok", true));
        });
    }
}
