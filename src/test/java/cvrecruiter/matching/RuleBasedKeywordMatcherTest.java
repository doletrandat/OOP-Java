package cvrecruiter.matching;

import cvrecruiter.model.JobPosting;
import cvrecruiter.model.Skill;
import cvrecruiter.model.SkillWeight;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedKeywordMatcherTest {

    private final RuleBasedKeywordMatcher matcher = new RuleBasedKeywordMatcher();

    @Test
    void matchesEnglishSkillAgainstVietnameseCv() {
        JobPosting job = jobWith("Database", "Java");
        String cv = "Kinh nghiệm 3 năm làm việc với cơ sở dữ liệu quan hệ.\nThành thạo Java.";

        Map<String, KeywordHit> hits = matcher.match(job, cv);

        assertTrue(hits.containsKey("Database"));
        assertTrue(hits.containsKey("Java"));
        KeywordHit db = hits.get("Database");
        assertNotNull(db);
        assertEquals("Kinh nghiệm 3 năm làm việc với cơ sở dữ liệu quan hệ.", db.evidence());
        assertEquals(1, db.occurrences());
    }

    @Test
    void shortSkillsDoNotMatchVietnameseWordsWithoutDiacritics() {
        JobPosting job = jobWith("SAP", "SEO", "Go");
        Map<String, KeywordHit> none = matcher.match(job,
                "Sắp xếp chứng từ cho kế toán. Vết sẹo nhỏ. Bàn gỗ.");
        assertTrue(none.isEmpty(), none.keySet().toString());

        Map<String, KeywordHit> hits = matcher.match(job,
                "Chuyển đổi phần mềm sang SAP Business One. Tối ưu SEO cho blog. Viết dịch vụ bằng Go.");
        assertEquals(3, hits.size());
    }

    @Test
    void matchesVietnameseSkillAgainstEnglishCv() {
        JobPosting job = jobWith("Làm việc nhóm");
        Map<String, KeywordHit> hits = matcher.match(job, "Strong teamwork and communication skills.");
        assertTrue(hits.containsKey("Làm việc nhóm"));
    }

    @Test
    void doesNotMatchJsInsideJson() {
        JobPosting job = jobWith("JavaScript");
        Map<String, KeywordHit> hits = matcher.match(job, "Xuất dữ liệu ra định dạng JSON.");
        assertFalse(hits.containsKey("JavaScript"));
    }

    @Test
    void countsRepeatedOccurrences() {
        JobPosting job = jobWith("SQL");
        Map<String, KeywordHit> hits = matcher.match(job, "SQL cơ bản.\nTruy vấn SQL nâng cao.\nTối ưu câu lệnh SQL.");
        assertEquals(3, hits.get("SQL").occurrences());
    }

    private JobPosting jobWith(String... skillNames) {
        List<Skill> skills = java.util.Arrays.stream(skillNames)
                .map(name -> new Skill(name, SkillWeight.REQUIRED))
                .toList();
        return new JobPosting("Test", "Test", "Test", skills);
    }
}
