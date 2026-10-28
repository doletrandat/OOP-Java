package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.model.AiStatus;
import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.MatchReport;
import com.ptit.cvrecruiter.model.MatchStatus;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.ptit.cvrecruiter.service.JobService;
import com.sun.net.httpserver.HttpExchange;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** API dành cho HR/admin: quản lý job, xem và chấm lại hồ sơ. */
public class AdminController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final AuthManager auth;
    private final AppConfig config;

    public AdminController(JobService jobService, ApplicationService applicationService,
                           AuthManager auth, AppConfig config) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.auth = auth;
        this.config = config;
    }

    public void register(Router router) {
        // ---- Xác thực ----
        router.add("POST", "/api/auth/login", (ex, params) -> {
            Map<String, Object> body = Http.readJsonMap(ex);
            Optional<String> token = auth.login((String) body.get("password"));
            if (token.isEmpty()) {
                Http.sendError(ex, 401, "Mat khau khong dung.");
                return;
            }
            // Cookie để các link trực tiếp (mở tab mới, dán URL) cũng xác thực được:
            // trình duyệt tự gửi cookie, còn header X-Auth-Token thì chỉ fetch mới gửi.
            ex.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE + "=" + token.get()
                    + "; Path=/; HttpOnly; SameSite=Strict");
            Http.sendJson(ex, 200, Map.of("token", token.get()));
        });

        router.add("POST", "/api/auth/logout", (ex, params) -> {
            auth.logout(tokenOf(ex));
            ex.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
            Http.sendJson(ex, 200, Map.of("ok", true));
        });

        router.add("GET", "/api/admin/ai-status", (ex, params) -> {
            requireAuth(ex);
            Http.sendJson(ex, 200, Map.of(
                    "configured", config.isAiConfigured(),
                    "provider", config.aiProvider,
                    "displayName", config.aiDisplayName(),
                    "model", config.aiModel,
                    "aiWeight", config.aiWeight,
                    "criteriaWeight", config.criteriaWeight,
                    "keywordWeight", Math.round((1 - config.aiWeight) * 100) / 100.0));
        });

        // ---- Quản lý job ----
        router.add("GET", "/api/admin/jobs", (ex, params) -> {
            requireAuth(ex);
            List<Map<String, Object>> jobs = jobService.findAll().stream()
                    .map(this::jobWithCount)
                    .toList();
            Http.sendJson(ex, 200, jobs);
        });

        router.add("GET", "/api/admin/jobs/{id}", (ex, params) -> {
            requireAuth(ex);
            JobPosting job = jobService.findById(params.get("id"))
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay vi tri tuyen dung."));
            Http.sendJson(ex, 200, jobWithCount(job));
        });

        router.add("POST", "/api/admin/jobs", (ex, params) -> {
            requireAuth(ex);
            Http.sendJson(ex, 201, jobService.create(Http.readJsonMap(ex)));
        });

        router.add("PUT", "/api/admin/jobs/{id}", (ex, params) -> {
            requireAuth(ex);
            Optional<JobPosting> job = jobService.update(params.get("id"), Http.readJsonMap(ex));
            if (job.isEmpty()) {
                Http.sendError(ex, 404, "Khong tim thay vi tri tuyen dung.");
                return;
            }
            Http.sendJson(ex, 200, job.get());
        });

        router.add("DELETE", "/api/admin/jobs/{id}", (ex, params) -> {
            requireAuth(ex);
            applicationService.deleteByJob(params.get("id"));
            if (!jobService.delete(params.get("id"))) {
                Http.sendError(ex, 404, "Khong tim thay vi tri tuyen dung.");
                return;
            }
            Http.sendJson(ex, 200, Map.of("ok", true));
        });

        // HR tải CV PDF lên trực tiếp (ứng viên không nộp qua hệ thống)
        router.add("POST", "/api/admin/jobs/{id}/cv", (ex, params) -> {
            requireAuth(ex);
            if (jobService.findById(params.get("id")).isEmpty()) {
                Http.sendError(ex, 404, "Khong tim thay vi tri tuyen dung.");
                return;
            }
            Http.MultipartData data = Http.readMultipart(ex, config.maxUploadBytes + 2 * 1024 * 1024);
            Application app = applicationService.addApplication(
                    params.get("id"),
                    data.fields().get("name"),
                    data.fields().get("email"),
                    data.fields().get("phone"),
                    data.fileName(),
                    data.fileBytes());
            Http.sendJson(ex, 201, detail(app));
        });

        // ---- Hồ sơ ứng tuyển ----
        router.add("GET", "/api/admin/jobs/{id}/applications", (ex, params) -> {
            requireAuth(ex);
            if (jobService.findById(params.get("id")).isEmpty()) {
                Http.sendError(ex, 404, "Khong tim thay vi tri tuyen dung.");
                return;
            }
            Http.sendJson(ex, 200, applicationService.listByJob(params.get("id")).stream()
                    .map(AdminController::summary)
                    .toList());
        });

        router.add("GET", "/api/admin/applications/{id}", (ex, params) -> {
            requireAuth(ex);
            Http.sendJson(ex, 200, detail(applicationService.get(params.get("id"))));
        });

        router.add("POST", "/api/admin/applications/{id}/rescore", (ex, params) -> {
            requireAuth(ex);
            Application updated = applicationService.rescore(params.get("id"));
            Http.sendJson(ex, 200, detail(updated));
        });

        // Chấm lại toàn bộ hồ sơ sau khi JD/kỹ năng thay đổi
        router.add("POST", "/api/admin/jobs/{id}/rescore-all", (ex, params) -> {
            requireAuth(ex);
            int count = applicationService.rescoreByJob(params.get("id"));
            Http.sendJson(ex, 200, Map.of("rescored", count));
        });

        // Quyết định của HR (con người, không do AI)
        router.add("POST", "/api/admin/applications/{id}/decision", (ex, params) -> {
            requireAuth(ex);
            Map<String, Object> body = Http.readJsonMap(ex);
            Application updated = applicationService.decide(
                    params.get("id"),
                    body.get("decision") == null ? null : body.get("decision").toString(),
                    body.get("note") == null ? null : body.get("note").toString());
            Http.sendJson(ex, 200, detail(updated));
        });

        router.add("DELETE", "/api/admin/applications/{id}", (ex, params) -> {
            requireAuth(ex);
            applicationService.delete(params.get("id")); // ném lỗi nếu không tồn tại
            Http.sendJson(ex, 200, Map.of("ok", true));
        });

        router.add("GET", "/api/admin/applications/{id}/cv", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            if (app == null) return;
            Path cvPath = Path.of(app.getCvPath());
            byte[] bytes = Files.readAllBytes(cvPath);
            ex.getResponseHeaders().set("Content-Type", "application/pdf");
            ex.getResponseHeaders().set("Content-Disposition",
                    "inline; filename=\"" + app.getCvFileName().replaceAll("[^\\w.-]", "_") + "\"");
            ex.sendResponseHeaders(200, bytes.length);
            try (var os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Thông tin CV: số trang, tên file, dung lượng
        router.add("GET", "/api/admin/applications/{id}/cv/info", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            if (app == null) return;
            Path cvPath = Path.of(app.getCvPath());
            int pages;
            try (PDDocument doc = Loader.loadPDF(cvPath.toFile())) {
                pages = doc.getNumberOfPages();
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pages", pages);
            body.put("fileName", app.getCvFileName());
            body.put("sizeBytes", Files.size(cvPath));
            Http.sendJson(ex, 200, body);
        });

        // Ảnh từng trang CV (PNG): dùng khi trình duyệt chặn xem PDF nhúng.
        // Trang tính từ 1.
        router.add("GET", "/api/admin/applications/{id}/cv/pages/{page}", (ex, params) -> {
            Application app = requireCvFile(ex, params.get("id"));
            if (app == null) return;
            Path cvPath = Path.of(app.getCvPath());
            int pageNumber;
            try {
                pageNumber = Integer.parseInt(params.get("page"));
            } catch (NumberFormatException e) {
                Http.sendError(ex, 400, "So trang khong hop le.");
                return;
            }
            byte[] png;
            try (PDDocument doc = Loader.loadPDF(cvPath.toFile())) {
                if (pageNumber < 1 || pageNumber > doc.getNumberOfPages()) {
                    Http.sendError(ex, 404, "Trang khong ton tai trong CV.");
                    return;
                }
                BufferedImage image = new PDFRenderer(doc)
                        .renderImageWithDPI(pageNumber - 1, RENDER_DPI);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                png = out.toByteArray();
            }
            ex.getResponseHeaders().set("Content-Type", "image/png");
            ex.getResponseHeaders().set("Cache-Control", "private, max-age=300");
            ex.sendResponseHeaders(200, png.length);
            try (var os = ex.getResponseBody()) {
                os.write(png);
            }
        });
    }

    /** Độ phân giải khi kết xuất CV thành ảnh (đủ nét để đọc, không quá nặng). */
    private static final float RENDER_DPI = 110f;

    /**
     * Lấy hồ sơ và kiểm tra file CV còn trên đĩa. Trả null (đã gửi lỗi 404) nếu thiếu.
     */
    private Application requireCvFile(HttpExchange ex, String applicationId) {
        requireAuth(ex);
        Application app = applicationService.get(applicationId);
        if (!Files.exists(Path.of(app.getCvPath()))) {
            Http.sendError(ex, 404, "File CV khong con ton tai tren may chu.");
            return null;
        }
        return app;
    }

    /** Tên cookie lưu token đăng nhập (HttpOnly, SameSite=Strict). */
    private static final String AUTH_COOKIE = "cvr_admin_token";

    /** Lỗi xác thực được Router bắt và trả về 401. */
    private void requireAuth(HttpExchange ex) {
        String token = tokenOf(ex);
        if (!auth.isValid(token)) {
            throw new AuthException();
        }
        // Phiên cũ chỉ có token trong localStorage → đồng bộ sang cookie để link trực tiếp dùng được
        if (!token.equals(cookieToken(ex))) {
            ex.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE + "=" + token
                    + "; Path=/; HttpOnly; SameSite=Strict");
        }
    }

    /**
     * Lấy token từ header X-Auth-Token (fetch của JS), Authorization: Bearer,
     * hoặc cookie — nhờ cookie mà mở link trực tiếp/mở tab mới vẫn xác thực được.
     */
    private static String tokenOf(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("X-Auth-Token");
        if (header != null && !header.isBlank()) return header;

        String authorization = ex.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return cookieToken(ex);
    }

    private static String cookieToken(HttpExchange ex) {
        for (String cookie : ex.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
            for (String part : cookie.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(AUTH_COOKIE)) return kv[1];
            }
        }
        return null;
    }

    private Map<String, Object> jobWithCount(JobPosting job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("department", job.getDepartment());
        map.put("description", job.getDescription());
        map.put("skills", job.getSkills());
        map.put("criteria", job.getCriteria());
        map.put("status", job.getStatus());
        map.put("createdAt", job.getCreatedAt());
        map.put("applicationCount", applicationService.listByJob(job.getId()).size());
        return map;
    }

    private static Map<String, Object> summary(Application app) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", app.getId());
        map.put("candidateName", app.getCandidateName());
        map.put("email", app.getEmail());
        map.put("phone", app.getPhone());
        map.put("submittedAt", app.getSubmittedAt());
        map.put("hrDecision", app.getHrDecision());
        map.put("hrNote", app.getHrNote());
        map.put("decidedAt", app.getDecidedAt());
        MatchReport r = app.getReport();
        if (r != null) {
            map.put("finalScore", r.getFinalScore());
            map.put("keywordScore", r.getKeywordScore());
            map.put("aiScore", r.getAiScore());
            map.put("aiStatus", r.getAiStatus());
            map.put("matchedCount", r.countByStatus(MatchStatus.MATCHED));
            map.put("totalSkills", r.getSkillMatches().size());
            map.put("criteriaScore", r.getCriteriaScore());
            map.put("criteriaMet", r.countCriteriaMet());
            map.put("criteriaTotal", r.getCriteriaResults().size());
        } else {
            map.put("finalScore", null);
        }
        return map;
    }

    private static Map<String, Object> detail(Application app) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", app.getId());
        map.put("jobId", app.getJobId());
        map.put("candidateName", app.getCandidateName());
        map.put("email", app.getEmail());
        map.put("phone", app.getPhone());
        map.put("cvFileName", app.getCvFileName());
        map.put("submittedAt", app.getSubmittedAt());
        map.put("hrDecision", app.getHrDecision());
        map.put("hrNote", app.getHrNote());
        map.put("decidedAt", app.getDecidedAt());
        map.put("report", app.getReport());
        return map;
    }
}
