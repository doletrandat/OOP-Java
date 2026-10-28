package com.ptit.cvrecruiter;

import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.repository.ApplicationRepository;
import com.ptit.cvrecruiter.repository.JobRepository;
import com.ptit.cvrecruiter.repository.JsonFileRepository;
import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.ai.AiAnalyzerFactory;
import com.ptit.cvrecruiter.matching.RuleBasedKeywordMatcher;
import com.ptit.cvrecruiter.parsing.PdfBoxCvTextExtractor;
import com.ptit.cvrecruiter.scoring.ScoringService;
import com.ptit.cvrecruiter.service.ApplicationService;
import com.ptit.cvrecruiter.service.JobService;
import com.ptit.cvrecruiter.web.AuthManager;
import com.ptit.cvrecruiter.web.WebServer;

/**
 * Điểm khởi động ứng dụng: lắp ráp các đối tượng (composition root)
 * theo kiểu tiêm phụ thuộc thủ công để giữ hướng OOP rõ ràng.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();

        JsonFileRepository<JobPosting> jobStore =
                new JsonFileRepository<>(config.dataDir.resolve("jobs.json"), JobPosting.class, JobPosting::getId);
        JsonFileRepository<Application> applicationStore =
                new JsonFileRepository<>(config.dataDir.resolve("applications.json"), Application.class, Application::getId);

        JobRepository jobRepository = new JobRepository(jobStore);
        ApplicationRepository applicationRepository = new ApplicationRepository(applicationStore);

        JobService jobService = new JobService(jobRepository);
        jobService.seedSampleJobsIfEmpty();

        ScoringService scoringService = new ScoringService(
                new RuleBasedKeywordMatcher(), AiAnalyzerFactory.create(config), config);
        ApplicationService applicationService = new ApplicationService(
                applicationRepository, jobRepository,
                new PdfBoxCvTextExtractor(), scoringService, config);

        AuthManager authManager = new AuthManager(config);

        WebServer server = new WebServer(config, jobService, applicationService, authManager);
        server.start();

        System.out.println("==============================================");
        System.out.println(" CV Recruiter dang chay tai: http://localhost:" + config.port);
        System.out.println(" Trang quan tri (HR): http://localhost:" + config.port + "/admin");
        System.out.println(" AI             : " + (config.isAiConfigured()
                ? "DA BAT - " + config.aiDisplayName()
                : "CHUA CAU HINH (dat AI_API_KEY / GEMINI_API_KEY trong config.properties)"));
        System.out.println("==============================================");
    }
}
