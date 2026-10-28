package com.ptit.cvrecruiter.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ptit.cvrecruiter.config.AppConfig;
import com.ptit.cvrecruiter.model.JobPosting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Triển khai AiAnalyzer bằng Google Gemini API (generateContent).
 * Yêu cầu Gemini trả về JSON chặt theo schema để hệ thống chấm điểm.
 */
public class GeminiAiAnalyzer implements AiAnalyzer {

    private static final String API_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AppConfig config;
    private final HttpClient http;

    public GeminiAiAnalyzer(AppConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return config.isAiConfigured();
    }

    @Override
    public AiResult analyze(JobPosting job, String cvText) {
        if (!isConfigured()) return AiResult.notConfigured();
        String prompt = AiPrompts.combinedPrompt(job, cvText);
        String lastError = null;
        for (String model : modelsToTry()) {
            try {
                String responseText = callGemini(model, prompt);
                return AiResult.ok(AiPrompts.parse(responseText));
            } catch (RetryableModelException e) {
                // Model quá tải / bị gỡ: thử model dự phòng tiếp theo
                lastError = e.getMessage();
            } catch (Exception e) {
                return AiResult.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
        return AiResult.error(lastError == null ? "Khong goi duoc Gemini API." : lastError);
    }

    private List<String> modelsToTry() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        if (config.aiModel != null && !config.aiModel.isBlank()) models.add(config.aiModel.trim());
        models.addAll(config.aiFallbackModels);
        return List.copyOf(models);
    }

    private String callGemini(String model, String prompt) throws Exception {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.1);
        genConfig.addProperty("responseMimeType", "application/json");
        genConfig.addProperty("maxOutputTokens", 8192);
        body.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + model + ":generateContent"))
                .timeout(Duration.ofSeconds(Math.max(10, config.aiTimeoutSeconds)))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", config.aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String message = "Gemini API (" + model + ") tra ve HTTP " + response.statusCode()
                    + ": " + shorten(response.body(), 300);
            if (response.statusCode() == 404 || response.statusCode() == 408 || response.statusCode() == 429
                    || response.statusCode() >= 500) {
                throw new RetryableModelException(message);
            }
            throw new IllegalStateException(message);
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini khong tra ve ket qua (co the bi chan boi an toan noi dung).");
        }
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        StringBuilder text = new StringBuilder();
        if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
            for (JsonElement el : candidate.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject p = el.getAsJsonObject();
                if (p.has("text")) text.append(p.get("text").getAsString());
            }
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("Gemini tra ve phan hoi trong (finishReason: "
                    + (candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "?") + ").");
        }
        return text.toString();
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Cho phép service biết model đang dùng để hiển thị. */
    public String getModelName() {
        return config.aiModel;
    }

    /** Lỗi có thể thử lại bằng model khác (quá tải, model bị gỡ, hết hạn mức tạm thời). */
    private static final class RetryableModelException extends Exception {
        RetryableModelException(String message) {
            super(message);
        }
    }
}
