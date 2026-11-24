import type { Criterion, CriterionType, Job, JobPayload } from "@/lib/api"

/** Dòng tiêu chí trong form, có khoá riêng để React giữ đúng ô khi thêm/xoá. */
export type CriterionRow = Criterion & { key: number }

let seq = 0
export const newCriterion = (c?: Partial<Criterion>): CriterionRow => ({
  key: ++seq,
  name: c?.name ?? "",
  type: c?.type ?? "EXPERIENCE",
  requirement: c?.requirement ?? "",
  weight: c?.weight ?? 2,
})

/** Gợi ý nhanh cho tiêu chí hay dùng, bấm là thêm. */
export const CRITERION_PRESETS: Partial<Criterion>[] = [
  { name: "Kinh nghiệm", type: "EXPERIENCE", requirement: "1" },
  { name: "Trình độ", type: "EDUCATION", requirement: "Đại học" },
  { name: "Tiếng Anh", type: "LANGUAGE", requirement: "TOEIC 600" },
  { name: "Chứng chỉ", type: "CERTIFICATION", requirement: "" },
]

/** Hướng dẫn nhập yêu cầu theo từng loại tiêu chí. */
export const REQUIREMENT_HELP: Record<CriterionType, { placeholder: string; help: string }> = {
  EXPERIENCE: { placeholder: "2", help: "Số năm tối thiểu. Hệ thống tính từ các mốc thời gian trong CV." },
  EDUCATION: { placeholder: "Đại học", help: "Trung cấp, Cao đẳng, Đại học, Thạc sĩ hoặc Tiến sĩ." },
  LANGUAGE: { placeholder: "TOEIC 600", help: "Chứng chỉ và mức tối thiểu, ví dụ TOEIC 600, IELTS 6.0, JLPT N3." },
  CERTIFICATION: {
    placeholder: "Kế toán trưởng, ACCA, CPA",
    help: "Tên chứng chỉ, cách nhau bằng dấu phẩy. Có một trong số đó là đạt.",
  },
  OTHER: { placeholder: "làm việc nhóm, thuyết trình", help: "Từ khoá cần tìm, cách nhau bằng dấu phẩy." },
}

/** Nội dung form đang sửa. */
export interface Draft {
  title: string
  department: string
  description: string
  required: string[]
  preferred: string[]
  criteria: CriterionRow[]
}

export function draftFrom(job: Job | null): Draft {
  return {
    title: job?.title ?? "",
    department: job?.department ?? "",
    description: job?.description ?? "",
    required: job?.skills.filter((s) => s.weight === "REQUIRED").map((s) => s.name) ?? [],
    preferred: job?.skills.filter((s) => s.weight !== "REQUIRED").map((s) => s.name) ?? [],
    criteria: (job?.criteria ?? []).map(newCriterion),
  }
}

/** So sánh nội dung, bỏ qua khoá nội bộ của dòng tiêu chí. */
export function snapshot(d: Draft) {
  return JSON.stringify({ ...d, criteria: d.criteria.map(({ key: _key, ...c }) => c) })
}

/**
 * Kiểm tra form. Khoá lỗi trùng với id ô nhập (bỏ tiền tố "je-") để focus vào ô sai đầu tiên.
 * Trả về lỗi và câu thông báo (null nếu hợp lệ).
 */
export function validate(d: Draft): { errors: Record<string, boolean>; message: string | null } {
  const errors: Record<string, boolean> = {}
  if (!d.title.trim()) errors.title = true
  if (!d.required.length && !d.preferred.length) errors.skills = true
  for (const c of d.criteria) {
    if (c.name.trim() && !c.requirement.trim()) errors[`req-${c.key}`] = true
    if (!c.name.trim() && c.requirement.trim()) errors[`name-${c.key}`] = true
  }
  const message = !Object.keys(errors).length
    ? null
    : errors.title
      ? "Vị trí cần có tên."
      : errors.skills
        ? "Thêm ít nhất một kỹ năng."
        : "Mỗi tiêu chí cần có cả tên lẫn yêu cầu."
  return { errors, message }
}

/** Body gửi lên server; bỏ các tiêu chí còn trống. */
export function toPayload(d: Draft): JobPayload {
  return {
    title: d.title.trim(),
    department: d.department.trim(),
    description: d.description,
    skills: [
      ...d.required.map((name) => ({ name, weight: "REQUIRED" as const })),
      ...d.preferred.map((name) => ({ name, weight: "PREFERRED" as const })),
    ],
    criteria: d.criteria
      .filter((c) => c.name.trim() && c.requirement.trim())
      .map(({ name, type, requirement, weight }) => ({
        name: name.trim(),
        type,
        requirement: requirement.trim(),
        weight,
      })),
  }
}

/** Chuyển một kỹ năng giữa nhóm bắt buộc và lợi thế. */
export function moveSkill(d: Draft, name: string, to: "required" | "preferred"): Draft {
  return to === "required"
    ? { ...d, preferred: d.preferred.filter((x) => x !== name), required: [...d.required, name] }
    : { ...d, required: d.required.filter((x) => x !== name), preferred: [...d.preferred, name] }
}
