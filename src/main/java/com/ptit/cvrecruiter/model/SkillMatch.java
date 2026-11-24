package com.ptit.cvrecruiter.model;

/**
 * Kết quả so khớp một kỹ năng, kèm bằng chứng trích dẫn.
 * Tự biết cách ghép kết quả của bộ khớp từ khoá với nhận định của AI.
 */
public class SkillMatch {

    private String skillName;
    private SkillWeight weight;
    private MatchStatus status = MatchStatus.MISSING;
    private EvidenceSource source = EvidenceSource.NONE;
    private String evidence = "";   // câu trích dẫn nguyên văn từ CV
    private int occurrences;        // số lần xuất hiện trong CV (theo từ khoá)
    private String aiNote = "";     // nhận xét riêng của AI cho kỹ năng này

    private SkillMatch() {
        // Gson
    }

    public SkillMatch(Skill skill) {
        this.skillName = skill.getName();
        this.weight = skill.getWeight();
    }

    /** Bộ khớp từ khoá tìm thấy kỹ năng trong CV. */
    public void foundByKeyword(String evidence, int occurrences) {
        this.status = MatchStatus.MATCHED;
        this.source = EvidenceSource.KEYWORD;
        this.evidence = evidence == null ? "" : evidence;
        this.occurrences = occurrences;
    }

    /**
     * Ghép nhận định của AI:
     * <ul>
     *   <li>AI thấy có dùng: khớp (nguồn BOTH nếu từ khoá cũng thấy, AI nếu chỉ AI thấy).</li>
     *   <li>Từ khoá thấy nhưng AI cho rằng chỉ được liệt kê, ghi "cơ bản" hay dùng với nghĩa khác:
     *       chưa đủ để coi là khớp, chuyển sang "chưa chắc" để HR hỏi lại.</li>
     *   <li>Chỉ AI thấy bằng chứng gián tiếp: "chưa chắc".</li>
     * </ul>
     */
    public void applyAiVerdict(MatchStatus aiStatus, String aiEvidence, String aiNote) {
        boolean keywordHit = source == EvidenceSource.KEYWORD;
        this.aiNote = aiNote == null ? "" : aiNote;
        if (getEvidence().isEmpty() && aiEvidence != null && !aiEvidence.isEmpty()) {
            this.evidence = aiEvidence;
        }
        switch (aiStatus) {
            case MATCHED -> {
                this.status = MatchStatus.MATCHED;
                this.source = keywordHit ? EvidenceSource.BOTH : EvidenceSource.AI;
            }
            case UNCERTAIN, MISSING -> {
                if (keywordHit) {
                    this.status = MatchStatus.UNCERTAIN;
                    this.source = EvidenceSource.BOTH;
                } else if (aiStatus == MatchStatus.UNCERTAIN) {
                    this.status = MatchStatus.UNCERTAIN;
                    this.source = EvidenceSource.AI;
                }
            }
        }
    }

    /** Phần trọng số được tính: khớp = đủ, chưa chắc = một nửa, không thấy = 0. */
    public double earnedWeight() {
        double w = getWeight().getScore();
        return switch (getStatus()) {
            case MATCHED -> w;
            case UNCERTAIN -> w * 0.5;
            case MISSING -> 0;
        };
    }

    public String getSkillName() { return skillName; }

    public SkillWeight getWeight() { return weight == null ? SkillWeight.PREFERRED : weight; }

    public MatchStatus getStatus() { return status == null ? MatchStatus.MISSING : status; }

    public EvidenceSource getSource() { return source == null ? EvidenceSource.NONE : source; }

    public String getEvidence() { return evidence == null ? "" : evidence; }

    public int getOccurrences() { return occurrences; }

    public String getAiNote() { return aiNote == null ? "" : aiNote; }
}
