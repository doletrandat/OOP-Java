package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillWeight;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptsTest {

    private JobPosting job() {
        JobPosting job = new JobPosting("Java Backend", "IT", "Xây dựng hệ thống microservices.",
                List.of(new Skill("Java", SkillWeight.REQUIRED)));
        job.setCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1)));
        return job;
    }

    @Test
    void promptAsksForMultipleDimensionsAndInference() {
        String prompt = AiPrompts.combinedPrompt(job(), "CV text");

        assertTrue(prompt.contains("Kỹ năng chuyên môn"));
        assertTrue(prompt.contains("Kinh nghiệm & thâm niên"));
        assertTrue(prompt.contains("Trình độ đào tạo"));
        assertTrue(prompt.contains("Ngoại ngữ"));
        assertTrue(prompt.contains("suy luận"));
        assertTrue(prompt.contains("\"criteria\""));
        assertTrue(prompt.contains("\"dimensions\""));
        assertTrue(prompt.contains("inferred"));
        // Không được yêu cầu chứng chỉ ngoại ngữ một cách máy móc
        assertTrue(prompt.contains("không bắt buộc phải có chứng chỉ"));
        // Tiêu chí của job phải nằm trong phần dữ liệu gửi AI
        assertTrue(prompt.contains("Tiếng Anh"));
        assertTrue(prompt.contains("TOEIC 600"));
    }

    @Test
    void parsesJsonWrappedInCodeFenceAndExtraText() {
        String raw = """
                ```json
                {"aiScore": 72, "summary": "Khá phù hợp",
                 "dimensions": [{"name": "Ngoại ngữ", "score": 80, "comment": "Làm việc tại Mỹ"}],
                 "skills": [{"skill": "Java", "status": "MATCHED", "evidence": "Java 4 năm"}],
                 "criteria": [{"name": "Tiếng Anh", "status": "MET", "actual": "làm việc tại Mỹ",
                               "evidence": "New York", "inferred": true}],
                 "highlights": ["Kinh nghiệm backend"], "concerns": []}
                ```
                """;

        AiAnalysis analysis = AiPrompts.parse(raw);

        assertEquals(72.0, analysis.getAiScore());
        assertFalse(analysis.getDimensions().isEmpty());
        assertEquals("Ngoại ngữ", analysis.getDimensions().get(0).getName());
        assertEquals(1, analysis.getCriteria().size());
        assertTrue(analysis.getCriteria().get(0).isInferred());
    }
}
