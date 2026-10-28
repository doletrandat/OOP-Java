package com.ptit.cvrecruiter.repository;

import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.JobStatus;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Repository cho vị trí tuyển dụng. */
public class JobRepository {

    private final JsonFileRepository<JobPosting> store;

    public JobRepository(JsonFileRepository<JobPosting> store) {
        this.store = store;
    }

    public List<JobPosting> findAll() {
        return store.findAll().stream()
                .sorted(Comparator.comparing(JobPosting::getCreatedAt).reversed())
                .toList();
    }

    public List<JobPosting> findOpenJobs() {
        return findAll().stream().filter(j -> j.getStatus() == JobStatus.OPEN).toList();
    }

    public Optional<JobPosting> findById(String id) {
        return store.findById(id);
    }

    public JobPosting save(JobPosting job) {
        return store.save(job);
    }

    public boolean delete(String id) {
        return store.delete(id);
    }

    public boolean isEmpty() {
        return store.count() == 0;
    }
}
