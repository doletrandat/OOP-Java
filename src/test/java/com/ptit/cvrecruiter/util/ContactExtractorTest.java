package com.ptit.cvrecruiter.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContactExtractorTest {

    private static final String CV = """
            TRAN THI MAI
            Email: mai.tran@example.com | Phone: 0912 345 678
            SUMMARY
            Junior developer with 1 year of PHP/Laravel experience.
            """;

    @Test
    void takesNameFromFirstLineAndTitleCasesIt() {
        assertEquals("Tran Thi Mai", ContactExtractor.name(CV, "cv_mid_support.pdf"));
    }

    @Test
    void skipsHeadingsAndLabels() {
        String text = "CURRICULUM VITAE\nHọ và tên: Nguyễn Văn An\nNgày sinh: 01/01/2000";
        assertEquals("Nguyễn Văn An", ContactExtractor.name(text, "a.pdf"));
    }

    @Test
    void fallsBackToFileName() {
        assertEquals("Le Van Cuong", ContactExtractor.name("Kinh nghiệm: 3 năm Java", "CV_le-van-cuong.pdf"));
        assertEquals("Ứng viên", ContactExtractor.name("", "cv.pdf"));
    }

    @Test
    void extractsEmailAndPhone() {
        assertEquals("mai.tran@example.com", ContactExtractor.email(CV));
        assertEquals("0912345678", ContactExtractor.phone(CV));
        assertEquals("+84912345678", ContactExtractor.phone("SĐT: +84 912.345.678"));
        assertEquals("", ContactExtractor.phone("Năm 2019 đến 2023, mã 123456"));
    }
}
