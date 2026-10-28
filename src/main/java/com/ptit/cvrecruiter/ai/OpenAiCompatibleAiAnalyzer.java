package com.ptit.cvrecruiter.ai;

import com.google.gson.JsonArray;
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
 * Gọi mọi API theo chuẩn OpenAI Chat Completions: DeepSeek, Groq, OpenRouter,
 * OpenAI, Ollama/LM Studio, vLLM... Chỉ cần đổi AI_BASE_URL + AI_MODEL.
 *
 * <p>Thiết kế theo Strategy: cùng interface {@link AiAnalyzer} với Gemini nên
 * {@code ScoringService} không cần biết đang dùng nhà cung cấp nào.
 */
public class OpenAiCompatibleAiAnalyzer implements AiAnalyzer {

    private final AppConfig config;
    private final HttpClient http;

    public OpenAiCompatibleAiAnalyzer(AppConfig config) {
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

        String system = AiPrompts.systemPrompt();
        String user = AiPrompts.userPrompt(job, cvText);
        String lastError = null;

        for (String model : modelsToTry()) {
            try {
                String text = call(model, system, user, true);
                return AiResult.ok(AiPrompts.parse(text));
            } catch (UnsupportedParameterException e) {
                // Một số endpoint không hỗ trợ response_format → thử lại không kèm tham số đó
                try {
                    String text = call(model, system, user, false);
                    return AiResult.ok(AiPrompts.parse(text));
                } catch (RetryableModelException retry) {
                    lastError = retry.getMessage();
                } catch (Exception failure) {
                    return AiResult.error(message(failure));
                }
            } catch (RetryableModelException e) {
                // Model quá tải / bị gỡ / hết hạn mức tạm thời → thử model dự phòng
                lastError = e.getMessage();
            } catch (Exception e) {
                return AiResult.error(message(e));
            }
        }
        return AiResult.error(lastError == null ? "Khong goi duoc API AI." : lastError);
    }

    private List<String> modelsToTry() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        if (config.aiModel != null && !config.aiModel.isBlank()) models.add(config.aiModel.trim());
        models.addAll(config.aiFallbackModels);
        return List.copyOf(models);
    }

    private String call(String model, String system, String user, boolean jsonMode) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.1);
        body.addProperty("max_tokens", 8192);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        messages.add(message("system", system));
        messages.add(message("user", user));
        body.add("messages", messages);

        if (jsonMode) {
            JsonObject format = new JsonObject();
            format.addProperty("type", "json_object");
            body.add("response_format", format);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl()))
                .timeout(Duration.ofSeconds(Math.max(10, config.aiTimeoutSeconds)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (config.aiApiKey != null && !config.aiApiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + config.aiApiKey);
        }

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();

        if (status != 200) {
            String message = config.aiProvider + " API (" + model + ") tra ve HTTP " + status
                    + ": " + shorten(response.body(), 400);
            if (status == 400 && jsonMode && mentionsResponseFormat(response.body())) {
                throw new UnsupportedParameterException(message);
            }
            if (status == 404 || status == 408 || status == 409 || status == 429 || status >= 500) {
                throw new RetryableModelException(message);
            }
            throw new IllegalStateException(message);
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI khong tra ve ket qua (choices rong).");
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        String content = null;
        if (choice.has("message") && choice.getAsJsonObject("message").has("content")) {
            content = choice.getAsJsonObject("message").get("content").getAsString();
        }
        if (content == null || content.isBlank()) {
            String reason = choice.has("finish_reason") ? choice.get("finish_reason").getAsString() : "?";
            throw new IllegalStateException("AI tra ve phan hoi rong (finish_reason: " + reason + ").");
        }
        return content;
    }

    private JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    /** Ghép base URL với đường dẫn chat completions, chấp nhận cả base đã có /v1. */
    private String chatCompletionsUrl() {
        String base = config.aiBaseUrl == null ? "" : config.aiBaseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/chat/completions")) return base;
        return base + "/chat/completions";
    }

    private boolean mentionsResponseFormat(String body) {
        if (body == null) return false;
        String lower = body.toLowerCase();
        return lower.contains("response_format") || lower.contains("json_object");
    }

    private String message(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Lỗi có thể thử lại bằng model khác. */
    private static final class RetryableModelException extends Exception {
        RetryableModelException(String message) {
            super(message);
        }
    }

    /** Endpoint không hỗ trợ response_format=json_object. */
    private static final class UnsupportedParameterException extends Exception {
        UnsupportedParameterException(String message) {
            super(message);
        }
    }
}
