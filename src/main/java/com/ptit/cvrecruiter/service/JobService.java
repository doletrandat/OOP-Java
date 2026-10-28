package com.ptit.cvrecruiter.service;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.JobStatus;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillWeight;
import com.ptit.cvrecruiter.repository.JobRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Nghiệp vụ quản lý vị trí tuyển dụng. */
public class JobService {

    private final JobRepository repository;

    public JobService(JobRepository repository) {
        this.repository = repository;
    }

    public List<JobPosting> findAll() {
        return repository.findAll();
    }

    public List<JobPosting> findOpen() {
        return repository.findOpenJobs();
    }

    public Optional<JobPosting> findById(String id) {
        return repository.findById(id);
    }

    public JobPosting create(Map<String, Object> payload) {
        JobPosting job = new JobPosting(
                str(payload.get("title")),
                str(payload.get("department")),
                str(payload.get("description")),
                parseSkills(payload.get("skills")));
        job.setCriteria(parseCriteria(payload.get("criteria")));
        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new IllegalArgumentException("Vui long nhap ten vi tri tuyen dung.");
        }
        if (job.getSkills().isEmpty()) {
            throw new IllegalArgumentException("Vui long them it nhat mot ky nang can tim.");
        }
        return repository.save(job);
    }

    public Optional<JobPosting> update(String id, Map<String, Object> payload) {
        return repository.findById(id).map(job -> {
            if (payload.containsKey("title")) job.setTitle(str(payload.get("title")));
            if (payload.containsKey("department")) job.setDepartment(str(payload.get("department")));
            if (payload.containsKey("description")) job.setDescription(str(payload.get("description")));
            if (payload.containsKey("skills")) job.setSkills(parseSkills(payload.get("skills")));
            if (payload.containsKey("criteria")) job.setCriteria(parseCriteria(payload.get("criteria")));
            if (payload.containsKey("status")) {
                job.setStatus("CLOSED".equalsIgnoreCase(str(payload.get("status")))
                        ? JobStatus.CLOSED : JobStatus.OPEN);
            }
            return repository.save(job);
        });
    }

    public boolean delete(String id) {
        return repository.delete(id);
    }

    @SuppressWarnings("unchecked")
    private List<Skill> parseSkills(Object raw) {
        List<Skill> skills = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    String name = str(m.get("name"));
                    if (name == null || name.isBlank()) continue;
                    skills.add(new Skill(name.trim(), SkillWeight.from(str(m.get("weight")))));
                } else if (o instanceof String s && !s.isBlank()) {
                    skills.add(new Skill(s.trim(), SkillWeight.PREFERRED));
                }
            }
        }
        return skills;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    /** Đọc danh sách tiêu chí bổ sung do HR gửi lên. */
    @SuppressWarnings("unchecked")
    private List<Criterion> parseCriteria(Object raw) {
        List<Criterion> criteria = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                String name = str(m.get("name"));
                if (name == null || name.isBlank()) continue;
                criteria.add(new Criterion(
                        name.trim(),
                        CriterionType.from(str(m.get("type"))),
                        str(m.get("requirement")),
                        intOf(m.get("weight"), 2)));
            }
        }
        return criteria;
    }

    private int intOf(Object raw, int def) {
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (Exception e) {
            return def;
        }
    }

    /** Tạo dữ liệu mẫu lần đầu chạy để không bị trống trang. */
    public void seedSampleJobsIfEmpty() {
        if (!repository.isEmpty()) return;

        JobPosting javaJob = new JobPosting(
                "Lập trình viên Java Backend (Fresher/Junior)",
                "Phòng Kỹ thuật Phần mềm",
                """
                Phát triển và bảo trì các hệ thống backend bằng Java.
                Yêu cầu công việc:
                - Nắm vững Java và lập trình hướng đối tượng.
                - Có kiến thức về cơ sở dữ liệu, viết được câu lệnh SQL.
                - Xây dựng REST API cho các dịch vụ web.
                - Sử dụng Git trong làm việc nhóm.
                - Ưu tiên ứng viên biết Spring Boot, Docker.
                - Tiếng Anh đọc hiểu tài liệu kỹ thuật.""",
                List.of(new Skill("Java", SkillWeight.REQUIRED),
                        new Skill("OOP", SkillWeight.REQUIRED),
                        new Skill("Database", SkillWeight.REQUIRED),
                        new Skill("SQL", SkillWeight.REQUIRED),
                        new Skill("REST API", SkillWeight.REQUIRED),
                        new Skill("Git", SkillWeight.REQUIRED),
                        new Skill("Spring Boot", SkillWeight.PREFERRED),
                        new Skill("Docker", SkillWeight.PREFERRED),
                        new Skill("English", SkillWeight.PREFERRED)));
        javaJob.setCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "1", 2),
                new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2),
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1)));
        repository.save(javaJob);

        JobPosting dataJob = new JobPosting(
                "Chuyên viên Phân tích Dữ liệu",
                "Phòng Dữ liệu & Business Intelligence",
                """
                Phân tích dữ liệu kinh doanh, xây dựng báo cáo và dashboard phục vụ ra quyết định.
                Yêu cầu công việc:
                - Thành thạo SQL truy vấn dữ liệu.
                - Biết sử dụng Python hoặc Excel để phân tích dữ liệu.
                - Biết dùng Power BI hoặc công cụ trực quan hoá tương đương.
                - Hiểu biết thống kê cơ bản.
                - Kỹ năng giao tiếp, làm việc nhóm tốt.""",
                List.of(new Skill("SQL", SkillWeight.REQUIRED),
                        new Skill("Data analysis", SkillWeight.REQUIRED),
                        new Skill("Python", SkillWeight.PREFERRED),
                        new Skill("Excel", SkillWeight.REQUIRED),
                        new Skill("Power BI", SkillWeight.PREFERRED),
                        new Skill("Statistics", SkillWeight.PREFERRED),
                        new Skill("Communication", SkillWeight.REQUIRED),
                        new Skill("Teamwork", SkillWeight.PREFERRED)));
        dataJob.setCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                new Criterion("Trình độ", CriterionType.EDUCATION, "Đại học", 2),
                new Criterion("Ngoại ngữ", CriterionType.LANGUAGE, "IELTS 6.0", 1)));
        repository.save(dataJob);
    }
}
