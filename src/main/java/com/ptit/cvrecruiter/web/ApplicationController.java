package com.ptit.cvrecruiter.web;

import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.model.HrDecision;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.ptit.cvrecruiter.service.CvUpload;
import com.ptit.cvrecruiter.service.JobService;
import com.ptit.cvrecruiter.web.dto.ApplicationDetailView;
import com.ptit.cvrecruiter.web.dto.ApplicationSummaryView;
import com.ptit.cvrecruiter.web.dto.ContactRequest;
import com.ptit.cvrecruiter.web.dto.DecisionRequest;

import java.util.Map;

/** Hồ sơ ứng tuyển: tải CV lên, xem, chấm lại, quyết định của HR. */
public class ApplicationController implements Controller {

    /** Phần dư cho header và các trường text của multipart. */
    private static final int MULTIPART_OVERHEAD = 2 * 1024 * 1024;

    private final SessionAuthenticator sessions;
    private final JobService jobs;
    private final ApplicationService applications;
    private final int maxUploadBytes;

    public ApplicationController(SessionAuthenticator sessions, JobService jobs,
                                 ApplicationService applications, int maxUploadBytes) {
        this.sessions = sessions;
        this.jobs = jobs;
        this.applications = applications;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public void register(Router router) {
        // HR tải CV PDF lên trực tiếp (ứng viên không nộp qua hệ thống)
        router.add("POST", "/api/admin/jobs/{id}/cv", (ex, params) -> {
            sessions.requireAuth(ex);
            String jobId = jobs.get(params.get("id")).getId();
            Http.MultipartData data = Http.readMultipart(ex, maxUploadBytes + MULTIPART_OVERHEAD);
            CvUpload upload = new CvUpload(data.fileName(), data.fileBytes(),
                    data.fields().get("name"), data.fields().get("email"), data.fields().get("phone"));
            Http.sendJson(ex, 201, ApplicationDetailView.of(applications.submit(jobId, upload)));
        });

        router.add("GET", "/api/admin/jobs/{id}/applications", (ex, params) -> {
            sessions.requireAuth(ex);
            String jobId = jobs.get(params.get("id")).getId();
            Http.sendJson(ex, 200, applications.listByJob(jobId).stream().map(ApplicationSummaryView::of).toList());
        });

        // Chấm lại toàn bộ hồ sơ sau khi JD/kỹ năng thay đổi
        router.add("POST", "/api/admin/jobs/{id}/rescore-all", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, Map.of("rescored", applications.rescoreByJob(params.get("id"))));
        });

        router.add("GET", "/api/admin/applications/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, ApplicationDetailView.of(applications.get(params.get("id"))));
        });

        router.add("POST", "/api/admin/applications/{id}/rescore", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, ApplicationDetailView.of(applications.rescore(params.get("id"))));
        });

        // Quyết định của HR (con người, không do AI)
        router.add("POST", "/api/admin/applications/{id}/decision", (ex, params) -> {
            sessions.requireAuth(ex);
            DecisionRequest body = Http.readJson(ex, DecisionRequest.class);
            Application updated = applications.decide(params.get("id"),
                    HrDecision.from(body.decision()), body.note());
            Http.sendJson(ex, 200, ApplicationDetailView.of(updated));
        });

        // HR sửa họ tên / email / số điện thoại của ứng viên
        router.add("PUT", "/api/admin/applications/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            ContactRequest body = Http.readJson(ex, ContactRequest.class);
            Application updated = applications.updateContact(params.get("id"),
                    body.name(), body.email(), body.phone());
            Http.sendJson(ex, 200, ApplicationDetailView.of(updated));
        });

        router.add("DELETE", "/api/admin/applications/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            applications.delete(params.get("id"));
            Http.sendJson(ex, 200, Map.of("ok", true));
        });
    }
}
