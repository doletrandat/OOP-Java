package com.ptit.cvrecruiter.service;

import com.ptit.cvrecruiter.common.NotFoundException;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.JobStatus;
import com.ptit.cvrecruiter.repository.JobRepository;

import java.util.List;

/** Nghiệp vụ quản lý vị trí tuyển dụng. */
public class JobService {

    private final JobRepository repository;
    private final ApplicationService applications;

    public JobService(JobRepository repository, ApplicationService applications) {
        this.repository = repository;
        this.applications = applications;
    }

    public List<JobPosting> findAll() {
        return repository.findAll();
    }

    public JobPosting get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay vi tri tuyen dung."));
    }

    public JobPosting create(JobDraft draft) {
        JobPosting job = new JobPosting(draft.title(), draft.department(), draft.description(),
                draft.skills(), draft.criteria());
        return repository.save(job);
    }

    /** Sửa vị trí; chỉ những phần có trong {@code draft} mới thay đổi. */
    public JobPosting update(String id, JobDraft draft) {
        JobPosting job = get(id);
        job.edit(draft.title(), draft.department(), draft.description());
        if (draft.skills() != null) job.replaceSkills(draft.skills());
        if (draft.criteria() != null) job.replaceCriteria(draft.criteria());
        if (draft.status() == JobStatus.OPEN) job.open();
        if (draft.status() == JobStatus.CLOSED) job.close();
        return repository.save(job);
    }

    /** Xoá vị trí cùng toàn bộ hồ sơ và file CV của nó. */
    public void delete(String id) {
        get(id);
        applications.deleteByJob(id);
        repository.delete(id);
    }

    public int countApplications(String jobId) {
        return applications.countByJob(jobId);
    }
}
