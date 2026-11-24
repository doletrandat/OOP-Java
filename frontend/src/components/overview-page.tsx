import { ArrowRightIcon, BriefcaseIcon, InboxIcon, PlusIcon } from "lucide-react"

import { JobStatusBadge, ScoreBadge } from "@/components/status"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import type { Job } from "@/lib/api"
import { paths } from "@/lib/router"
import { pipeline, useStore } from "@/lib/store"

export function OverviewPage({ onCreateJob }: { onCreateJob: () => void }) {
  const { jobs, apps } = useStore()

  const open = jobs?.filter((j) => j.status === "OPEN") ?? []
  // Số liệu chỉ tính các vị trí đang tuyển; vị trí đã đóng không còn việc phải làm
  const totals = pipeline(open.flatMap((j) => apps[j.id] ?? []))
  const closed = jobs?.filter((j) => j.status !== "OPEN") ?? []
  const waiting = open
    .map((j) => ({ job: j, n: pipeline(apps[j.id]).NEW }))
    .filter((x) => x.n > 0)
    .sort((a, b) => b.n - a.n)

  return (
    <div className="mx-auto w-full max-w-6xl p-4 sm:p-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {jobs
              ? totals.NEW
                ? `Còn ${totals.NEW} CV chưa xử lý ở ${waiting.length} vị trí đang tuyển.`
                : "Bạn đã xử lý hết CV."
              : "Đang tải…"}
          </p>
        </div>
        <Button onClick={onCreateJob}>
          <PlusIcon data-icon="inline-start" />
          Tạo vị trí
        </Button>
      </div>

      <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Vị trí đang tuyển" value={jobs ? open.length : null} />
        <Stat label="CV đang xét" value={jobs ? totals.total : null} />
        <Stat label="Chưa xử lý" value={jobs ? totals.NEW : null} tone={totals.NEW ? "warning" : undefined} />
        <Stat label="Mời phỏng vấn" value={jobs ? totals.SHORTLISTED : null} tone="success" />
      </div>

      {waiting.length > 0 && (
        <Card className="mt-6" size="sm">
          <CardHeader>
            <CardTitle>Cần xem tiếp</CardTitle>
            <CardDescription>Mở thẳng CV đầu tiên chưa xử lý, xếp theo điểm cao nhất.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col divide-y">
            {waiting.map(({ job, n }) => {
              const next = [...(apps[job.id] ?? [])]
                .filter((a) => a.hrDecision === "NEW")
                .sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1))[0]
              return (
                <a
                  key={job.id}
                  href={next ? paths.review(job.id, next.id) : paths.job(job.id)}
                  className="group flex items-center gap-3 rounded-md py-2.5 outline-none first:pt-0 last:pb-0 focus-visible:ring-3 focus-visible:ring-ring/50"
                >
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium">{job.title}</div>
                    <div className="text-xs text-muted-foreground">
                      {n} CV chưa xử lý{next ? `, bắt đầu với ${next.candidateName}` : ""}
                    </div>
                  </div>
                  {next && <ScoreBadge value={next.finalScore} />}
                  <ArrowRightIcon className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
                </a>
              )
            })}
          </CardContent>
        </Card>
      )}

      <h2 className="mt-8 mb-3 text-sm font-medium text-muted-foreground">Vị trí đang tuyển</h2>
      {!jobs ? (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-44 rounded-xl" />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <Card>
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <BriefcaseIcon />
              </EmptyMedia>
              <EmptyTitle>Chưa có vị trí nào</EmptyTitle>
              <EmptyDescription>
                Tạo một vị trí với danh sách kỹ năng cần tìm. Sau đó kéo các file CV vào để hệ thống chấm và xếp hạng.
              </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <Button onClick={onCreateJob}>
                <PlusIcon data-icon="inline-start" />
                Tạo vị trí
              </Button>
            </EmptyContent>
          </Empty>
        </Card>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {open.map((j) => (
            <JobCard key={j.id} job={j} />
          ))}
          <button
            type="button"
            onClick={onCreateJob}
            className="flex min-h-44 flex-col items-center justify-center gap-2 rounded-xl border border-dashed text-sm text-muted-foreground transition-colors outline-none hover:border-primary/50 hover:bg-accent/40 hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <PlusIcon className="size-5" />
            Tạo vị trí mới
          </button>
        </div>
      )}

      {closed.length > 0 && (
        <>
          <h2 className="mt-8 mb-3 text-sm font-medium text-muted-foreground">Đã đóng</h2>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {closed.map((j) => (
              <JobCard key={j.id} job={j} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function Stat({ label, value, tone }: { label: string; value: number | null; tone?: "success" | "warning" }) {
  return (
    <Card size="sm">
      <CardHeader>
        <CardDescription>{label}</CardDescription>
        <div
          className={`text-2xl font-semibold tracking-tight tabular-nums ${
            tone === "warning" ? "text-warning-foreground" : tone === "success" ? "text-success-foreground" : ""
          }`}
        >
          {value == null ? <Skeleton className="h-8 w-10" /> : value}
        </div>
      </CardHeader>
    </Card>
  )
}

function JobCard({ job }: { job: Job }) {
  const { apps } = useStore()
  const list = apps[job.id]
  const p = pipeline(list)
  const top = [...(list ?? [])].sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1)).slice(0, 3)

  return (
    <Card className="relative transition-shadow hover:ring-foreground/20">
      <CardHeader>
        <CardTitle className="truncate">
          <a href={paths.job(job.id)} className="outline-none after:absolute after:inset-0 after:rounded-xl focus-visible:after:ring-3 focus-visible:after:ring-ring/50">
            {job.title}
          </a>
        </CardTitle>
        <CardDescription className="flex items-center gap-2">
          <span className="truncate">{job.department || "Chưa ghi phòng ban"}</span>
          {job.status !== "OPEN" && <JobStatusBadge status={job.status} />}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {p.total === 0 ? (
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <InboxIcon className="size-4" /> Chưa có CV nào.
          </p>
        ) : (
          <>
            <PipelineBar p={p} />
            <ul className="flex flex-col gap-1.5">
              {top.map((a) => (
                <li key={a.id} className="flex items-center justify-between gap-2 text-sm">
                  <span className="truncate">{a.candidateName}</span>
                  <ScoreBadge value={a.finalScore} />
                </li>
              ))}
            </ul>
          </>
        )}
      </CardContent>
      <CardFooter className="mt-auto text-xs text-muted-foreground">
        {job.skills.length} kỹ năng · {job.criteria?.length ?? 0} tiêu chí
      </CardFooter>
    </Card>
  )
}

export function PipelineBar({ p }: { p: ReturnType<typeof pipeline> }) {
  const seg = [
    { n: p.SHORTLISTED, cls: "bg-success", label: "Mời phỏng vấn" },
    { n: p.NEW, cls: "bg-muted-foreground/30", label: "Chưa xử lý" },
    { n: p.REJECTED, cls: "bg-destructive/70", label: "Từ chối" },
  ]
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex h-1.5 w-full gap-0.5 overflow-hidden rounded-full bg-muted" aria-hidden>
        {seg.map(
          (s) => s.n > 0 && <span key={s.label} className={s.cls} style={{ width: `${(s.n / p.total) * 100}%` }} />
        )}
      </div>
      <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span className="font-medium text-foreground tabular-nums">{p.total} CV</span>
        {seg.map((s) => (
          <span key={s.label} className="inline-flex items-center gap-1.5 tabular-nums">
            <span aria-hidden className={`size-1.5 rounded-full ${s.cls}`} />
            {s.n} {s.label.toLowerCase()}
          </span>
        ))}
      </div>
    </div>
  )
}
