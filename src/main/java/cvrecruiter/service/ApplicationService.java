package cvrecruiter.service;

import cvrecruiter.common.NotFoundException;
import cvrecruiter.common.ValidationException;
import cvrecruiter.model.Application;
import cvrecruiter.model.ContactInfo;
import cvrecruiter.model.HrDecision;
import cvrecruiter.model.JobPosting;
import cvrecruiter.parsing.CvTextExtractor;
import cvrecruiter.repository.ApplicationRepository;
import cvrecruiter.repository.JobRepository;
import cvrecruiter.scoring.ScoringService;
import cvrecruiter.storage.CvStorage;
import cvrecruiter.util.ContactExtractor;

import java.nio.file.Path;
import java.util.List;

/**
 * Nghiệp vụ hồ sơ ứng tuyển: nhận file PDF, trích xuất văn bản, chấm điểm, lưu lại;
 * ghi nhận quyết định của HR.
 */
public class ApplicationService {

    private final ApplicationRepository repository;
    private final JobRepository jobs;
    private final CvTextExtractor textExtractor;
    private final ScoringService scoring;
    private final CvStorage storage;
    private final int maxUploadBytes;

    public ApplicationService(ApplicationRepository repository, JobRepository jobs, CvTextExtractor textExtractor,
                              ScoringService scoring, CvStorage storage, int maxUploadBytes) {
        this.repository = repository;
        this.jobs = jobs;
        this.textExtractor = textExtractor;
        this.scoring = scoring;
        this.storage = storage;
        this.maxUploadBytes = maxUploadBytes;
    }

    /** HR tải CV lên cho một vị trí: trích xuất văn bản, đoán thông tin liên hệ, chấm điểm và lưu. */
    public Application submit(String jobId, CvUpload upload) {
        JobPosting job = job(jobId);
        ValidationException.require(!upload.isEmpty(), "Vui long chon file CV (PDF).");
        ValidationException.require(upload.bytes().length <= maxUploadBytes, "File CV vuot qua gioi han cho phep.");
        ValidationException.require(upload.email() == null || upload.email().isBlank()
                || ContactInfo.isValidEmail(upload.email()), "Email khong hop le.");

        String cvText = textExtractor.extract(upload.fileName(), upload.bytes());
        ContactInfo contact = detectContact(upload, cvText);

        Path saved = storage.store(jobId, upload.bytes());
        try {
            Application application = new Application(jobId, contact, upload.fileName(), saved.toString(), cvText);
            application.attachReport(scoring.score(job, cvText));
            return repository.save(application);
        } catch (RuntimeException e) {
            storage.delete(saved); // không để lại file mồ côi khi chấm điểm thất bại
            throw e;
        }
    }

    public List<Application> listByJob(String jobId) {
        return repository.findByJob(jobId);
    }

    public int countByJob(String jobId) {
        return repository.countByJob(jobId);
    }

    public Application get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Khong tim thay ho so."));
    }

    /** Chấm lại một hồ sơ, ví dụ sau khi sửa yêu cầu của vị trí hoặc bật AI. */
    public Application rescore(String applicationId) {
        Application app = get(applicationId);
        app.attachReport(scoring.score(job(app.getJobId()), app.getCvText()));
        return repository.save(app);
    }

    /** Chấm lại toàn bộ hồ sơ của một vị trí. */
    public int rescoreByJob(String jobId) {
        JobPosting job = job(jobId);
        List<Application> apps = repository.findByJob(jobId);
        for (Application app : apps) {
            app.attachReport(scoring.score(job, app.getCvText()));
            repository.save(app);
        }
        return apps.size();
    }

    /** Ghi nhận quyết định của HR; AI không tham gia bước này. */
    public Application decide(String applicationId, HrDecision decision, String note) {
        Application app = get(applicationId);
        app.decide(decision, note);
        return repository.save(app);
    }

    /** HR sửa họ tên, email, số điện thoại (ví dụ khi hệ thống đoán tên sai). */
    public Application updateContact(String applicationId, String name, String email, String phone) {
        Application app = get(applicationId);
        app.updateContact(name, email, phone);
        return repository.save(app);
    }

    public void delete(String applicationId) {
        Application app = get(applicationId);
        repository.delete(applicationId);
        if (app.hasCvFile()) storage.delete(Path.of(app.getCvPath()));
    }

    public void deleteByJob(String jobId) {
        for (Application app : repository.findByJob(jobId)) {
            repository.delete(app.getId());
        }
        storage.deleteJobFolder(jobId);
    }

    /** Thông tin HR nhập được ưu tiên; ô nào trống thì đọc từ CV. */
    private ContactInfo detectContact(CvUpload upload, String cvText) {
        String name = isBlank(upload.name()) ? ContactExtractor.name(cvText, upload.fileName()) : upload.name();
        String email = upload.email();
        if (isBlank(email)) {
            String found = ContactExtractor.email(cvText);
            email = ContactInfo.isValidEmail(found) ? found : "";
        }
        String phone = isBlank(upload.phone()) ? ContactExtractor.phone(cvText) : upload.phone();
        return new ContactInfo(name, email, phone);
    }

    private JobPosting job(String jobId) {
        return jobs.findById(jobId).orElseThrow(() -> new NotFoundException("Vi tri tuyen dung khong ton tai."));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
