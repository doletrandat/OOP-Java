package cvrecruiter.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Một hồ sơ ứng tuyển: CV PDF của một ứng viên cho một vị trí.
 * Các thay đổi đi qua phương thức nghiệp vụ (chấm điểm, ra quyết định, sửa liên hệ)
 * thay vì setter, để hồ sơ luôn ở trạng thái hợp lệ.
 */
public class Application {

    private String id;
    private String jobId;
    private String candidateName;
    private String email;
    private String phone;
    private String cvFileName;    // tên file gốc
    private String cvPath;        // đường dẫn file PDF đã lưu trên server
    private String cvText;        // văn bản trích xuất từ PDF
    private String submittedAt;
    private MatchReport report;
    // Quyết định của HR: AI không bao giờ tự đặt giá trị này
    private HrDecision hrDecision = HrDecision.NEW;
    private String hrNote = "";
    private String decidedAt = "";

    private Application() {
        // Gson
    }

    public Application(String jobId, ContactInfo contact, String cvFileName, String cvPath, String cvText) {
        this.id = UUID.randomUUID().toString();
        this.jobId = jobId;
        applyContact(contact);
        this.cvFileName = cvFileName;
        this.cvPath = cvPath;
        this.cvText = cvText == null ? "" : cvText;
        this.submittedAt = Instant.now().toString();
    }

    /** Gắn kết quả chấm điểm mới nhất. */
    public void attachReport(MatchReport report) {
        this.report = report;
    }

    /** HR ra quyết định (mời phỏng vấn, từ chối, hoặc đặt lại) kèm ghi chú nội bộ. */
    public void decide(HrDecision decision, String note) {
        this.hrDecision = decision == null ? HrDecision.NEW : decision;
        this.hrNote = note == null ? "" : note.trim();
        this.decidedAt = Instant.now().toString();
    }

    /** Sửa họ tên, email, số điện thoại; null nghĩa là giữ nguyên. */
    public void updateContact(String name, String email, String phone) {
        applyContact(contact().with(name, email, phone));
    }

    public ContactInfo contact() {
        return new ContactInfo(candidateName, email, phone);
    }

    public boolean belongsToJob(String jobId) {
        return this.jobId != null && this.jobId.equals(jobId);
    }

    public boolean hasCvFile() {
        return cvPath != null && !cvPath.isBlank();
    }

    /** Điểm dùng để xếp hạng; hồ sơ chưa chấm xếp cuối. */
    public double rankingScore() {
        return report == null ? -1 : report.getFinalScore();
    }

    private void applyContact(ContactInfo contact) {
        this.candidateName = contact.name();
        this.email = contact.email();
        this.phone = contact.phone();
    }

    public String getId() { return id; }

    public String getJobId() { return jobId; }

    public String getCandidateName() { return candidateName; }

    public String getEmail() { return email == null ? "" : email; }

    public String getPhone() { return phone == null ? "" : phone; }

    public String getCvFileName() { return cvFileName; }

    public String getCvPath() { return cvPath; }

    public String getCvText() { return cvText == null ? "" : cvText; }

    public String getSubmittedAt() { return submittedAt; }

    public MatchReport getReport() { return report; }

    public HrDecision getHrDecision() { return hrDecision == null ? HrDecision.NEW : hrDecision; }

    public String getHrNote() { return hrNote == null ? "" : hrNote; }

    public String getDecidedAt() { return decidedAt == null ? "" : decidedAt; }
}
