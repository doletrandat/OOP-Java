package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.common.NotFoundException;
import com.ptit.cvrecruiter.common.ValidationException;
import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.parsing.CvRenderer;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Xem file CV gốc: PDF, thông tin file và ảnh từng trang. */
public class CvFileController implements Controller {

    private final SessionAuthenticator sessions;
    private final ApplicationService applications;
    private final CvRenderer renderer;

    public CvFileController(SessionAuthenticator sessions, ApplicationService applications, CvRenderer renderer) {
        this.sessions = sessions;
        this.applications = applications;
        this.renderer = renderer;
    }

    @Override
    public void register(Router router) {
        router.add("GET", "/api/admin/applications/{id}/cv", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            String safeName = app.getCvFileName().replaceAll("[^\\w.-]", "_");
            ex.getResponseHeaders().set("Content-Disposition", "inline; filename=\"" + safeName + "\"");
            Http.sendBytes(ex, "application/pdf", Files.readAllBytes(Path.of(app.getCvPath())));
        });

        router.add("GET", "/api/admin/applications/{id}/cv/info", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            Path file = Path.of(app.getCvPath());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pages", renderer.pageCount(file));
            body.put("fileName", app.getCvFileName());
            body.put("sizeBytes", Files.size(file));
            Http.sendJson(ex, 200, body);
        });

        // Ảnh từng trang CV (PNG, trang tính từ 1): dùng khi trình duyệt chặn xem PDF nhúng
        router.add("GET", "/api/admin/applications/{id}/cv/pages/{page}", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            byte[] png = renderer.renderPage(Path.of(app.getCvPath()), parsePage(params.get("page")));
            ex.getResponseHeaders().set("Cache-Control", "private, max-age=300");
            Http.sendBytes(ex, "image/png", png);
        });
    }

    /** Lấy hồ sơ và kiểm tra file CV còn trên đĩa. */
    private Application requireCvFile(HttpExchange ex, String applicationId) throws IOException {
        sessions.requireAuth(ex);
        Application app = applications.get(applicationId);
        if (!app.hasCvFile()) throw new NotFoundException("File CV khong con ton tai tren may chu.");
        return app;
    }

    private static int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException("So trang khong hop le.");
        }
    }
}
