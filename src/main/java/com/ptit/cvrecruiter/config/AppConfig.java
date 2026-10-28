package com.ptit.cvrecruiter.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Cấu hình ứng dụng: đọc từ file config.properties (nếu có) rồi được
 * ghi đè bởi biến môi trường. Giữ mặc định hợp lý để chạy được ngay.
 *
 * <p>Phần AI hỗ trợ nhiều nhà cung cấp:
 * <ul>
 *   <li>{@code gemini} — Google Gemini (API riêng của Google)</li>
 *   <li>{@code deepseek} — DeepSeek (tương thích OpenAI)</li>
 *   <li>{@code openai} — OpenAI hoặc bất kỳ endpoint tương thích OpenAI
 *       (Groq, OpenRouter, Ollama, LM Studio, vLLM...)</li>
 *   <li>{@code none} — tắt hoàn toàn tiêu chí AI</li>
 * </ul>
 */
public final class AppConfig {

    public final int port;
    /** gemini | deepseek | openai | none */
    public final String aiProvider;
    public final String aiApiKey;
    /** URL gốc của endpoint tương thích OpenAI, ví dụ https://api.deepseek.com/v1 */
    public final String aiBaseUrl;
    public final String aiModel;
    /** Các model thử tiếp theo khi model chính quá tải / bị gỡ. */
    public final List<String> aiFallbackModels;
    public final int aiTimeoutSeconds;

    public final String adminPassword;
    public final double aiWeight;          // trọng số tiêu chí AI (còn lại là quy tắc)
    public final double criteriaWeight;    // trong phần quy tắc: tỉ lệ điểm tiêu chí bổ sung
    public final Path dataDir;             // nơi lưu jobs.json / applications.json
    public final Path uploadsDir;          // nơi lưu file CV PDF
    public final int maxUploadBytes;

    private AppConfig(int port, String aiProvider, String aiApiKey, String aiBaseUrl,
                      String aiModel, List<String> aiFallbackModels, int aiTimeoutSeconds,
                      String adminPassword, double aiWeight, double criteriaWeight,
                      Path dataDir, Path uploadsDir, int maxUploadBytes) {
        this.port = port;
        this.aiProvider = aiProvider;
        this.aiApiKey = aiApiKey;
        this.aiBaseUrl = aiBaseUrl;
        this.aiModel = aiModel;
        this.aiFallbackModels = aiFallbackModels;
        this.aiTimeoutSeconds = aiTimeoutSeconds;
        this.adminPassword = adminPassword;
        this.aiWeight = aiWeight;
        this.criteriaWeight = criteriaWeight;
        this.dataDir = dataDir;
        this.uploadsDir = uploadsDir;
        this.maxUploadBytes = maxUploadBytes;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        Path cfgFile = Path.of("config.properties");
        if (Files.exists(cfgFile)) {
            try (InputStream in = Files.newInputStream(cfgFile)) {
                props.load(in);
            } catch (IOException ignored) {
                // Nếu đọc lỗi thì cứ dùng mặc định + env
            }
        }

        int port = intOf(envOr(props, "PORT", "8080"), 8080);

        // ---- Cấu hình AI ----
        // Không hard-code khoá API trong mã nguồn: đặt trong config.properties (đã gitignore)
        // hoặc biến môi trường.
        String geminiKey = envOr(props, "GEMINI_API_KEY", "");
        String rawKey = envOr(props, "AI_API_KEY", "");
        String provider = envOr(props, "AI_PROVIDER", "").trim().toLowerCase(Locale.ROOT);
        if (provider.isEmpty()) {
            // Tự suy ra để tương thích cấu hình cũ (chỉ có GEMINI_API_KEY)
            if (!geminiKey.isBlank()) provider = "gemini";
            else if (!rawKey.isBlank()) provider = "openai";
            else provider = "none";
        }
        provider = normalizeProvider(provider);

        String apiKey;
        String baseUrl;
        String defaultModel;
        switch (provider) {
            case "gemini" -> {
                apiKey = rawKey.isBlank() ? geminiKey : rawKey;
                baseUrl = "";
                defaultModel = "gemini-3.5-flash";
            }
            case "none" -> {
                apiKey = "";
                baseUrl = "";
                defaultModel = "";
            }
            default -> {
                apiKey = rawKey.isBlank() ? geminiKey : rawKey;
                baseUrl = envOr(props, "AI_BASE_URL", defaultBaseUrl(provider));
                defaultModel = defaultModel(provider);
            }
        }

        String model = provider.equals("none")
                ? ""
                : envOr(props, "AI_MODEL", defaultModel);
        List<String> fallbacks = parseList(envOr(props, "AI_FALLBACK_MODELS",
                provider.equals("gemini") ? "gemini-3.6-flash" : ""));
        int aiTimeout = intOf(envOr(props, "AI_TIMEOUT_SECONDS", "60"), 60);

        String password = envOr(props, "ADMIN_PASSWORD", "admin123");
        double aiWeight = Math.max(0, Math.min(1, doubleOf(envOr(props, "AI_WEIGHT", "0.4"), 0.4)));
        double criteriaWeight = Math.max(0, Math.min(1,
                doubleOf(envOr(props, "CRITERIA_WEIGHT", "0.35"), 0.35)));
        Path dataDir = Path.of(envOr(props, "DATA_DIR", "data"));
        Path uploadsDir = Path.of(envOr(props, "UPLOADS_DIR", "uploads"));
        int maxUpload = intOf(envOr(props, "MAX_UPLOAD_MB", "15"), 15) * 1024 * 1024;

        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Khong tao duoc thu muc du lieu: " + e.getMessage(), e);
        }

