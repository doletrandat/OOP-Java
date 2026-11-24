package cvrecruiter.config;

/**
 * Trọng số khi ghép các phần điểm.
 *
 * @param ai       tỉ lệ điểm AI trong điểm tổng (phần còn lại là điểm quy tắc)
 * @param criteria trong phần điểm quy tắc: tỉ lệ dành cho tiêu chí bổ sung (phần còn lại là kỹ năng)
 */
public record ScoringWeights(double ai, double criteria) {

    public static final double DEFAULT_AI = 0.4;
    public static final double DEFAULT_CRITERIA = 0.35;

    public ScoringWeights {
        ai = clamp(ai);
        criteria = clamp(criteria);
    }

    public static ScoringWeights defaults() {
        return new ScoringWeights(DEFAULT_AI, DEFAULT_CRITERIA);
    }

    /** Tỉ lệ điểm quy tắc (từ khoá + tiêu chí) trong điểm tổng. */
    public double rules() {
        return 1 - ai;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
