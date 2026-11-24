package com.ptit.cvrecruiter.model;

import com.ptit.cvrecruiter.common.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Các đối tượng nghiệp vụ tự giữ bất biến và tự xử lý các quy tắc của mình. */
class DomainModelTest {

    private static final Skill JAVA = new Skill("Java", SkillWeight.REQUIRED);

    @Test
    void jobRequiresTitleAndAtLeastOneSkill() {
        assertThrows(ValidationException.class, () -> new JobPosting(" ", "", "", List.of(JAVA)));
        assertThrows(ValidationException.class, () -> new JobPosting("Dev", "", "", List.of()));

        JobPosting job = new JobPosting("Dev", "IT", "", List.of(JAVA));
        assertThrows(ValidationException.class, () -> job.replaceSkills(List.of()));
        assertEquals(1, job.getSkills().size());
    }

    @Test
    void jobEditKeepsFieldsThatAreNull() {
        JobPosting job = new JobPosting("Dev", "IT", "Mô tả", List.of(JAVA));
        job.edit(null, "Kỹ thuật", null);
        assertEquals("Dev", job.getTitle());
        assertEquals("Kỹ thuật", job.getDepartment());
        assertEquals("Mô tả", job.getDescription());

        job.close();
        assertFalse(job.isOpen());
        job.open();
        assertTrue(job.isOpen());
    }

    @Test
    void jobSkillListCannotBeChangedFromOutside() {
        JobPosting job = new JobPosting("Dev", "IT", "", List.of(JAVA));
        assertThrows(UnsupportedOperationException.class, () -> job.getSkills().add(JAVA));
    }

    @Test
    void skillMatchMergesKeywordHitWithAiVerdict() {
        SkillMatch confirmed = new SkillMatch(JAVA);
        confirmed.foundByKeyword("Java 4 năm", 2);
        confirmed.applyAiVerdict(MatchStatus.MATCHED, "", "Dùng hằng ngày");
        assertEquals(MatchStatus.MATCHED, confirmed.getStatus());
        assertEquals(EvidenceSource.BOTH, confirmed.getSource());
        assertEquals("Java 4 năm", confirmed.getEvidence());

        // Từ khoá có nhưng AI thấy chỉ liệt kê: chưa chắc, được nửa trọng số
        SkillMatch listedOnly = new SkillMatch(JAVA);
        listedOnly.foundByKeyword("Kỹ năng: Java", 1);
        listedOnly.applyAiVerdict(MatchStatus.MISSING, "", "Chỉ liệt kê");
        assertEquals(MatchStatus.UNCERTAIN, listedOnly.getStatus());
        assertEquals(JAVA.getWeight().getScore() * 0.5, listedOnly.earnedWeight());

        // Không có từ khoá, AI cũng không thấy: giữ nguyên là thiếu
        SkillMatch missing = new SkillMatch(JAVA);
        missing.applyAiVerdict(MatchStatus.MISSING, "", "");
        assertEquals(MatchStatus.MISSING, missing.getStatus());
        assertEquals(EvidenceSource.NONE, missing.getSource());
        assertEquals(0, missing.earnedWeight());
    }

    @Test
    void applicationDecisionRecordsTime() {
        Application app = new Application("job-1", new ContactInfo("An", "an@example.com", ""),
                "cv.pdf", "cv.pdf", "text");
        assertEquals(HrDecision.NEW, app.getHrDecision());
        app.decide(HrDecision.SHORTLISTED, "  hẹn phỏng vấn  ");
        assertEquals(HrDecision.SHORTLISTED, app.getHrDecision());
        assertFalse(app.getDecidedAt().isBlank());
        assertTrue(app.belongsToJob("job-1"));
    }

    @Test
    void contactRejectsInvalidEmail() {
        assertThrows(ValidationException.class, () -> new ContactInfo("An", "khong-phai-email", ""));
    }
}
