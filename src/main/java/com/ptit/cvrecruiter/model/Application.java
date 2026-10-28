package com.ptit.cvrecruiter.model;

import java.time.Instant;
import java.util.UUID;

/** Một hồ sơ ứng tuyển: ứng viên nộp CV PDF cho một vị trí. */
public class Application {

    private String id;
    private String jobId;
    private String candidateName;
    private String email;
    private String phone;
    private String cvFileName;    // tên file gốc ứng viên upload
    private String cvPath;        // đường dẫn file PDF đã lưu trên server
    private String cvText;        // văn bản trích xuất từ PDF
    private String submittedAt = Instant.now().toString();
    private MatchReport report;
    // Quyết định của HR (tham khảo điểm AI nhưng con người tự quyết định)
    private HrDecision hrDecision = HrDecision.NEW;
    private String hrNote = "";
    private String decidedAt = "";

    public Application() {
        // Gson cần constructor rỗng
    }

    public Application(String jobId, String candidateName, String email, String phone,
                       String cvFileName, String cvPath, String cvText) {
        this.id = UUID.randomUUID().toString();
        this.jobId = jobId;
        this.candidateName = candidateName;
        this.email = email;
        this.phone = phone;
        this.cvFileName = cvFileName;
        this.cvPath = cvPath;
        this.cvText = cvText == null ? "" : cvText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCvFileName() { return cvFileName; }
    public void setCvFileName(String cvFileName) { this.cvFileName = cvFileName; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getCvText() { return cvText == null ? "" : cvText; }
    public void setCvText(String cvText) { this.cvText = cvText; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }

    public MatchReport getReport() { return report; }
    public void setReport(MatchReport report) { this.report = report; }

    public HrDecision getHrDecision() { return hrDecision == null ? HrDecision.NEW : hrDecision; }
    public void setHrDecision(HrDecision hrDecision) {
        this.hrDecision = hrDecision == null ? HrDecision.NEW : hrDecision;
    }

    public String getHrNote() { return hrNote == null ? "" : hrNote; }
    public void setHrNote(String hrNote) { this.hrNote = hrNote; }

    public String getDecidedAt() { return decidedAt == null ? "" : decidedAt; }
    public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }

    /** Ứng viên nộp CV cho vị trí nào (trao đổi với JobRepository để lấy thông tin hiển thị). */
    public boolean belongsToJob(String jobId) {
        return this.jobId != null && this.jobId.equals(jobId);
    }
}
