package com.ptit.cvrecruiter.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ptit.cvrecruiter.config.AiSettings;
import com.ptit.cvrecruiter.model.JobPosting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Đánh giá CV bằng DeepSeek qua API Chat Completions. */
public class DeepSeekAiAnalyzer implements AiAnalyzer {

    private static final URI ENDPOINT = URI.create("https://api.deepseek.com/chat/completions");

    private final AiSettings settings;
    private final HttpClient http;

    public DeepSeekAiAnalyzer(AiSettings settings) {
        this.settings = settings;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    @Override
    public AiResult analyze(JobPosting job, String cvText) {
        if (!isConfigured()) return AiResult.notConfigured();
        try {
            return AiResult.ok(AiPrompts.parse(complete(job, cvText)));
        } catch (Exception e) {
            return AiResult.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /** Gửi prompt và trả về văn bản JSON mà model sinh ra. */
    private String complete(JobPosting job, String cvText) throws Exception {
        String body = requestBody(AiPrompts.systemPrompt(), AiPrompts.userPrompt(job, cvText)).toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("DeepSeek API (" + settings.model() + ") tra ve HTTP "
                    + response.statusCode() + ": " + shorten(response.body(), 400));
        }
        return readContent(response.body());
    }

    private JsonObject requestBody(String system, String user) {
        JsonArray messages = new JsonArray();
        messages.add(message("system", system));
        messages.add(message("user", user));

        JsonObject format = new JsonObject();
        format.addProperty("type", "json_object");

        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model());
        body.addProperty("temperature", 0.1);
        body.addProperty("max_tokens", 8192);
        body.addProperty("stream", false);
        body.add("messages", messages);
        body.add("response_format", format);
        return body;
    }

    private static JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static String readContent(String responseBody) {
        JsonArray choices = JsonParser.parseString(responseBody).getAsJsonObject().getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI khong tra ve ket qua (choices rong).");
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        String content = choice.has("message") && choice.getAsJsonObject("message").has("content")
                ? choice.getAsJsonObject("message").get("content").getAsString()
                : null;
        if (content == null || content.isBlank()) {
            String reason = choice.has("finish_reason") ? choice.get("finish_reason").getAsString() : "?";
            throw new IllegalStateException("AI tra ve phan hoi rong (finish_reason: " + reason + ").");
        }
        return content;
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
