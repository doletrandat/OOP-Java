package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.ptit.cvrecruiter.service.JobService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Web server nhúng (dùng com.sun.net.httpserver có sẵn của JDK):
 * - /api/**  : REST API nội bộ cho HR do Router điều phối
 * - /        : chuyển hướng về trang quản trị /admin
 * - còn lại  : giao diện web tĩnh trong /resources/static
 */
public class WebServer {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".js", "application/javascript; charset=utf-8"),
            Map.entry(".json", "application/json; charset=utf-8"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".woff2", "font/woff2"));

    private final AppConfig config;
    private final Router router = new Router();
    private HttpServer server;

    public WebServer(AppConfig config, JobService jobService,
                     ApplicationService applicationService, AuthManager authManager) {
        this.config = config;
        new AdminController(jobService, applicationService, authManager, config).register(router);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith("/api/")) {
                if (!router.dispatch(ex)) {
                    Http.sendError(ex, 404, "Khong tim thay API: " + path);
                }
            } else {
                serveStatic(ex, path);
            }
        } finally {
            ex.close();
        }
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        // Hệ thống chỉ dành cho HR: trang gốc luôn đưa về khu vực quản trị
        if (path.equals("/") || path.equals("/index.html")) {
            ex.getResponseHeaders().set("Location", "/admin");
            ex.sendResponseHeaders(302, -1);
            return;
        }
        String filePath = switch (path) {
            case "/admin", "/admin/" -> "/admin.html";
            default -> path;
        };
        String resource = "/static" + filePath;

        try (InputStream in = WebServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                byte[] notFound = "404 - Khong tim thay trang".getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }
            byte[] bytes = in.readAllBytes();
            String type = CONTENT_TYPES.getOrDefault(extensionOf(filePath), "application/octet-stream");
            ex.getResponseHeaders().set("Content-Type", type);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String extensionOf(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot < 0 ? "" : filePath.substring(dot).toLowerCase();
    }
}
