package com.ptit.cvrecruiter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Vị trí tuyển dụng do HR tạo. */
public class JobPosting {

    private String id;
    private String title;
    private String department;
    private String description;
    private List<Skill> skills = new ArrayList<>();
    private List<Criterion> criteria = new ArrayList<>();
    private JobStatus status = JobStatus.OPEN;
    private String createdAt = Instant.now().toString();

    public JobPosting() {
        // Gson cần constructor rỗng
    }

    public JobPosting(String title, String department, String description, List<Skill> skills) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.department = department;
        this.description = description == null ? "" : description;
        this.skills = skills == null ? new ArrayList<>() : skills;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Skill> getSkills() { return skills; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }

    /** Tiêu chí bổ sung (kinh nghiệm, trình độ, ngoại ngữ...) — có thể rỗng. */
    public List<Criterion> getCriteria() { return criteria == null ? new ArrayList<>() : criteria; }
    public void setCriteria(List<Criterion> criteria) { this.criteria = criteria; }

    public JobStatus getStatus() { return status == null ? JobStatus.OPEN : status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
