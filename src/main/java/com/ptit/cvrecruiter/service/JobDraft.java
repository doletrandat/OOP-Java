package com.ptit.cvrecruiter.service;

import com.ptit.cvrecruiter.model.Criterion;
import com.ptit.cvrecruiter.model.JobStatus;
import com.ptit.cvrecruiter.model.Skill;

import java.util.List;

/**
 * Dữ liệu HR gửi lên để tạo hoặc sửa vị trí. Khi sửa, trường null nghĩa là giữ nguyên
 * (ví dụ chỉ gửi {@code status} để đóng hoặc mở lại vị trí).
 */
public record JobDraft(String title, String department, String description,
                       List<Skill> skills, List<Criterion> criteria, JobStatus status) {

    public static JobDraft statusOnly(JobStatus status) {
        return new JobDraft(null, null, null, null, null, status);
    }
}
