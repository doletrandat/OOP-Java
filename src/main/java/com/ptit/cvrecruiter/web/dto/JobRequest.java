package com.ptit.cvrecruiter.web.dto;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.model.JobStatus;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillWeight;
import com.ptit.cvrecruiter.service.JobDraft;

import java.util.List;
import java.util.Objects;

/**
 * Body JSON khi tạo hoặc sửa vị trí. Trường không gửi lên là null (giữ nguyên khi sửa).
 * Kỹ năng và tiêu chí không có tên bị bỏ qua.
 */
public record JobRequest(String title, String department, String description,
                         List<SkillInput> skills, List<CriterionInput> criteria, String status) {

    public record SkillInput(String name, String weight) {
        Skill toSkill() {
            return name == null || name.isBlank() ? null : new Skill(name.trim(), SkillWeight.from(weight));
        }
    }

    public record CriterionInput(String name, String type, String requirement, Integer weight) {
        Criterion toCriterion() {
            if (name == null || name.isBlank()) return null;
            return new Criterion(name.trim(), CriterionType.from(type), requirement,
                    weight == null ? Criterion.DEFAULT_WEIGHT : weight);
        }
    }

    public JobDraft toDraft() {
        return new JobDraft(title, department, description,
                skills == null ? null : skills.stream().filter(Objects::nonNull)
                        .map(SkillInput::toSkill).filter(Objects::nonNull).toList(),
                criteria == null ? null : criteria.stream().filter(Objects::nonNull)
                        .map(CriterionInput::toCriterion).filter(Objects::nonNull).toList(),
                status == null ? null : ("CLOSED".equalsIgnoreCase(status) ? JobStatus.CLOSED : JobStatus.OPEN));
    }
}
