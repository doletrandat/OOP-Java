package cvrecruiter.web.dto;

import cvrecruiter.model.Application;
import cvrecruiter.model.HrDecision;
import cvrecruiter.model.MatchReport;

/** Chi tiết một hồ sơ kèm báo cáo chấm điểm (không gửi toàn văn CV và đường dẫn file). */
public record ApplicationDetailView(
        String id, String jobId, String candidateName, String email, String phone,
        String cvFileName, String submittedAt, HrDecision hrDecision, String hrNote, String decidedAt,
        MatchReport report) {

    public static ApplicationDetailView of(Application app) {
        return new ApplicationDetailView(
                app.getId(), app.getJobId(), app.getCandidateName(), app.getEmail(), app.getPhone(),
                app.getCvFileName(), app.getSubmittedAt(), app.getHrDecision(), app.getHrNote(),
                app.getDecidedAt(), app.getReport());
    }
}
