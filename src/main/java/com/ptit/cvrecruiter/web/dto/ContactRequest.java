package com.ptit.cvrecruiter.web.dto;

/** Trường null nghĩa là giữ nguyên. */
public record ContactRequest(String name, String email, String phone) {
}