        return new AppConfig(port, provider, apiKey, baseUrl, model, fallbacks, aiTimeout,
                password, aiWeight, criteriaWeight, dataDir, uploadsDir, maxUpload);
    }

    public boolean isAiConfigured() {
        if ("none".equals(aiProvider)) return false;
        if (aiApiKey != null && !aiApiKey.isBlank()) return true;
        // Ollama / LM Studio chạy nội bộ có thể không cần khoá
        String url = aiBaseUrl == null ? "" : aiBaseUrl;
        return url.contains("localhost") || url.contains("127.0.0.1");
    }

    /** Nhãn hiển thị trên giao diện, ví dụ "DeepSeek · deepseek-chat". */
    public String aiDisplayName() {
        if (!isAiConfigured()) return "Chưa cấu hình AI";
        String name = switch (aiProvider) {
            case "gemini" -> "Gemini";
            case "deepseek" -> "DeepSeek";
            case "openai" -> "OpenAI-compatible";
            default -> aiProvider;
        };
        return name + " · " + aiModel;
    }

    private static String normalizeProvider(String p) {
        return switch (p) {
            case "gemini", "google" -> "gemini";
            case "deepseek", "deep-seek" -> "deepseek";
            case "openai", "openai-compatible", "groq", "openrouter", "ollama", "custom", "local" -> "openai";
            case "none", "off", "disabled" -> "none";
            default -> "openai";
        };
    }

    private static String defaultBaseUrl(String provider) {
        return provider.equals("deepseek")
                ? "https://api.deepseek.com/v1"
                : "https://api.openai.com/v1";
    }

    private static String defaultModel(String provider) {
        return provider.equals("deepseek") ? "deepseek-chat" : "gpt-4o-mini";
    }

    private static List<String> parseList(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split(",")) {
            String v = part.trim();
            if (!v.isEmpty() && !result.contains(v)) result.add(v);
        }
        return List.copyOf(result);
    }

    private static String envOr(Properties props, String key, String def) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env.trim();
        String p = props.getProperty(key);
        if (p != null && !p.isBlank()) return p.trim();
        return def;
    }

    private static int intOf(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static double doubleOf(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}
