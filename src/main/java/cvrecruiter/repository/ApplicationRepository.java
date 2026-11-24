package cvrecruiter.repository;

import cvrecruiter.model.Application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Kho hồ sơ ứng tuyển. */
public class ApplicationRepository {

    private final Repository<Application> store;

    public ApplicationRepository(Repository<Application> store) {
        this.store = store;
    }

    /** Hồ sơ của một vị trí, điểm cao xếp trước. */
    public List<Application> findByJob(String jobId) {
        return store.findAll().stream()
                .filter(a -> a.belongsToJob(jobId))
                .sorted(Comparator.comparingDouble(Application::rankingScore).reversed())
                .toList();
    }

    public int countByJob(String jobId) {
        return (int) store.findAll().stream().filter(a -> a.belongsToJob(jobId)).count();
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
