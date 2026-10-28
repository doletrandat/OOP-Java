package com.ptit.cvrecruiter.parsing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfBoxCvTextExtractorTest {

    private final PdfBoxCvTextExtractor extractor = new PdfBoxCvTextExtractor();

    @Test
    void rejectsFileWithoutPdfSignature() {
        byte[] fake = "Day khong phai PDF".getBytes(StandardCharsets.UTF_8);
        assertThrows(CvExtractionException.class, () -> extractor.extract("cv.pdf", fake));
    }

    @Test
    void rejectsNonPdfExtension() {
        byte[] pdf = "%PDF-1.7\n...".getBytes(StandardCharsets.UTF_8);
        assertThrows(CvExtractionException.class, () -> extractor.extract("cv.docx", pdf));
    }

    @Test
    void reportsCorruptedPdf() {
        byte[] broken = "%PDF-1.7\nkhong-phai-cau-truc-pdf".getBytes(StandardCharsets.UTF_8);
        assertThrows(CvExtractionException.class, () -> extractor.extract("cv.pdf", broken));
    }
}
