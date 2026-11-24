import type { CriterionType, HrDecision } from "@/lib/api"

export type Tone = "success" | "warning" | "destructive" | "secondary"

export function fmtScore(v: number | null | undefined): string {
  return v == null || v < 0 ? "—" : String(Math.round(v))
}

export function scoreTone(v: number | null | undefined): Tone {
  if (v == null || v < 0) return "secondary"
  if (v >= 75) return "success"
  if (v >= 50) return "warning"
  return "destructive"
}

/** Màu thanh Progress theo mức điểm. */
export const progressTone: Record<Tone, string> = {
  success: "[&>[data-slot=progress-indicator]]:bg-success",
  warning: "[&>[data-slot=progress-indicator]]:bg-warning",
  destructive: "[&>[data-slot=progress-indicator]]:bg-destructive",
  secondary: "[&>[data-slot=progress-indicator]]:bg-muted-foreground",
}

export const scoreText: Record<Tone, string> = {
  success: "text-success-foreground",
  warning: "text-warning-foreground",
  destructive: "text-destructive",
  secondary: "text-muted-foreground",
}

export function fmtTime(iso?: string): string {
  if (!iso) return "—"
  try {
    return new Date(iso).toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  } catch {
    return iso
  }
}

export const AI_STATUS_LABEL = {
  OK: "Đã chấm AI",
  NOT_CONFIGURED: "Chưa bật AI",
  ERROR: "AI lỗi",
} as const

export const DECISION_META: Record<HrDecision, { label: string; variant: Tone }> = {
  NEW: { label: "Chưa xử lý", variant: "secondary" },
  SHORTLISTED: { label: "Mời phỏng vấn", variant: "success" },
  REJECTED: { label: "Từ chối", variant: "destructive" },
}

export const CRITERION_TYPES: { value: CriterionType; label: string; hint: string }[] = [
  { value: "EXPERIENCE", label: "Số năm kinh nghiệm", hint: "VD: 2" },
  { value: "EDUCATION", label: "Trình độ đào tạo", hint: "VD: Đại học" },
  { value: "LANGUAGE", label: "Ngoại ngữ", hint: "VD: TOEIC 600, IELTS 6.5" },
  { value: "CERTIFICATION", label: "Chứng chỉ nghề", hint: "VD: AWS Certified, Oracle OCP" },
  { value: "OTHER", label: "Khác", hint: "VD: làm việc nhóm" },
]

export function criterionTypeLabel(t: CriterionType) {
  return (CRITERION_TYPES.find((c) => c.value === t) ?? CRITERION_TYPES[4]).label
}

export const WEIGHT_LABEL: Record<number, string> = {
  1: "Nhẹ",
  2: "Quan trọng",
  3: "Rất quan trọng",
}

const rtf = new Intl.RelativeTimeFormat("vi", { numeric: "auto" })
/** "3 giờ trước", "hôm qua"… dùng cho danh sách; ngày giờ đầy đủ đặt trong title. */
export function relativeTime(iso?: string): string {
  if (!iso) return "—"
  const diff = (new Date(iso).getTime() - Date.now()) / 1000
  if (Number.isNaN(diff)) return "—"
  const abs = Math.abs(diff)
  if (abs < 60) return "vừa xong"
  if (abs < 3600) return rtf.format(Math.round(diff / 60), "minute")
  if (abs < 86400) return rtf.format(Math.round(diff / 3600), "hour")
  if (abs < 86400 * 30) return rtf.format(Math.round(diff / 86400), "day")
  return fmtTime(iso).split(" ").pop() ?? fmtTime(iso)
}

/** Chữ cái đầu của họ và tên cuối: "Trần Thị Mai" → "TM". */
export function initials(name: string): string {
  const w = name.trim().split(/\s+/).filter(Boolean)
  if (!w.length) return "?"
  return ((w[0][0] ?? "") + (w.length > 1 ? w[w.length - 1][0] : "")).toUpperCase()
}

/** Bỏ dấu tiếng Việt để tìm kiếm "mai" khớp "Mai", "tran" khớp "Trần". */
export function fold(s: string): string {
  return s.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase()
}
