package com.ptit.cvrecruiter.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ định tuyến API gọn nhẹ: khai báo tuyến dạng "/api/admin/jobs/{id}/cv",
 * tự trích tham số đường dẫn và chuẩn hoá xử lý lỗi.
 */
public class Router {

    @FunctionalInterface
    public interface ApiHandler {
        void handle(HttpExchange ex, Map<String, String> params) throws IOException;
    }

    private record Route(String method, Pattern pattern, List<String> paramNames, ApiHandler handler) {
    }

    private final List<Route> routes = new ArrayList<>();

    public void add(String method, String pathPattern, ApiHandler handler) {
        List<String> paramNames = new ArrayList<>();
        StringBuilder regex = new StringBuilder("^");
        for (String part : pathPattern.split("/")) {
            if (part.isEmpty()) continue;
            regex.append('/');
            if (part.startsWith("{") && part.endsWith("}")) {
                paramNames.add(part.substring(1, part.length() - 1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(part));
            }
        }
        regex.append("$");
        routes.add(new Route(method, Pattern.compile(regex.toString()), paramNames, handler));
    }

    public boolean dispatch(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        boolean pathMatched = false;
        for (Route route : routes) {
            Matcher m = route.pattern().matcher(path);
            if (!m.matches()) continue;
            pathMatched = true;
            if (!route.method().equals(method)) continue;

            Map<String, String> params = new LinkedHashMap<>();
            for (int i = 0; i < route.paramNames().size(); i++) {
                params.put(route.paramNames().get(i),
                        URLDecoder.decode(m.group(i + 1), StandardCharsets.UTF_8));
            }
            try {
                route.handler().handle(ex, params);
            } catch (AuthException e) {
                Http.sendError(ex, 401, e.getMessage());
            } catch (IllegalArgumentException e) {
                Http.sendError(ex, 400, e.getMessage() == null ? "Yeu cau khong hop le." : e.getMessage());
            } catch (Exception e) {
                Http.sendError(ex, 500, "Loi he thong: " + e.getMessage());
            }
            return true;
        }
        if (pathMatched) {
            Http.sendError(ex, 405, "Phuong thuc HTTP khong duoc ho tro cho duong dan nay.");
            return true;
        }
        return false;
    }
}
