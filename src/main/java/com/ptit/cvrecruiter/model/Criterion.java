package com.ptit.cvrecruiter.model;

/**
 * Một tiêu chí đánh giá bổ sung do HR cấu hình cho vị trí tuyển dụng
 * (ví dụ: tối thiểu 2 năm kinh nghiệm, trình độ Đại học, TOEIC 600).
 */
public class Criterion {

    private String name = "";
    private CriterionType type = CriterionType.OTHER;
    private String requirement = "";
    private int weight = 2;   // 1 = nhẹ, 2 = quan trọng, 3 = rất quan trọng

    public Criterion() {
        // Gson cần constructor rỗng
    }

    public Criterion(String name, CriterionType type, String requirement, int weight) {
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? CriterionType.OTHER : type;
        this.requirement = requirement == null ? "" : requirement.trim();
        this.weight = Math.max(1, Math.min(3, weight));
    }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public CriterionType getType() { return type == null ? CriterionType.OTHER : type; }
    public void setType(CriterionType type) { this.type = type; }

    public String getRequirement() { return requirement == null ? "" : requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public int getWeight() { return weight < 1 || weight > 3 ? 2 : weight; }
    public void setWeight(int weight) { this.weight = Math.max(1, Math.min(3, weight)); }
}
