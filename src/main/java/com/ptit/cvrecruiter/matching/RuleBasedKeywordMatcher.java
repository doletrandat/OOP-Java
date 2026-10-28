package com.ptit.cvrecruiter.matching;

import com.ptit.cvrecruiter.model.JobPosting;
import com.ptit.cvrecruiter.model.Skill;
import com.ptit.cvrecruiter.util.TextNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ so khớp từ khoá dựa trên từ điển đồng nghĩa.
 * So trên văn bản đã chuẩn hoá (không dấu, chữ thường) nhưng
 * trích dẫn bằng chứng từ câu gốc để HR đọc được.
 */
public class RuleBasedKeywordMatcher implements KeywordMatcher {

    private final SynonymDictionary dictionary;

    public RuleBasedKeywordMatcher(SynonymDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public RuleBasedKeywordMatcher() {
        this(new SynonymDictionary());
    }

    @Override
    public Map<String, KeywordHit> match(JobPosting job, String cvText) {
        Map<String, KeywordHit> result = new HashMap<>();
        if (cvText == null || cvText.isBlank()) return result;

        List<String> sentences = TextNormalizer.splitSentences(cvText);
        // Chuẩn hoá từng câu một lần để tái sử dụng cho mọi kỹ năng
        List<String> normalizedSentences = new ArrayList<>(sentences.size());
        for (String s : sentences) {
            normalizedSentences.add(TextNormalizer.normalize(s));
        }

        for (Skill skill : job.getSkills()) {
            if (skill.getName() == null || skill.getName().isBlank()) continue;
            KeywordHit hit = findSkill(skill.getName(), sentences, normalizedSentences);
            if (hit != null) result.put(skill.getName(), hit);
        }
        return result;
    }

    private KeywordHit findSkill(String skillName, List<String> sentences, List<String> normalized) {
        List<String> variants = dictionary.expand(skillName);
        int total = 0;
        String evidence = null;
        String matchedVariant = null;

        for (String variant : variants) {
            Pattern pattern = compilePattern(variant);
            if (pattern == null) continue;
            for (int i = 0; i < normalized.size(); i++) {
                Matcher m = pattern.matcher(normalized.get(i));
                int count = 0;
                while (m.find()) {
                    count++;
                    if (evidence == null) {
                        evidence = TextNormalizer.truncate(sentences.get(i), 320);
                        matchedVariant = variant;
                    }
                }
                total += count;
            }
        }

        if (total == 0) return null;
        return new KeywordHit(Math.min(total, 999), evidence, matchedVariant);
    }

    /**
     * Biến thể -> regex: các từ cách nhau bởi khoảng trắng, có ranh giới từ
     * ở hai đầu khi ký tự đầu/cuối là chữ/số (tránh "js" khớp trong "json").
     */
    private Pattern compilePattern(String variant) {
        if (variant == null || variant.isBlank()) return null;
        String[] tokens = variant.trim().split("\\s+");
        StringBuilder regex = new StringBuilder();
        boolean startWordChar = isWordChar(variant.charAt(0));
        boolean endWordChar = isWordChar(variant.charAt(variant.length() - 1));
        if (startWordChar) regex.append("\\b");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) regex.append("\\s+");
            regex.append(Pattern.quote(tokens[i]));
        }
        if (endWordChar) regex.append("\\b");
        try {
            return Pattern.compile(regex.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c);
    }
}
