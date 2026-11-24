/* Kiểu dữ liệu trả về từ API của server Java. */

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
export interface CvInfo {
  pages: number
  fileName: string
  sizeBytes: number
}
/** Body khi tạo hoặc sửa vị trí. */
export interface JobPayload {
  title: string
  department: string
  description: string
  skills: Skill[]
  criteria: Criterion[]
}
