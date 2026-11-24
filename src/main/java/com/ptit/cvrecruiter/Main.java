package com.ptit.cvrecruiter;

import com.ptit.cvrecruiter.ai.AiAnalyzerFactory;
import com.ptit.cvrecruiter.config.AiSettings;
import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.config.StorageSettings;
import com.ptit.cvrecruiter.matching.RuleBasedKeywordMatcher;
import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.parsing.PdfBoxCvRenderer;
import com.ptit.cvrecruiter.parsing.PdfBoxCvTextExtractor;
import com.ptit.cvrecruiter.repository.ApplicationRepository;
import com.ptit.cvrecruiter.repository.JobRepository;
import com.ptit.cvrecruiter.repository.JsonFileRepository;
import com.ptit.cvrecruiter.scoring.ScoringService;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.ptit.cvrecruiter.service.JobService;
import com.ptit.cvrecruiter.storage.LocalCvStorage;
import com.ptit.cvrecruiter.web.AiStatusController;
import com.ptit.cvrecruiter.web.ApplicationController;
import com.ptit.cvrecruiter.web.AuthController;
import com.ptit.cvrecruiter.web.AuthManager;
import com.ptit.cvrecruiter.web.CvFileController;
import com.ptit.cvrecruiter.web.JobController;
import com.ptit.cvrecruiter.web.SessionAuthenticator;
import com.ptit.cvrecruiter.web.WebServer;

import java.util.List;

/**
 * Điểm khởi động ứng dụng (composition root): lắp ráp các đối tượng bằng
 * tiêm phụ thuộc thủ công. Các lớp khác chỉ phụ thuộc vào interface được truyền vào.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        StorageSettings storage = config.storage();

        JobRepository jobRepository = new JobRepository(
                new JsonFileRepository<>(storage.jobsFile(), JobPosting.class, JobPosting::getId));
        ApplicationRepository applicationRepository = new ApplicationRepository(
                new JsonFileRepository<>(storage.applicationsFile(), Application.class, Application::getId));

        ScoringService scoring = new ScoringService(
                new RuleBasedKeywordMatcher(), AiAnalyzerFactory.create(config.ai()), config.weights());
        ApplicationService applications = new ApplicationService(
                applicationRepository, jobRepository, new PdfBoxCvTextExtractor(), scoring,
                new LocalCvStorage(storage.uploadsDir()), storage.maxUploadBytes());
        JobService jobs = new JobService(jobRepository, applications);

        SessionAuthenticator sessions = new SessionAuthenticator(new AuthManager(config.adminPassword()));
        new WebServer(config.port(), List.of(
                new AuthController(sessions),
                new AiStatusController(sessions, config.ai(), config.weights()),
                new JobController(sessions, jobs),
                new ApplicationController(sessions, jobs, applications, storage.maxUploadBytes()),
                new CvFileController(sessions, applications, new PdfBoxCvRenderer())
        )).start();

        printBanner(config.port(), config.ai());
    }

    private static void printBanner(int port, AiSettings ai) {
        System.out.println("==============================================");
        System.out.println(" CV Recruiter dang chay tai: http://localhost:" + port);
        System.out.println(" Trang quan tri (HR): http://localhost:" + port + "/admin");
        System.out.println(" AI             : " + (ai.isConfigured()
                ? "DA BAT - " + ai.displayName()
                : "CHUA CAU HINH (dat AI_API_KEY trong config.properties)"));
        System.out.println("==============================================");
    }
}
