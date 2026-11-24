import type { ApplicationSummary } from "@/lib/api"

/** Số ứng viên theo trạng thái và điểm cao nhất / trung bình của một vị trí. */
export function pipeline(list: ApplicationSummary[] | undefined) {
  const l = list ?? []
  const scored = l.filter((a) => a.finalScore != null)
  return {
    total: l.length,
    NEW: l.filter((a) => a.hrDecision === "NEW" || !a.hrDecision).length,
    SHORTLISTED: l.filter((a) => a.hrDecision === "SHORTLISTED").length,
    REJECTED: l.filter((a) => a.hrDecision === "REJECTED").length,
    top: scored.length ? Math.max(...scored.map((a) => a.finalScore!)) : null,
    avg: scored.length ? scored.reduce((s, a) => s + a.finalScore!, 0) / scored.length : null,
  }
}
