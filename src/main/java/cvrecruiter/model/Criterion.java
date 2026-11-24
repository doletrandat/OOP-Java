package cvrecruiter.model;

/**
 * Một tiêu chí đánh giá bổ sung do HR cấu hình cho vị trí tuyển dụng
 * (ví dụ: tối thiểu 2 năm kinh nghiệm, trình độ Đại học, TOEIC 600). Bất biến.
 */
public class Criterion {

    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 3;
    public static final int DEFAULT_WEIGHT = 2;

    private String name = "";
    private CriterionType type = CriterionType.OTHER;
    private String requirement = "";
    private int weight = DEFAULT_WEIGHT;   // 1 = nhẹ, 2 = quan trọng, 3 = rất quan trọng

    private Criterion() {
        // Gson
    }

    public Criterion(String name, CriterionType type, String requirement, int weight) {
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? CriterionType.OTHER : type;
        this.requirement = requirement == null ? "" : requirement.trim();
        this.weight = clampWeight(weight);
    }

    public String getName() { return name == null ? "" : name; }

    public CriterionType getType() { return type == null ? CriterionType.OTHER : type; }

    public String getRequirement() { return requirement == null ? "" : requirement; }

    public int getWeight() { return clampWeight(weight); }

    public boolean hasRequirement() {
        return !getRequirement().isBlank();
    }

    private static int clampWeight(int weight) {
        return weight < MIN_WEIGHT || weight > MAX_WEIGHT ? DEFAULT_WEIGHT : weight;
    }
}
