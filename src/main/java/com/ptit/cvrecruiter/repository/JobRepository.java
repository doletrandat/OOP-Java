package com.ptit.cvrecruiter.repository;

import com.ptit.cvrecruiter.model.JobPosting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Kho vị trí tuyển dụng, thêm các truy vấn riêng của nghiệp vụ tuyển dụng. */
public class JobRepository {

    private final Repository<JobPosting> store;

    public JobRepository(Repository<JobPosting> store) {
        this.store = store;
    }

    /** Mới tạo xếp trước. */
    public List<JobPosting> findAll() {
        return store.findAll().stream()
                .sorted(Comparator.comparing(JobPosting::getCreatedAt).reversed())
                .toList();
    }

    public List<JobPosting> findOpen() {
        return findAll().stream().filter(JobPosting::isOpen).toList();
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
}
