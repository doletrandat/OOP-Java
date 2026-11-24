import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { toast } from "sonner"

import { aiApi, applicationsApi, jobsApi, type AiStatus, type ApplicationSummary, type HrDecision, type Job } from "@/lib/api"

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
  // Bản mới nhất của apps cho các hàm callback (không gắn vào deps để callback giữ nguyên)
  const appsRef = useRef(apps)
  useEffect(() => {
    appsRef.current = apps
  }, [apps])

  const loadApps = useCallback(async (jobId: string) => {
    const list = await applicationsApi.listByJob(jobId)
    setApps((prev) => ({ ...prev, [jobId]: list }))
  }, [])

  const reloadJobs = useCallback(async () => {
    try {
      const list = await jobsApi.list()
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
        const [job] = await Promise.all([jobsApi.get(jobId), loadApps(jobId)])
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
            applicationsApi.decide(id, decision, before.get(id)?.hrNote ?? "")
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
    aiApi.status()
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
