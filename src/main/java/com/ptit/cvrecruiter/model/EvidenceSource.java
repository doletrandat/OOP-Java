package com.ptit.cvrecruiter.model;

/** Nguồn của bằng chứng khi so khớp kỹ năng. */
public enum EvidenceSource {
    KEYWORD,  // khớp từ khoá / từ đồng nghĩa
    AI,       // Gemini AI xác nhận
    BOTH,     // cả hai tiêu chí cùng xác nhận
    NONE
}
