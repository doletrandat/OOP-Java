package com.ptit.cvrecruiter.criteria;

import com.ptit.cvrecruiter.model.CriterionResult;

/**
 * Một cách đối chiếu tiêu chí với CV (Strategy). Mỗi loại tiêu chí có quy tắc riêng;
 * {@link CriteriaEvaluator} chọn quy tắc theo {@link com.ptit.cvrecruiter.model.CriterionType}.
 */
public interface CriterionRule {

    /**
     * Kết luận tiêu chí dựa trên văn bản CV.
     *
     * @param result kết quả đang trống, đã mang sẵn tên, loại và yêu cầu của tiêu chí
     * @param cvText toàn văn CV (không null)
     * @return chính {@code result} sau khi đã kết luận
     */
    CriterionResult evaluate(CriterionResult result, String cvText);
}
