package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.matching.SynonymDictionary;
import com.ptit.cvrecruiter.model.CriterionResult;
import com.ptit.cvrecruiter.model.CriterionStatus;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.List;

/**
 * Đối chiếu theo từ khoá (chứng chỉ, tiêu chí tự do). Yêu cầu có thể gồm nhiều phương án;
 * chỉ cần một phương án xuất hiện trong CV mà không kèm "đang chờ cấp" / "đang học" là đạt.
 */
class KeywordRule implements CriterionRule {

    private final SynonymDictionary dictionary;

    KeywordRule(SynonymDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public CriterionResult evaluate(CriterionResult result, String cvText) {
        String normalizedCv = TextNormalizer.normalize(cvText);
        List<String> labels = CvText.splitAlternatives(result.getRequirement());

        // Nhắc tới nhưng ghi "đang chờ cấp" / "đang học" thì chưa tính là đạt; vẫn tìm tiếp
        // phương án khác, có thể ứng viên đã có một chứng chỉ khác trong danh sách.
        CriterionStatus weakest = null;
        String weakActual = "";
        String weakEvidence = "";
        for (String label : labels) {
            for (String variant : dictionary.expand(TextNormalizer.normalize(label))) {
                if (variant.isBlank() || !CvText.containsWord(normalizedCv, variant)) continue;
                String evidence = CvText.findEvidence(cvText, List.of(variant));
                CriterionStatus status = CvText.qualify(evidence);
                if (status == CriterionStatus.MET) {
                    return result.conclude(CriterionStatus.MET, label, evidence, "");
                }
                if (weakest == null || status.value() > weakest.value()) {
                    weakest = status;
                    weakActual = label;
                    weakEvidence = evidence;
                }
            }
        }
        if (weakest != null) {
            boolean inProgress = weakest == CriterionStatus.PARTIAL;
            return result.conclude(weakest,
                    weakActual + (inProgress ? " (đang học)" : " (chưa có)"),
                    weakEvidence,
                    inProgress
                            ? "CV ghi đang trong quá trình học hoặc thi, chưa có chứng chỉ."
                            : "CV ghi chưa có hoặc đang chờ cấp.");
        }
        return result.conclude(CriterionStatus.NOT_MET, "", "",
                "Không tìm thấy trong CV (đã tìm: " + TextNormalizer.truncate(result.getRequirement(), 120) + ").");
    }
}
