package com.ptit.cvrecruiter.repository;

import com.ptit.cvrecruiter.model.Application;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Repository cho hồ sơ ứng tuyển. */
public class ApplicationRepository {

    private final JsonFileRepository<Application> store;

    public ApplicationRepository(JsonFileRepository<Application> store) {
        this.store = store;
    }

    public List<Application> findAll() {
        return store.findAll();
    }

    public List<Application> findByJob(String jobId) {
        return findAll().stream()
                .filter(a -> a.belongsToJob(jobId))
                .sorted(Comparator.comparing((Application a) ->
                        a.getReport() == null ? -1.0 : a.getReport().getFinalScore()).reversed())
                .toList();
    }

    public Optional<Application> findById(String id) {
        return store.findById(id);
    }

    public Application save(Application application) {
        return store.save(application);
    }

    public boolean delete(String id) {
        return store.delete(id);
    }
}
