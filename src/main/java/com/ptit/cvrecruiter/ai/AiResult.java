package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.AiStatus;

/** Kết quả một lượt gọi AI: thành công, lỗi, hoặc chưa cấu hình. */
public class AiResult {

    private final AiStatus status;
    private final String error;
    private final AiAnalysis analysis;

    private AiResult(AiStatus status, String error, AiAnalysis analysis) {
        this.status = status;
        this.error = error == null ? "" : error;
        this.analysis = analysis;
    }

    public static AiResult ok(AiAnalysis analysis) {
        return new AiResult(AiStatus.OK, "", analysis);
    }

    public static AiResult error(String message) {
        return new AiResult(AiStatus.ERROR, message, null);
    }

    public static AiResult notConfigured() {
        return new AiResult(AiStatus.NOT_CONFIGURED, "", null);
    }

    public AiStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public AiAnalysis getAnalysis() {
        return analysis;
    }

    public boolean isSuccess() {
        return status == AiStatus.OK && analysis != null;
    }
}
