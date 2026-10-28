package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.ai.CriterionVerdict;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.List;

/**
 * Kết hợp kết quả đối chiếu bằng quy tắc với nhận định của AI cho từng tiêu chí.
 *
 * <p>Nguyên tắc an toàn:
 * <ul>
 *   <li>Không bao giờ hạ một tiêu chí đã ĐẠT bằng bằng chứng rõ ràng trong CV.</li>
 *   <li>Chỉ nâng tiêu chí khi quy tắc KHÔNG tìm thấy bằng chứng nào và AI đưa ra
 *       căn cứ (trích dẫn hoặc suy luận có dữ kiện).</li>
 *   <li>Kết luận của AI luôn được đánh dấu nguồn "AI" và cờ suy luận để HR kiểm chứng.</li>
 * </ul>
 */
public class CriteriaMerger {

    public void merge(List<CriterionResult> ruleResults, List<CriterionVerdict> verdicts) {
        if (ruleResults == null || ruleResults.isEmpty() || verdicts == null) return;

        for (CriterionVerdict verdict : verdicts) {
            CriterionResult target = find(ruleResults, verdict.getName());
            if (target == null) continue;   // AI bịa tiêu chí không có trong JD → bỏ qua

            CriterionStatus aiStatus = verdict.toStatus();
            if (target.getStatus() == CriterionStatus.MET) {
                if (aiStatus == CriterionStatus.MET || aiStatus == CriterionStatus.PARTIAL) {
                    target.setSource("BOTH");
                }
                continue;
            }

            boolean ruleFoundNothing = target.getEvidence().isBlank();
            boolean aiHasBasis = !verdict.getEvidence().isBlank()
                    || verdict.isInferred()
                    || !verdict.getActual().isBlank();

            if ((aiStatus == CriterionStatus.MET || aiStatus == CriterionStatus.PARTIAL)
                    && ruleFoundNothing && aiHasBasis) {
                target.setStatus(aiStatus);
                if (target.getActual().isBlank()) target.setActual(verdict.getActual());
                if (target.getEvidence().isBlank()) target.setEvidence(verdict.getEvidence());
                target.setSource("AI");
                target.setInferred(verdict.isInferred());
                target.setNote(buildNote(verdict, true));
            } else if (target.getStatus() == CriterionStatus.UNKNOWN && !verdict.getNote().isBlank()) {
                // Quy tắc chưa đủ dữ kiện: giữ UNKNOWN nhưng bổ sung nhận định của AI
                target.setNote(buildNote(verdict, false));
            }
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

    private String buildNote(CriterionVerdict verdict, boolean upgraded) {
        String note = verdict.getNote().isBlank()
                ? (upgraded ? "AI bổ sung bằng chứng mà quy tắc chưa nhận ra." : "")
                : verdict.getNote();
        String prefix = verdict.isInferred() ? "AI suy luận: " : "AI: ";
        return prefix + note;
    }
}
