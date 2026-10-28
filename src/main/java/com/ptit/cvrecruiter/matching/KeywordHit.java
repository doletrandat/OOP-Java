package com.ptit.cvrecruiter.matching;

/**
 * Kết quả khớp từ khoá cho một kỹ năng: số lần xuất hiện
 * và câu trích dẫn đầu tiên làm bằng chứng.
 */
public record KeywordHit(int occurrences, String evidence, String matchedVariant) {
}
