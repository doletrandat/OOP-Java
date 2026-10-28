package com.ptit.cvrecruiter.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizerTest {

    @Test
    void removesVietnameseDiacriticsAndLowercases() {
        assertEquals("co so du lieu", TextNormalizer.normalize("Cơ Sở Dữ Liệu"));
        assertEquals("lap trinh huong doi tuong", TextNormalizer.normalize("Lập trình hướng đối tượng"));
    }

    @Test
    void keepsTechSymbols() {
        assertEquals("c#", TextNormalizer.normalize("C#"));
        assertEquals("c++", TextNormalizer.normalize("C++"));
        assertEquals(".net core", TextNormalizer.normalize(".NET Core"));
    }

    @Test
    void splitsLinesAndSentencesForEvidence() {
        var parts = TextNormalizer.splitSentences("Kinh nghiệm Java. Làm việc nhóm tốt;\nDùng SQL");
        assertEquals(3, parts.size());
        assertTrue(parts.contains("Kinh nghiệm Java."));
    }
}
