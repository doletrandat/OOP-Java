import { useCallback, useEffect, useMemo, useState } from "react"
import {
  CheckIcon,
  LockIcon,
  MoreHorizontalIcon,
  PencilIcon,
  RefreshCwIcon,
  RotateCcwIcon,
  Trash2Icon,
  UnlockIcon,
  UploadIcon,
} from "lucide-react"
import { toast } from "sonner"

import { CandidateTable } from "@/components/candidate-table"
import type { Filter, SortKey } from "@/components/candidates/use-candidate-list"
import { PipelineBar } from "@/components/overview-page"
import { RequirementsView } from "@/components/requirements-view"
import { ReviewView } from "@/components/review-view"
import { JobStatusBadge } from "@/components/status"
import { UploadDialog } from "@/components/upload"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Spinner } from "@/components/ui/spinner"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { jobsApi, type Job } from "@/lib/api"
import { fmtScore } from "@/lib/format"
import { navigate, paths, type Route } from "@/lib/router"
import { pipeline } from "@/lib/pipeline"
import { useStore } from "@/lib/store"
import { useUploadQueue } from "@/lib/use-upload-queue"

export function JobWorkspace({
  job,
  route,
  onEditJob,
}: {
  job: Job
  route: Exclude<Route, { name: "home" }>
  onEditJob: () => void
}) {
  const { apps, reloadJob, reloadJobs } = useStore()
  const list = apps[job.id]
  const p = pipeline(list)

  // Bộ lọc giữ nguyên khi mở hồ sơ rồi quay lại danh sách
  const [filter, setFilter] = useState<Filter>("all")
  const [query, setQuery] = useState("")
  const [sort, setSort] = useState<SortKey>("score")
  const [visibleIds, setVisibleIds] = useState<string[]>([])
  const [reviewOrder, setReviewOrder] = useState<string[]>([])

  const [uploadOpen, setUploadOpen] = useState(false)
  const [confirm, setConfirm] = useState<"rescore" | "delete" | null>(null)
  const [rescoring, setRescoring] = useState(false)

  const queue = useUploadQueue(job.id, () => reloadJob(job.id))

  useEffect(() => {
    if (!list) reloadJob(job.id)
  }, [job.id, list, reloadJob])

  // Thứ tự duyệt hồ sơ chụp lại lúc mở, để quyết định xong (và ứng viên rời bộ lọc) vẫn đi tiếp đúng thứ tự
  const openReview = useCallback(
    (appId: string) => {
      setReviewOrder(visibleIds.includes(appId) ? visibleIds : [])
      navigate(paths.review(job.id, appId))
    },
    [job.id, visibleIds]
  )

  const byScore = useMemo(
    () => [...(list ?? [])].sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1)).map((a) => a.id),
    [list]
  )
  const order = reviewOrder.length ? reviewOrder : byScore

  async function rescoreAll() {
    setRescoring(true)
    const run = jobsApi.rescoreAll(job.id).then(async (r) => {
      await reloadJob(job.id)
      return r
    })
    toast.promise(run, {
      loading: `Đang chấm lại ${p.total} CV…`,
      success: (r) => `Đã chấm lại ${r.rescored} CV.`,
      error: (e: Error) => e.message,
    })
    run.catch(() => {}).finally(() => setRescoring(false))
  }

  async function toggleStatus() {
    const closing = job.status === "OPEN"
    try {
      await jobsApi.setOpen(job.id, !closing)
      toast.success(closing ? "Đã đóng vị trí. Bạn vẫn xem được các CV cũ." : "Đã mở lại vị trí.")
      reloadJob(job.id)
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  async function deleteJob() {
    try {
      await jobsApi.remove(job.id)
      toast.success(`Đã xoá "${job.title}".`)
      navigate(paths.home())
      reloadJobs()
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  if (route.name === "review") {
    return (
      <ReviewView
        key="review"
        job={job}
        appId={route.appId}
        order={order}
        onBack={() => navigate(paths.job(job.id))}
      />
    )
  }

  const tab = route.tab === "requirements" ? "requirements" : "candidates"

  return (
    <div className="mx-auto w-full max-w-6xl p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold tracking-tight text-balance">{job.title}</h1>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
            <span>{job.department || "Chưa ghi phòng ban"}</span>
            <JobStatusBadge status={job.status} />
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={onEditJob}>
            <PencilIcon data-icon="inline-start" />
            Sửa
          </Button>
          <Button onClick={() => setUploadOpen(true)}>
            {queue.active ? <Spinner data-icon="inline-start" /> : <UploadIcon data-icon="inline-start" />}
            {queue.active ? `Đang tải ${queue.finished + 1}/${queue.items.length}` : "Tải CV lên"}
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="icon" aria-label="Thao tác khác">
                <MoreHorizontalIcon />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-52">
              <DropdownMenuItem disabled={!p.total || rescoring} onSelect={() => setConfirm("rescore")}>
                <RefreshCwIcon /> Chấm lại tất cả
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={toggleStatus}>
                {job.status === "OPEN" ? <LockIcon /> : <UnlockIcon />}
                {job.status === "OPEN" ? "Đóng vị trí" : "Mở lại vị trí"}
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onSelect={() => setConfirm("delete")}>
                <Trash2Icon /> Xoá vị trí
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {p.total > 0 && (
        <div className="mt-5 flex flex-wrap items-center gap-x-8 gap-y-3 rounded-xl border bg-card p-4">
          <div className="min-w-64 flex-1">
            <PipelineBar p={p} />
          </div>
          <div className="flex gap-6 text-sm">
            <div>
              <div className="text-xs text-muted-foreground">Điểm cao nhất</div>
              <div className="font-semibold tabular-nums">{fmtScore(p.top)}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Điểm trung bình</div>
              <div className="font-semibold tabular-nums">{fmtScore(p.avg)}</div>
            </div>
          </div>
          {p.NEW > 0 && (
            <Button
              variant="secondary"
              onClick={() => {
                const next = [...(list ?? [])]
                  .filter((a) => a.hrDecision === "NEW")
                  .sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1))
                setReviewOrder(next.map((a) => a.id))
                navigate(paths.review(job.id, next[0].id))
              }}
            >
              <CheckIcon data-icon="inline-start" />
              Duyệt {p.NEW} CV chưa xử lý
            </Button>
          )}
        </div>
      )}

      <Tabs
        value={tab}
        onValueChange={(v) => navigate(v === "requirements" ? paths.requirements(job.id) : paths.job(job.id), { replace: true })}
        className="mt-6"
      >
        <TabsList variant="line" className="w-full justify-start border-b">
          <TabsTrigger value="candidates" className="flex-none">
            Ứng viên {list && <span className="text-muted-foreground tabular-nums">{p.total}</span>}
          </TabsTrigger>
          <TabsTrigger value="requirements" className="flex-none">
            Yêu cầu
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="mt-4">
        {tab === "candidates" ? (
          <CandidateTable
            job={job}
            list={list}
            filter={filter}
            setFilter={setFilter}
            query={query}
            setQuery={setQuery}
            sort={sort}
            setSort={setSort}
            onVisibleChange={setVisibleIds}
            onOpen={openReview}
            onUpload={() => setUploadOpen(true)}
          />
        ) : (
          <RequirementsView job={job} onEdit={onEditJob} />
        )}
      </div>

      <UploadDialog
        open={uploadOpen}
        onOpenChange={setUploadOpen}
        queue={queue}
        onOpenCandidate={(id) => {
          setUploadOpen(false)
          openReview(id)
        }}
      />

      <AlertDialog open={confirm === "rescore"} onOpenChange={(o) => !o && setConfirm(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Chấm lại {p.total} CV?</AlertDialogTitle>
            <AlertDialogDescription>
              Mọi CV của vị trí này được chấm lại theo yêu cầu hiện tại. Quyết định và ghi chú của HR giữ nguyên. Khi AI
              đang bật, việc này có thể mất vài phút.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Huỷ</AlertDialogCancel>
            <AlertDialogAction onClick={rescoreAll}>
              <RotateCcwIcon data-icon="inline-start" />
              Chấm lại
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={confirm === "delete"} onOpenChange={(o) => !o && setConfirm(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xoá vị trí "{job.title}"?</AlertDialogTitle>
            <AlertDialogDescription>
              {p.total
                ? `${p.total} CV của vị trí này cũng bị xoá theo. Bạn không thể hoàn tác.`
                : "Bạn không thể hoàn tác thao tác này."}{" "}
              Nếu chỉ muốn ngừng nhận CV, hãy đóng vị trí.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Huỷ</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={deleteJob}>
              Xoá vị trí
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
