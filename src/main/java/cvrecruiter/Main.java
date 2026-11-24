package cvrecruiter;

import cvrecruiter.ai.AiAnalyzerFactory;
import cvrecruiter.config.AiSettings;
import cvrecruiter.config.AppConfig;
import cvrecruiter.config.StorageSettings;
import cvrecruiter.matching.RuleBasedKeywordMatcher;
import cvrecruiter.model.Application;
import cvrecruiter.model.JobPosting;
import cvrecruiter.parsing.PdfBoxCvRenderer;
import cvrecruiter.parsing.PdfBoxCvTextExtractor;
import cvrecruiter.repository.ApplicationRepository;
import cvrecruiter.repository.JobRepository;
import cvrecruiter.repository.JsonFileRepository;
import cvrecruiter.scoring.ScoringService;
import cvrecruiter.service.ApplicationService;
import cvrecruiter.service.JobService;
import cvrecruiter.storage.LocalCvStorage;
import cvrecruiter.web.AiStatusController;
import cvrecruiter.web.ApplicationController;
import cvrecruiter.web.AuthController;
import cvrecruiter.web.AuthManager;
import cvrecruiter.web.CvFileController;
import cvrecruiter.web.JobController;
import cvrecruiter.web.SessionAuthenticator;
import cvrecruiter.web.WebServer;

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
