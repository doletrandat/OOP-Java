import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { toast } from "sonner"

import { api, post, type AiStatus, type ApplicationSummary, type HrDecision, type Job } from "@/lib/api"

/**
 * Bộ nhớ dùng chung cho cả ứng dụng: danh sách vị trí và ứng viên của từng vị trí.
 * Thanh bên, trang tổng quan và trang vị trí cùng đọc một nguồn nên số liệu luôn khớp.
 */
interface Store {
  aiStatus: AiStatus | null
  jobs: Job[] | null
  apps: Record<string, ApplicationSummary[] | undefined>
  reloadJobs: () => Promise<void>
  reloadJob: (jobId: string) => Promise<void>
  /** Cập nhật lạc quan một ứng viên trong bộ nhớ (trước khi server trả lời). */
  patchApp: (jobId: string, appId: string, patch: Partial<ApplicationSummary>) => void
  removeApps: (jobId: string, ids: string[]) => void
  decide: (jobId: string, ids: string[], decision: HrDecision, opts?: { silent?: boolean }) => Promise<void>
}

const Ctx = createContext<Store | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  const [aiStatus, setAiStatus] = useState<AiStatus | null>(null)
  const [jobs, setJobs] = useState<Job[] | null>(null)
  const [apps, setApps] = useState<Record<string, ApplicationSummary[] | undefined>>({})
  const appsRef = useRef(apps)
  appsRef.current = apps

  const loadApps = useCallback(async (jobId: string) => {
    const list = await api<ApplicationSummary[]>(`/api/admin/jobs/${jobId}/applications`)
    setApps((prev) => ({ ...prev, [jobId]: list }))
  }, [])

  const reloadJobs = useCallback(async () => {
    try {
      const list = await api<Job[]>("/api/admin/jobs")
      setJobs(list)
      // Tải ứng viên của mọi vị trí để thanh bên và trang tổng quan có số liệu theo trạng thái
      await Promise.all(list.map((j) => loadApps(j.id).catch(() => {})))
    } catch (e) {
      toast.error((e as Error).message)
    }
  }, [loadApps])

  const reloadJob = useCallback(
    async (jobId: string) => {
      try {
        const [job] = await Promise.all([api<Job>(`/api/admin/jobs/${jobId}`), loadApps(jobId)])
        setJobs((prev) => {
          if (!prev) return prev
          const count = appsRef.current[jobId]?.length ?? job.applicationCount
          const merged = { ...job, applicationCount: count }
          return prev.some((j) => j.id === jobId) ? prev.map((j) => (j.id === jobId ? merged : j)) : [...prev, merged]
        })
      } catch (e) {
        toast.error((e as Error).message)
      }
    },
    [loadApps]
  )

  const patchApp = useCallback((jobId: string, appId: string, patch: Partial<ApplicationSummary>) => {
    setApps((prev) => ({
      ...prev,
      [jobId]: prev[jobId]?.map((a) => (a.id === appId ? { ...a, ...patch } : a)),
    }))
  }, [])

  const removeApps = useCallback((jobId: string, ids: string[]) => {
    setApps((prev) => ({ ...prev, [jobId]: prev[jobId]?.filter((a) => !ids.includes(a.id)) }))
    setJobs((prev) =>
      prev?.map((j) => (j.id === jobId ? { ...j, applicationCount: Math.max(0, j.applicationCount - ids.length) } : j)) ??
      prev
    )
  }, [])

  const decide = useCallback(
    async (jobId: string, ids: string[], decision: HrDecision, opts: { silent?: boolean } = {}) => {
      const before = new Map((appsRef.current[jobId] ?? []).map((a) => [a.id, a]))
      ids.forEach((id) => patchApp(jobId, id, { hrDecision: decision }))
      try {
        await Promise.all(
          ids.map((id) =>
            api(`/api/admin/applications/${id}/decision`, post({ decision, note: before.get(id)?.hrNote ?? "" }))
          )
        )
      } catch (e) {
        ids.forEach((id) => {
          const prev = before.get(id)
          if (prev) patchApp(jobId, id, { hrDecision: prev.hrDecision })
        })
        if (!opts.silent) toast.error((e as Error).message)
        throw e
      }
    },
    [patchApp]
  )

  useEffect(() => {
    api<AiStatus>("/api/admin/ai-status")
      .then(setAiStatus)
      .catch(() => setAiStatus({ configured: false, aiWeight: 0 }))
    reloadJobs()
  }, [reloadJobs])

  const value = useMemo(
    () => ({ aiStatus, jobs, apps, reloadJobs, reloadJob, patchApp, removeApps, decide }),
    [aiStatus, jobs, apps, reloadJobs, reloadJob, patchApp, removeApps, decide]
  )
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useStore() {
  const s = useContext(Ctx)
  if (!s) throw new Error("useStore must be used inside StoreProvider")
  return s
}

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
