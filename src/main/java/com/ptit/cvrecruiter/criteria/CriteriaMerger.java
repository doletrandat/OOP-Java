package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.ai.CriterionVerdict;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.List;

/**
 * Kết hợp kết quả đối chiếu bằng quy tắc với nhận định của AI cho từng tiêu chí.
 *
 * <p>Quy tắc đọc chữ rất chính xác nhưng không hiểu ngữ cảnh: nó thấy "3 năm" trong
 * "3 năm kinh nghiệm (tính từ năm 2 đại học)", thấy "chứng chỉ hành nghề" trong "đang chờ cấp
 * chứng chỉ hành nghề", và không cộng được các mốc "2021 đến nay". AI đọc được ngữ cảnh đó.
 * Vì vậy:
 * <ul>
 *   <li>Kinh nghiệm, trình độ, chứng chỉ, tiêu chí khác: khi AI có kết luận kèm căn cứ (giá trị
 *       đọc được, câu trích hoặc giải thích), kết luận của AI được dùng, kể cả khi thấp hơn quy tắc.
 *       Ghi chú nói rõ quy tắc đã đọc được gì để HR tự kiểm tra.</li>
 *   <li>Ngoại ngữ: khi quy tắc đã đọc được điểm chứng chỉ (ví dụ "TOEIC 780") thì giữ kết quả so
 *       sánh số của quy tắc. Khi quy tắc không thấy chứng chỉ, AI được nâng lên nếu có căn cứ
 *       (ví dụ làm việc lâu năm ở nước nói tiếng Anh).</li>
 *   <li>AI không có căn cứ hoặc trả về UNKNOWN: giữ kết quả quy tắc.</li>
 * </ul>
 * Mọi kết luận lấy từ AI được đánh dấu nguồn {@code AI} (hoặc {@code BOTH} khi hai bên khớp nhau).
 */
public class CriteriaMerger {

    public void merge(List<CriterionResult> ruleResults, List<CriterionVerdict> verdicts) {
        if (ruleResults == null || ruleResults.isEmpty() || verdicts == null) return;

        for (CriterionVerdict verdict : verdicts) {
            CriterionResult target = find(ruleResults, verdict.getName());
            if (target == null) continue;   // AI bịa tiêu chí không có trong JD → bỏ qua

            CriterionStatus aiStatus = verdict.toStatus();
            CriterionStatus ruleStatus = target.getStatus();
            boolean aiHasBasis = !verdict.getEvidence().isBlank()
                    || !verdict.getActual().isBlank()
                    || !verdict.getNote().isBlank();
            if (aiStatus == CriterionStatus.UNKNOWN || !aiHasBasis) {
                if (ruleStatus == CriterionStatus.UNKNOWN && !verdict.getNote().isBlank()) {
                    target.annotate(prefix(verdict) + verdict.getNote());
                }
                continue;
            }

            if (aiStatus == ruleStatus) {
                target.confirmByAi(verdict.getNote().isBlank() ? null : prefix(verdict) + verdict.getNote());
                continue;
            }

            boolean ruleReadCertificateScore = target.getType() == CriterionType.LANGUAGE
                    && !target.getEvidence().isBlank();
            if (ruleReadCertificateScore) {
                continue;   // so sánh điểm TOEIC/IELTS của quy tắc là chính xác, giữ nguyên
            }
            boolean downgrade = aiStatus.value() < ruleStatus.value();
            if (downgrade && target.getType() == CriterionType.LANGUAGE) {
                continue;   // ngoại ngữ: AI chỉ được bổ sung, không hạ kết quả quy tắc
            }
            if (downgrade && verdict.getActual().isBlank() && verdict.getEvidence().isBlank()) {
                continue;   // hạ kết quả phải nêu được giá trị thực tế hoặc câu trích, không chỉ nhận xét
            }

            target.overrideByAi(aiStatus, verdict.getActual(), verdict.getEvidence(),
                    buildNote(verdict, ruleStatus, target.getActual()), verdict.isInferred());
        }
    }

    private CriterionResult find(List<CriterionResult> results, String name) {
        String wanted = TextNormalizer.normalize(name);
        if (wanted.isBlank()) return null;

        for (CriterionResult r : results) {
            if (TextNormalizer.normalize(r.getName()).equals(wanted)) return r;
        }
        for (CriterionResult r : results) {
            String current = TextNormalizer.normalize(r.getName());
            if (!current.isBlank() && (current.contains(wanted) || wanted.contains(current))) return r;
        }
        return null;
    }

    private String prefix(CriterionVerdict verdict) {
        return verdict.isInferred() ? "AI suy luận: " : "AI: ";
    }

    private String buildNote(CriterionVerdict verdict, CriterionStatus ruleStatus, String ruleSaw) {
        String note = verdict.getNote().isBlank() ? "AI đọc lại ngữ cảnh trong CV." : verdict.getNote();
        StringBuilder sb = new StringBuilder(prefix(verdict)).append(note);
        if (ruleStatus != CriterionStatus.UNKNOWN && !ruleSaw.isBlank()) {
            sb.append(" (Quy tắc từ khoá đọc được \"").append(ruleSaw).append("\" và kết luận ")
              .append(ruleStatus.label().toLowerCase()).append(".)");
        }
        return sb.toString();
    }
}
