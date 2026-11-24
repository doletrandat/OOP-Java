package com.ptit.cvrecruiter.model;

import com.ptit.cvrecruiter.common.ValidationException;

/** Một kỹ năng HR cần tìm trong CV (đối tượng giá trị, bất biến). */
public class Skill {

    private String name;
    private SkillWeight weight;

    private Skill() {
        // Gson
    }

    public Skill(String name, SkillWeight weight) {
        ValidationException.require(name != null && !name.isBlank(), "Ten ky nang khong duoc de trong.");
        this.name = name.trim();
        this.weight = weight == null ? SkillWeight.PREFERRED : weight;
    }

    public String getName() {
        return name;
    }

    public SkillWeight getWeight() {
        return weight == null ? SkillWeight.PREFERRED : weight;
    }

    public boolean isRequired() {
        return getWeight() == SkillWeight.REQUIRED;
    }
}
