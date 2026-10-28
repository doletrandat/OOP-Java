package com.ptit.cvrecruiter.matching;

import com.ptit.cvrecruiter.model.JobPosting;

import java.util.Map;

/**
 * Chiến lược so khớp từ khoá: với mỗi kỹ năng trong JD,
 * tìm bằng chứng xuất hiện trong văn bản CV.
 */
public interface KeywordMatcher {

    /**
     * @return map từ tên kỹ năng (theo JD) -> kết quả khớp;
     *         kỹ năng không xuất hiện sẽ không có entry.
     */
    Map<String, KeywordHit> match(JobPosting job, String cvText);
}
