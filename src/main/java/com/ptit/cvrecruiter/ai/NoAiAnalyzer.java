package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.JobPosting;

/**
 * Chiến lược "không dùng AI" (Null Object): giữ nguyên luồng chấm điểm quy tắc
 * khi HR không cấu hình nhà cung cấp AI nào.
 */
public class NoAiAnalyzer implements AiAnalyzer {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public AiResult analyze(JobPosting job, String cvText) {
        return AiResult.notConfigured();
    }
}
