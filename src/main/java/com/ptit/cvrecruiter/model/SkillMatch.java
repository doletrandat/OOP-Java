package com.ptit.cvrecruiter.model;

/** Kết quả so khớp chi tiết cho một kỹ năng, kèm bằng chứng trích dẫn. */
public class SkillMatch {

    private String skillName;
    private SkillWeight weight;
    private MatchStatus status = MatchStatus.MISSING;
    private EvidenceSource source = EvidenceSource.NONE;
    private String evidence = "";   // câu trích dẫn nguyên văn từ CV
    private int occurrences;        // số lần xuất hiện trong CV (theo từ khoá)
    private String aiNote = "";     // nhận xét riêng của AI cho kỹ năng này

    public SkillMatch() {
        // Gson cần constructor rỗng
    }

    public SkillMatch(String skillName, SkillWeight weight) {
        this.skillName = skillName;
        this.weight = weight;
    }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public SkillWeight getWeight() { return weight == null ? SkillWeight.PREFERRED : weight; }
    public void setWeight(SkillWeight weight) { this.weight = weight; }

    public MatchStatus getStatus() { return status == null ? MatchStatus.MISSING : status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public EvidenceSource getSource() { return source == null ? EvidenceSource.NONE : source; }
    public void setSource(EvidenceSource source) { this.source = source; }

    public String getEvidence() { return evidence == null ? "" : evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public int getOccurrences() { return occurrences; }
    public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

    public String getAiNote() { return aiNote == null ? "" : aiNote; }
    public void setAiNote(String aiNote) { this.aiNote = aiNote; }
}
