package com.ptit.cvrecruiter.parsing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/** Bộ trích xuất văn bản từ PDF dùng Apache PDFBox. */
public class PdfBoxCvTextExtractor implements CvTextExtractor {

    @Override
    public String extract(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new CvExtractionException("File rong.");
        }
        if (!looksLikePdf(bytes)) {
            throw new CvExtractionException("File tai len khong phai PDF hop le.");
        }
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return extractPdf(bytes);
        }
        throw new CvExtractionException("Chi chap nhan file PDF. Ban vua nop: " + fileName);
    }

    /** Kiểm tra chữ ký %PDF- ở đầu file để chặn file giả mạo đổi đuôi. */
    private boolean looksLikePdf(byte[] bytes) {
        byte[] magic = {'%', 'P', 'D', 'F', '-'};
        if (bytes.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) return false;
        }
        return true;
    }

    private String extractPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new CvExtractionException(
                        "File PDF này chỉ chứa ảnh (CV scan) nên không có văn bản để chấm điểm. "
                                + "Vui lòng dùng bản PDF xuất trực tiếp từ Word/Google Docs "
                                + "(mở file và bôi đen được chữ).");
            }
            return text.strip();
        } catch (IOException e) {
            throw new CvExtractionException("File PDF khong doc duoc hoac bi hong: " + e.getMessage(), e);
        }
    }
}
