package cvrecruiter.web.dto;

import cvrecruiter.model.Criterion;
import cvrecruiter.model.JobPosting;
import cvrecruiter.model.JobStatus;
import cvrecruiter.model.Skill;

import java.util.List;

/** Vị trí tuyển dụng kèm số hồ sơ, gửi cho giao diện. */
public record JobView(String id, String title, String department, String description,
                      List<Skill> skills, List<Criterion> criteria, JobStatus status,
                      String createdAt, int applicationCount) {

    public static JobView of(JobPosting job, int applicationCount) {
        return new JobView(job.getId(), job.getTitle(), job.getDepartment(), job.getDescription(),
                job.getSkills(), job.getCriteria(), job.getStatus(), job.getCreatedAt(), applicationCount);
    }
}
