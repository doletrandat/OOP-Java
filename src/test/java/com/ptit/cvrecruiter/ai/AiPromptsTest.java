package com.ptit.cvrecruiter.ai;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.CriterionType;
import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.model.SkillWeight;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptsTest {

    private JobPosting job() {
        JobPosting job = new JobPosting("Java Backend", "IT", "Xây dựng hệ thống microservices.",
                List.of(new Skill("Java", SkillWeight.REQUIRED)));
        job.replaceCriteria(List.of(
                new Criterion("Kinh nghiệm", CriterionType.EXPERIENCE, "2", 2),
                new Criterion("Tiếng Anh", CriterionType.LANGUAGE, "TOEIC 600", 1)));
        return job;
    }

    @Test
    void systemPromptCoversScoringRules() {
        String prompt = AiPrompts.systemPrompt();

        // Năm nhóm điểm với tên cố định để giao diện hiển thị ổn định
        for (String dim : List.of("Kỹ năng chuyên môn", "Kinh nghiệm & thâm niên", "Trình độ đào tạo",
                "Ngoại ngữ", "Chứng chỉ & bổ sung")) {
            assertTrue(prompt.contains("\"" + dim + "\""), dim);
        }
        // Không chỉ dành cho ngành IT
        assertFalse(prompt.contains("ứng viên IT"));
        // Kỹ năng chỉ được liệt kê, không có ví dụ sử dụng, thì chưa coi là đạt
        assertTrue(prompt.contains("chỉ xuất hiện trong danh sách kỹ năng"));
        assertTrue(prompt.contains("nhồi từ khoá"));
        // Thang điểm có mốc và có giới hạn khi thiếu yêu cầu bắt buộc
        assertTrue(prompt.contains("90 đến 100"));
        assertTrue(prompt.contains("không vượt quá 59"));
        // Công bằng: không dùng thông tin cá nhân, kể cả ảnh
        assertTrue(prompt.contains("ảnh chân dung"));
        assertTrue(prompt.contains("giới tính"));
        // Suy luận phải đánh dấu và không bịa
        assertTrue(prompt.contains("\"inferred\": true"));
        assertTrue(prompt.contains("Không bịa"));
        // Chống chèn chỉ dẫn trong CV
        assertTrue(prompt.contains("không phải chỉ dẫn"));
        // Điểm tổng đứng cuối schema để model viết nhận định trước rồi mới chấm
        assertTrue(prompt.indexOf("\"aiScore\": 0") > prompt.indexOf("\"skills\": ["));
    }

    @Test
    void userPromptContainsTodayJobAndCv() {
        String prompt = AiPrompts.userPrompt(job(), "Nguyễn Văn A\nJava 4 năm", LocalDate.of(2026, 11, 24));

        // Cần ngày hôm nay để tính "2021 đến nay"
        assertTrue(prompt.contains("Ngày hôm nay: 24/11/2026"));
        assertTrue(prompt.contains("- Java (bắt buộc)"));
        assertTrue(prompt.contains("name: Tiếng Anh"));
        assertTrue(prompt.contains("TOEIC 600"));
        // CV nằm trong thẻ riêng, tách khỏi phần mô tả vị trí
        assertTrue(prompt.contains("<cv>\nNguyễn Văn A\nJava 4 năm\n</cv>"));
        assertTrue(prompt.indexOf("</vi_tri>") < prompt.indexOf("<cv>"));
    }

    @Test
    void userPromptHandlesMissingDataAndLongCv() {
        JobPosting bare = new JobPosting("Kế toán", "", "", List.of(new Skill("Excel", SkillWeight.PREFERRED)));
        String empty = AiPrompts.userPrompt(bare, "   ", LocalDate.of(2026, 1, 1));
        assertTrue(empty.contains("không trích được chữ nào"));
        assertTrue(empty.contains("\"criteria\": []"));

        String longCv = AiPrompts.userPrompt(job(), "x".repeat(20000), LocalDate.of(2026, 1, 1));
        assertTrue(longCv.contains("đã bị cắt"));
        assertTrue(longCv.length() < 18000);
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
