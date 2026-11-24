package cvrecruiter.ai;

import cvrecruiter.model.JobPosting;

/** Chiến lược phân tích CV bằng AI (tiêu chí chấm điểm thứ hai). */
public interface AiAnalyzer {

    boolean isConfigured();

    AiResult analyze(JobPosting job, String cvText);
}
