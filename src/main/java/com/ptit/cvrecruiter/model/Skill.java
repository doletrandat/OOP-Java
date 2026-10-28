package com.ptit.cvrecruiter.model;

/** Một kỹ năng mà HR yêu cầu trong vị trí tuyển dụng. */
public class Skill {

    private String name;
    private SkillWeight weight;

    public Skill() {
        // Gson cần constructor rỗng
    }

    public Skill(String name, SkillWeight weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SkillWeight getWeight() {
        return weight == null ? SkillWeight.PREFERRED : weight;
    }

    public void setWeight(SkillWeight weight) {
        this.weight = weight;
    }
}
