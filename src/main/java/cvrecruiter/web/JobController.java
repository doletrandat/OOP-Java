package cvrecruiter.web;

import cvrecruiter.model.JobPosting;
import cvrecruiter.service.JobService;
import cvrecruiter.web.dto.JobRequest;
import cvrecruiter.web.dto.JobView;

import java.util.Map;

/** Quản lý vị trí tuyển dụng. */
public class JobController implements Controller {

    private final SessionAuthenticator sessions;
    private final JobService jobs;

    public JobController(SessionAuthenticator sessions, JobService jobs) {
        this.sessions = sessions;
        this.jobs = jobs;
    }

    @Override
    public void register(Router router) {
        router.add("GET", "/api/admin/jobs", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, jobs.findAll().stream().map(this::view).toList());
        });

        router.add("GET", "/api/admin/jobs/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, view(jobs.get(params.get("id"))));
        });

        router.add("POST", "/api/admin/jobs", (ex, params) -> {
            sessions.requireAuth(ex);
            JobRequest body = Http.readJson(ex, JobRequest.class);
            Http.sendJson(ex, 201, view(jobs.create(body.toDraft())));
        });

        // Sửa một phần: chỉ gửi status để đóng/mở vị trí cũng được
        router.add("PUT", "/api/admin/jobs/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            JobRequest body = Http.readJson(ex, JobRequest.class);
            Http.sendJson(ex, 200, view(jobs.update(params.get("id"), body.toDraft())));
        });

        router.add("DELETE", "/api/admin/jobs/{id}", (ex, params) -> {
            sessions.requireAuth(ex);
            jobs.delete(params.get("id"));
            Http.sendJson(ex, 200, Map.of("ok", true));
        });
    }

    private JobView view(JobPosting job) {
        return JobView.of(job, jobs.countApplications(job.getId()));
    }
}
