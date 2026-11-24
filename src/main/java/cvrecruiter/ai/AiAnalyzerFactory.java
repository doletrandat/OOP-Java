package cvrecruiter.ai;

import cvrecruiter.config.AiSettings;

/**
 * Chọn cài đặt {@link AiAnalyzer} theo cấu hình: có khoá API thì gọi DeepSeek,
 * không có thì dùng {@link NoAiAnalyzer}. Phần còn lại của ứng dụng chỉ biết interface.
 */
public final class AiAnalyzerFactory {

    private AiAnalyzerFactory() {
    }

    public static AiAnalyzer create(AiSettings settings) {
        return settings.isConfigured() ? new DeepSeekAiAnalyzer(settings) : new NoAiAnalyzer();
    }
}
