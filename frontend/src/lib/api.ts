/* Các endpoint của server Java. Component gọi qua đây thay vì tự ghép URL. */

import { del, post, put, rawFetch, request } from "@/lib/http"
import type {
  AiStatus,
  Application,
  ApplicationSummary,
  CvInfo,
  HrDecision,
  Job,
  JobPayload,
} from "@/lib/types"

export { auth, setUnauthorizedHandler, UnauthorizedError } from "@/lib/http"
export type * from "@/lib/types"

const jobUrl = (id: string) => `/api/admin/jobs/${id}`
const appUrl = (id: string) => `/api/admin/applications/${id}`

export const authApi = {
  login: (password: string) => request<{ token: string }>("/api/auth/login", post({ password })),
  logout: () => request("/api/auth/logout", post()),
}

export const aiApi = {
  status: () => request<AiStatus>("/api/admin/ai-status"),
}

export const jobsApi = {
  list: () => request<Job[]>("/api/admin/jobs"),
  get: (id: string) => request<Job>(jobUrl(id)),
  create: (payload: JobPayload) => request<Job>("/api/admin/jobs", post(payload)),
  update: (id: string, payload: JobPayload) => request<Job>(jobUrl(id), put(payload)),
  setOpen: (id: string, open: boolean) => request<Job>(jobUrl(id), put({ status: open ? "OPEN" : "CLOSED" })),
  remove: (id: string) => request(jobUrl(id), del()),
  rescoreAll: (id: string) => request<{ rescored: number }>(`${jobUrl(id)}/rescore-all`, post()),
}

export const applicationsApi = {
  listByJob: (jobId: string) => request<ApplicationSummary[]>(`${jobUrl(jobId)}/applications`),
  get: (id: string) => request<Application>(appUrl(id)),
  /** form gồm file CV và (tuỳ chọn) name, email, phone. */
  upload: (jobId: string, form: FormData) => request<Application>(`${jobUrl(jobId)}/cv`, post(form)),
  rescore: (id: string) => request<Application>(`${appUrl(id)}/rescore`, post()),
  decide: (id: string, decision: HrDecision, note: string) =>
    request<Application>(`${appUrl(id)}/decision`, post({ decision, note })),
  updateContact: (id: string, contact: { name: string; email: string; phone: string }) =>
    request<Application>(appUrl(id), put(contact)),
  remove: (id: string) => request(appUrl(id), del()),
}

export const cvApi = {
  info: async (appId: string) => (await rawFetch(`${appUrl(appId)}/cv/info`)).json() as Promise<CvInfo>,
  page: async (appId: string, page: number) => (await rawFetch(`${appUrl(appId)}/cv/pages/${page}`)).blob(),
  pdf: async (appId: string) => (await rawFetch(`${appUrl(appId)}/cv`)).blob(),
}
