package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.config.AppConfig;

/**
 * Factory chọn chiến lược AI theo cấu hình. Nhờ đó phần còn lại của ứng dụng
 * chỉ làm việc với interface {@link AiAnalyzer}, đổi nhà cung cấp không cần sửa code.
 */
public final class AiAnalyzerFactory {

    private AiAnalyzerFactory() {
    }

    public static AiAnalyzer create(AppConfig config) {
        if (!config.isAiConfigured()) return new NoAiAnalyzer();
        return switch (config.aiProvider) {
            case "gemini" -> new GeminiAiAnalyzer(config);
            default -> new OpenAiCompatibleAiAnalyzer(config);
        };
    }
}
