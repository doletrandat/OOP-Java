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
    void takesNameFromLineSharedWithContact() {
        String text = "TRẦN QUỐC BẢO 0900 000 001 (SỐ MẪU)\nquocbao@example.com\nCHUYÊN VIÊN PHÂN TÍCH DỮ LIỆU Hà Nội";
        assertEquals("Trần Quốc Bảo", ContactExtractor.name(text, "cv_mau_a.pdf"));
        assertEquals("Lê Văn Tám", ContactExtractor.name("Lê Văn Tám | tam@example.com | Hà Nội", "x.pdf"));
    }

    @Test
    void handlesLowEffortResumes() {
        assertEquals("Tran Van Hung", ContactExtractor.name("CV xin viec\nho ten: tran van hung\nsdt 0987654321", "cv.pdf"));
        assertEquals("Tom", ContactExtractor.name("My resume\ni want marketing job", "resume-tom.pdf"));
        assertEquals("Tom", ContactExtractor.name("My resume\n\nhi, i am tom. i want marketing job.", "x.pdf"));
        assertEquals("Mai", ContactExtractor.name("CV XIN VIEC\n\nTen: Mai\nnam sinh 2001", "x.pdf"));
        assertEquals("John", ContactExtractor.name("MY CV\n\nJohn\n\nskills: html, css", "my-cv.pdf"));
        assertEquals("Quang", ContactExtractor.name(
                "DON XIN VIEC\n\nKinh gui phong nhan su,\ntoi ten la Quang, nam nay 24 tuoi.", "x.pdf"));
        // CV không có tên thì để "Ứng viên", không lấy chức danh hay tên file làm tên
        assertEquals("Ứng viên", ContactExtractor.name("java dev\n2 nam kn lam java", "cv-java.pdf"));
        assertEquals("Bùi Văn Nam", ContactExtractor.name("Bùi Văn Nam\nĐiều dưỡng", "x.pdf"));
        // "i am good at…" không phải tên
        assertEquals("Ứng viên", ContactExtractor.name("i am good at facebook and instagram", "cv.pdf"));
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
