package com.ptit.cvrecruiter.config;

/**
 * Cấu hình gọi DeepSeek (bất biến).
 *
 * @param apiKey          khoá API DeepSeek; để trống thì tắt AI
 * @param model           tên model, ví dụ deepseek-chat
 * @param timeoutSeconds  thời gian chờ tối đa cho một lượt gọi
 */
public record AiSettings(String apiKey, String model, int timeoutSeconds) {

    public static final String DEFAULT_MODEL = "deepseek-chat";

    public AiSettings {
        apiKey = apiKey == null ? "" : apiKey.trim();
        model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        timeoutSeconds = Math.max(10, timeoutSeconds);
    }

    /** Cấu hình tắt AI, dùng cho kiểm thử và khi người dùng không đặt khoá. */
    public static AiSettings disabled() {
        return new AiSettings("", DEFAULT_MODEL, 60);
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /** Nhãn hiển thị trên giao diện, ví dụ "DeepSeek · deepseek-chat". */
    public String displayName() {
        return isConfigured() ? "DeepSeek · " + model : "Chưa cấu hình AI";
    }
}
