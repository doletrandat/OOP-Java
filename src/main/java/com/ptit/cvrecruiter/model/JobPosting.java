package com.ptit.cvrecruiter.model;

import com.ptit.cvrecruiter.common.ValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vị trí tuyển dụng do HR tạo. Tự bảo vệ bất biến: luôn có tên và ít nhất một kỹ năng.
 */
public class JobPosting {

    private String id;
    private String title;
    private String department;
    private String description;
    private List<Skill> skills = new ArrayList<>();
    private List<Criterion> criteria = new ArrayList<>();
    private JobStatus status = JobStatus.OPEN;
    private String createdAt = Instant.now().toString();

    private JobPosting() {
        // Gson
    }

    public JobPosting(String title, String department, String description, List<Skill> skills) {
        this(title, department, description, skills, List.of());
    }

    public JobPosting(String title, String department, String description,
                      List<Skill> skills, List<Criterion> criteria) {
        this.id = UUID.randomUUID().toString();
        edit(title, department, description);
        replaceSkills(skills);
        replaceCriteria(criteria);
    }

    /** Sửa thông tin chung; null nghĩa là giữ nguyên giá trị cũ. */
    public void edit(String title, String department, String description) {
        if (title != null) {
            ValidationException.require(!title.isBlank(), "Vui long nhap ten vi tri tuyen dung.");
            this.title = title.trim();
        }
        ValidationException.require(this.title != null, "Vui long nhap ten vi tri tuyen dung.");
        if (department != null) this.department = department.trim();
        if (description != null) this.description = description;
    }

    public void replaceSkills(List<Skill> skills) {
        ValidationException.require(skills != null && !skills.isEmpty(),
                "Vui long them it nhat mot ky nang can tim.");
        this.skills = new ArrayList<>(skills);
    }

    public void replaceCriteria(List<Criterion> criteria) {
        this.criteria = criteria == null ? new ArrayList<>() : new ArrayList<>(criteria);
    }

    public void open() {
        this.status = JobStatus.OPEN;
    }

    public void close() {
        this.status = JobStatus.CLOSED;
    }

    public boolean isOpen() {
        return getStatus() == JobStatus.OPEN;
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getDepartment() { return department == null ? "" : department; }

    public String getDescription() { return description == null ? "" : description; }

    public List<Skill> getSkills() { return skills == null ? List.of() : List.copyOf(skills); }

    /** Tiêu chí bổ sung (kinh nghiệm, trình độ, ngoại ngữ...), có thể rỗng. */
    public List<Criterion> getCriteria() { return criteria == null ? List.of() : List.copyOf(criteria); }

    public JobStatus getStatus() { return status == null ? JobStatus.OPEN : status; }

    public String getCreatedAt() { return createdAt == null ? "" : createdAt; }
}
