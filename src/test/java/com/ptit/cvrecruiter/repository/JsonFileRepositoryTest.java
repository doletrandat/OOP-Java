package com.ptit.cvrecruiter.repository;

import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillWeight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JsonFileRepositoryTest {

    @Test
    void reloadsRealModelInstancesNotLinkedTreeMaps(@TempDir Path dir) {
        Path file = dir.resolve("jobs.json");

        JsonFileRepository<JobPosting> first =
                new JsonFileRepository<>(file, JobPosting.class, JobPosting::getId);
        first.save(new JobPosting("Backend Java", "IT", "JD",
                List.of(new Skill("Java", SkillWeight.REQUIRED))));

        // Mô phỏng lần khởi động server tiếp theo: đọc lại từ file
        JsonFileRepository<JobPosting> reloaded =
                new JsonFileRepository<>(file, JobPosting.class, JobPosting::getId);

        JobPosting job = reloaded.findAll().get(0);
        assertInstanceOf(JobPosting.class, job);
        assertEquals("Backend Java", job.getTitle());
        assertEquals("Java", job.getSkills().get(0).getName());
    }
}
