/* Gọi API của server Java. Token lưu trong localStorage và gửi qua header. */

const TOKEN_KEY = "cvr_admin_token"

export const auth = {
  get token(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* trình duyệt chặn bộ nhớ: phiên chỉ sống trong tab hiện tại */
    }
  },
}

export class UnauthorizedError extends Error {}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn
}

function headers(extra?: HeadersInit): Headers {
  const h = new Headers(extra)
  const t = auth.token
  if (t) h.set("X-Auth-Token", t)
  return h
}

export async function api<T = unknown>(path: string, opts: RequestInit = {}): Promise<T> {
  const h = headers(opts.headers)
  if (opts.body && !(opts.body instanceof FormData)) h.set("Content-Type", "application/json")
  const res = await fetch(path, { ...opts, headers: h })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    if (res.status === 401 && path !== "/api/auth/login") {
      onUnauthorized()
      throw new UnauthorizedError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
    }
    throw new Error((data as { error?: string }).error || `Lỗi ${res.status}`)
  }
  return data as T
}

/** fetch thô (dùng cho file PDF và ảnh trang CV) kèm token. */
export async function rawFetch(url: string): Promise<Response> {
  const res = await fetch(url, { headers: headers() })
  if (!res.ok) {
    let msg = `Lỗi ${res.status}`
    try {
      const data = await res.json()
      if (data?.error) msg = data.error
    } catch {
      /* không phải JSON */
    }
    if (res.status === 401) {
      onUnauthorized()
      msg = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
    }
    throw new Error(msg)
  }
  return res
}

export const post = (body?: unknown): RequestInit => ({
  method: "POST",
  body: body instanceof FormData ? body : body === undefined ? undefined : JSON.stringify(body),
})
export const put = (body: unknown): RequestInit => ({ method: "PUT", body: JSON.stringify(body) })

/* ---------- Kiểu dữ liệu ---------- */

export type SkillWeight = "REQUIRED" | "PREFERRED"
export type CriterionType = "EXPERIENCE" | "EDUCATION" | "LANGUAGE" | "CERTIFICATION" | "OTHER"
export type HrDecision = "NEW" | "SHORTLISTED" | "REJECTED"
export type AiStatusCode = "OK" | "NOT_CONFIGURED" | "ERROR"

export interface Skill {
  name: string
  weight: SkillWeight
}
export interface Criterion {
  name: string
  type: CriterionType
  requirement: string
  weight: number
}
export interface Job {
  id: string
  title: string
  department?: string
  description?: string
  status: "OPEN" | "CLOSED"
  skills: Skill[]
  criteria?: Criterion[]
  applicationCount: number
}
export interface ApplicationSummary {
  id: string
  candidateName: string
  email?: string
  phone?: string
  finalScore: number | null
  keywordScore: number | null
  aiScore: number | null
  aiStatus: AiStatusCode
  matchedCount: number | null
  totalSkills: number
  criteriaMet: number
  criteriaTotal: number
  hrDecision: HrDecision
  hrNote?: string
  submittedAt: string
}
export interface SkillMatch {
  skillName: string
  weight: SkillWeight
  status: "MATCHED" | "UNCERTAIN" | "MISSING"
  source: "KEYWORD" | "AI" | "BOTH" | "NONE"
  occurrences: number
  evidence?: string
  aiNote?: string
}
export interface CriterionResult {
  name: string
  type: CriterionType
  requirement: string
  weight: number
  status: "MET" | "PARTIAL" | "NOT_MET" | "UNKNOWN"
  source?: "RULE" | "AI" | "BOTH"
  inferred?: boolean
  actual?: string
  note?: string
  evidence?: string
}
export interface AiDimension {
  name: string
  score: number
  comment?: string
}
export interface MatchReport {
  finalScore: number
  keywordScore: number
  aiScore: number | null
  criteriaScore: number
  aiStatus: AiStatusCode
  aiError?: string
  aiSummary?: string
  aiDimensions?: AiDimension[]
  aiHighlights?: string[]
  aiConcerns?: string[]
  skillMatches?: SkillMatch[]
  criteriaResults?: CriterionResult[]
  scoredAt?: string
}
export interface Application {
  id: string
  jobId: string
  candidateName: string
  email?: string
  phone?: string
  cvFileName: string
  submittedAt: string
  hrDecision: HrDecision
  hrNote?: string
  report?: MatchReport | null
}
export interface AiStatus {
  configured: boolean
  displayName?: string
  model?: string
  aiWeight: number
}
