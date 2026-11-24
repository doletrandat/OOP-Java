package com.ptit.cvrecruiter.service;

import com.ptit.cvrecruiter.util.ContactExtractor;
import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.model.Application;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.MatchReport;
import com.ptit.cvrecruiter.parsing.CvExtractionException;
import com.ptit.cvrecruiter.parsing.CvTextExtractor;
import com.ptit.cvrecruiter.repository.ApplicationRepository;
import com.ptit.cvrecruiter.repository.JobRepository;
import com.ptit.cvrecruiter.scoring.ScoringService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Nghiệp vụ xử lý hồ sơ ứng tuyển do HR tải lên:
 * nhận file PDF -> trích xuất văn bản -> chấm điểm -> lưu lại.
 */
public class ApplicationService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");

    private final ApplicationRepository repository;
    private final JobRepository jobRepository;
    private final CvTextExtractor textExtractor;
    private final ScoringService scoringService;
    private final AppConfig config;

    public ApplicationService(ApplicationRepository repository, JobRepository jobRepository,
                              CvTextExtractor textExtractor, ScoringService scoringService,
                              AppConfig config) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.textExtractor = textExtractor;
        this.scoringService = scoringService;
        this.config = config;
    }

    /**
     * HR tải CV PDF lên cho một vị trí: trích xuất văn bản, chấm điểm và lưu hồ sơ.
     */
    public Application addApplication(String jobId, String name, String email, String phone,
                                      String cvFileName, byte[] cvBytes) {
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Vi tri tuyen dung khong ton tai."));
        String cleanEmail = email == null ? "" : email.trim();
        if (!cleanEmail.isEmpty() && !EMAIL.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Email khong hop le.");
        }
        if (cvFileName == null || cvBytes == null || cvBytes.length == 0) {
            throw new IllegalArgumentException("Vui long chon file CV (PDF).");
        }
        if (cvBytes.length > config.maxUploadBytes) {
            throw new IllegalArgumentException("File CV vuot qua gioi han cho phep.");
        }

        String cvText;
        try {
            cvText = textExtractor.extract(cvFileName, cvBytes);
        } catch (CvExtractionException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // HR có thể bỏ trống thông tin liên hệ: hệ thống tự đoán từ nội dung CV
        String cleanName = name == null || name.isBlank()
                ? ContactExtractor.name(cvText, cvFileName) : name.trim();
        if (cleanEmail.isEmpty()) {
            String found = ContactExtractor.email(cvText);
            if (EMAIL.matcher(found).matches()) cleanEmail = found;
        }
        String cleanPhone = phone == null || phone.isBlank() ? ContactExtractor.phone(cvText) : phone.trim();

        Path saved = saveCvFile(jobId, cvBytes);
        Application application = new Application(jobId, cleanName, cleanEmail,
                cleanPhone, cvFileName, saved.toString(), cvText);

        try {
            MatchReport report = scoringService.score(job, cvText);
            application.setReport(report);
            return repository.save(application);
        } catch (RuntimeException e) {
            // Không để lại file mồ côi nếu bước chấm điểm thất bại
            try {
                Files.deleteIfExists(saved);
            } catch (IOException ignored) {
                // bỏ qua: lỗi dọn dẹp không nên che lỗi gốc
            }
            throw e;
        }
    }

    /** Chấm lại điểm (ví dụ sau khi HR cấu hình thêm Gemini API key). */
    public Application rescore(String applicationId) {
        Application app = repository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so."));
        JobPosting job = jobRepository.findById(app.getJobId()).orElse(null);
        if (job == null) {
            throw new IllegalArgumentException("Vi tri tuyen dung cua ho so nay khong con ton tai.");
        }
        app.setReport(scoringService.score(job, app.getCvText()));
        return repository.save(app);
    }

    public List<Application> listByJob(String jobId) {
        return repository.findByJob(jobId);
    }

    /** Chấm lại toàn bộ hồ sơ của một vị trí (dùng sau khi HR sửa JD/kỹ năng). */
    public int rescoreByJob(String jobId) {
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Vi tri tuyen dung khong ton tai."));
        int count = 0;
        for (Application app : repository.findByJob(jobId)) {
            app.setReport(scoringService.score(job, app.getCvText()));
            repository.save(app);
            count++;
        }
        return count;
    }

    /** Ghi nhận quyết định của HR; AI không tham gia vào bước này. */
    public Application decide(String applicationId, String decision, String note) {
        Application app = get(applicationId);
        app.setHrDecision(com.ptit.cvrecruiter.model.HrDecision.from(decision));
        app.setHrNote(note == null ? "" : note.trim());
        app.setDecidedAt(java.time.Instant.now().toString());
        return repository.save(app);
    }

    /** HR sửa họ tên / liên hệ (ví dụ khi hệ thống đoán tên sai). */
    public Application updateContact(String applicationId, String name, String email, String phone) {
        Application app = get(applicationId);
        if (name != null) {
            if (name.isBlank()) throw new IllegalArgumentException("Ho ten khong duoc de trong.");
            app.setCandidateName(name.trim());
        }
        if (email != null) {
            String e = email.trim();
            if (!e.isEmpty() && !EMAIL.matcher(e).matches()) {
                throw new IllegalArgumentException("Email khong hop le.");
            }
            app.setEmail(e);
        }
        if (phone != null) app.setPhone(phone.trim());
        return repository.save(app);
    }

    public Application get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so."));
    }

    public void deleteByJob(String jobId) {
        for (Application a : repository.findByJob(jobId)) {
            repository.delete(a.getId());
            deleteCvFile(a);
        }
        deleteJobUploadDir(jobId);
    }

    public void delete(String applicationId) {
        Application app = get(applicationId);
        repository.delete(applicationId);
        deleteCvFile(app);
    }

    private void deleteCvFile(Application app) {
        if (app.getCvPath() == null || app.getCvPath().isBlank()) return;
        try {
            Files.deleteIfExists(Path.of(app.getCvPath()));
        } catch (IOException ignored) {
            // File có thể đã bị xoá tay; không chặn nghiệp vụ
        }
    }

    private void deleteJobUploadDir(String jobId) {
        if (jobId == null || jobId.isBlank()) return;
        Path dir = config.uploadsDir.resolve(jobId);
        if (!Files.isDirectory(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // bỏ qua
                }
            });
        } catch (IOException ignored) {
            // bỏ qua
        }
    }

    private Path saveCvFile(String jobId, byte[] bytes) {
        try {
            Path dir = config.uploadsDir.resolve(jobId);
            Files.createDirectories(dir);
            Path file = dir.resolve(UUID.randomUUID() + ".pdf");
            Files.write(file, bytes);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Khong luu duoc file CV: " + e.getMessage(), e);
        }
    }
}
