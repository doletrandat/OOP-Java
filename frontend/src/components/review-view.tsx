import { useCallback, useEffect, useMemo, useState } from "react"
import {
  AlertCircleIcon,
  AlertTriangleIcon,
  ArrowLeftIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  DownloadIcon,
  ExternalLinkIcon,
  KeyboardIcon,
  MailIcon,
  MoreHorizontalIcon,
  PencilIcon,
  PhoneIcon,
  RefreshCwIcon,
  SparklesIcon,
  Trash2Icon,
  XIcon,
} from "lucide-react"
import { toast } from "sonner"

import { DeleteCandidatesDialog, EditContactDialog, type ContactTarget } from "@/components/candidate-dialogs"
import { CriterionStatusBadge, ScoreProgress, SkillStatusBadge } from "@/components/status"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"
import { Input } from "@/components/ui/input"
import { Kbd } from "@/components/ui/kbd"
import { Label } from "@/components/ui/label"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { api, post, type Application, type HrDecision, type Job, type MatchReport } from "@/lib/api"
import { downloadCv, forgetCv, openCvInNewTab, prefetchCv, useCvPages } from "@/lib/cv-cache"
import {
  AI_STATUS_LABEL,
  criterionTypeLabel,
  DECISION_META,
  fmtScore,
  fmtTime,
  initials,
  scoreText,
  scoreTone,
  WEIGHT_LABEL,
} from "@/lib/format"
import { navigate, paths } from "@/lib/router"
import { useStore } from "@/lib/store"
import { cn } from "@/lib/utils"

/* Bộ nhớ đệm chi tiết hồ sơ để chuyển qua lại không phải chờ */
const details = new Map<string, Application>()
async function fetchDetail(id: string, fresh = false): Promise<Application> {
  if (!fresh && details.has(id)) return details.get(id)!
  const a = await api<Application>(`/api/admin/applications/${id}`)
  details.set(id, a)
  return a
}

const AUTO_KEY = "cvr_auto_advance"
function readAuto() {
  try {
    return localStorage.getItem(AUTO_KEY) !== "0"
  } catch {
    return true
  }
}

export function ReviewView({
  job,
  appId,
  order,
  onBack,
}: {
  job: Job
  appId: string
  order: string[]
  onBack: () => void
}) {
  const { apps, decide, patchApp, removeApps } = useStore()
  const list = apps[job.id]
  const summary = list?.find((a) => a.id === appId)
  const [detail, setDetail] = useState<Application | null>(details.get(appId) ?? null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [note, setNote] = useState("")
  const [noteState, setNoteState] = useState<"idle" | "saving" | "saved">("idle")
  const [auto, setAuto] = useState(readAuto)
  const [pane, setPane] = useState<"analysis" | "cv">("analysis")
  const [rescoring, setRescoring] = useState(false)
  const [editing, setEditing] = useState<ContactTarget | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Thứ tự duyệt: theo danh sách đang lọc lúc mở; nếu không có thì theo điểm
  const seq = useMemo(() => {
    const ids = new Set(list?.map((a) => a.id))
    const o = order.filter((id) => ids.has(id))
    return o.includes(appId) ? o : [...(list ?? [])].sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1)).map((a) => a.id)
  }, [order, list, appId])
  const idx = seq.indexOf(appId)
  const prevId = idx > 0 ? seq[idx - 1] : undefined
  const nextId = idx >= 0 && idx < seq.length - 1 ? seq[idx + 1] : undefined

  useEffect(() => {
    let cancelled = false
    setLoadError(null)
    setDetail(details.get(appId) ?? null)
    fetchDetail(appId)
      .then((a) => !cancelled && setDetail(a))
      .catch((e: Error) => !cancelled && setLoadError(e.message))
    return () => {
      cancelled = true
    }
  }, [appId])

  // Tải sẵn hồ sơ kế tiếp
  useEffect(() => {
    if (!nextId) return
    const t = setTimeout(() => {
      fetchDetail(nextId).catch(() => {})
      prefetchCv(nextId)
    }, 400)
    return () => clearTimeout(t)
  }, [nextId])

  useEffect(() => {
    setNote(summary?.hrNote ?? detail?.hrNote ?? "")
    setNoteState("idle")
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appId, detail?.id])

  const go = useCallback((id?: string) => id && navigate(paths.review(job.id, id), { replace: true }), [job.id])

  const decision: HrDecision = summary?.hrDecision ?? detail?.hrDecision ?? "NEW"
  const name = summary?.candidateName ?? detail?.candidateName ?? ""

  const choose = useCallback(
    async (d: HrDecision) => {
      const prev = decision
      const target = prev === d ? "NEW" : d
      try {
        await decide(job.id, [appId], target)
      } catch {
        return
      }
      details.delete(appId)
      toast.success(target === "NEW" ? `Đã bỏ quyết định cho ${name}.` : `${name}: ${DECISION_META[target].label.toLowerCase()}.`, {
        duration: 8000,
        action: { label: "Hoàn tác", onClick: () => decide(job.id, [appId], prev).catch(() => {}) },
      })
      if (!auto || target === "NEW") return
      // Sang CV chưa xử lý kế tiếp trong thứ tự duyệt
      const pending = (id: string) => id !== appId && (list?.find((a) => a.id === id)?.hrDecision ?? "NEW") === "NEW"
      const after = seq.slice(idx + 1).find(pending) ?? seq.slice(0, idx).find(pending)
      if (after) go(after)
      else toast.info("Bạn đã xử lý hết CV trong danh sách này.")
    },
    [decision, decide, job.id, appId, name, auto, list, seq, idx, go]
  )

  async function saveNote() {
    if (note === (summary?.hrNote ?? "")) return
    setNoteState("saving")
    try {
      await api(`/api/admin/applications/${appId}/decision`, post({ decision, note }))
      patchApp(job.id, appId, { hrNote: note })
      details.delete(appId)
      setNoteState("saved")
    } catch (e) {
      toast.error((e as Error).message)
      setNoteState("idle")
    }
  }

  async function rescore() {
    setRescoring(true)
    try {
      const a = await api<Application>(`/api/admin/applications/${appId}/rescore`, post())
      details.set(appId, a)
      setDetail(a)
      if (a.report) {
        patchApp(job.id, appId, {
          finalScore: a.report.finalScore,
          keywordScore: a.report.keywordScore,
          aiScore: a.report.aiScore,
          aiStatus: a.report.aiStatus,
        })
      }
      toast.success(`Đã chấm lại: ${fmtScore(a.report?.finalScore)} điểm.`)
    } catch (e) {
      toast.error((e as Error).message)
    } finally {
      setRescoring(false)
    }
  }

  async function remove() {
    setDeleting(false)
    const goTo = nextId ?? prevId
    try {
      await api(`/api/admin/applications/${appId}`, { method: "DELETE" })
      removeApps(job.id, [appId])
      forgetCv(appId)
      details.delete(appId)
      toast.success(`Đã xoá ${name}.`)
      if (goTo) go(goTo)
      else onBack()
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  // Phím tắt
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return
      const t = e.target as HTMLElement
      if (/INPUT|TEXTAREA|SELECT/.test(t.tagName) || t.isContentEditable) return
      if (document.querySelector("[role=dialog],[role=alertdialog],[role=menu]")) return
      const k = e.key.toLowerCase()
      if (k === "j" || e.key === "ArrowRight" || e.key === "ArrowDown") go(nextId)
      else if (k === "k" || e.key === "ArrowLeft" || e.key === "ArrowUp") go(prevId)
      else if (k === "s") choose("SHORTLISTED")
      else if (k === "r") choose("REJECTED")
      else if (k === "n") document.getElementById("review-note")?.focus()
      else if (e.key === "Escape") onBack()
      else return
      e.preventDefault()
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [go, nextId, prevId, choose, onBack])

  const r = detail?.report

  return (
    <div className="flex h-[calc(100dvh-3rem)] flex-col">
      {/* Thanh công cụ */}
      <div className="flex items-center gap-2 border-b px-3 py-2 sm:px-4">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon" onClick={onBack} aria-label="Về danh sách">
              <ArrowLeftIcon />
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            Về danh sách <Kbd>Esc</Kbd>
          </TooltipContent>
        </Tooltip>
        <Avatar className="hidden size-9 sm:flex">
          <AvatarFallback>{initials(name || "?")}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="flex min-w-0 items-center gap-1">
            <h1 className="truncate text-base font-semibold">{name || <Skeleton className="h-5 w-40" />}</h1>
            {summary && (
              <Button
                variant="ghost"
                size="icon-xs"
                aria-label="Sửa thông tin ứng viên"
                onClick={() => setEditing(summary)}
              >
                <PencilIcon />
              </Button>
            )}
          </div>
          <div className="flex min-w-0 items-center gap-3 truncate text-xs text-muted-foreground">
            {summary?.email && (
              <a href={`mailto:${summary.email}`} className="inline-flex items-center gap-1 hover:text-foreground">
                <MailIcon className="size-3" />
                {summary.email}
              </a>
            )}
            {summary?.phone && (
              <a href={`tel:${summary.phone}`} className="hidden items-center gap-1 hover:text-foreground sm:inline-flex">
                <PhoneIcon className="size-3" />
                {summary.phone}
              </a>
            )}
            {detail && <span className="hidden md:inline">Tải lên {fmtTime(detail.submittedAt)}</span>}
          </div>
        </div>

        <span className="hidden text-sm text-muted-foreground tabular-nums sm:inline">
          {idx + 1} / {seq.length}
        </span>
        <div className="flex">
          <NavButton label="Hồ sơ trước" kbd="K" disabled={!prevId} onClick={() => go(prevId)}>
            <ChevronLeftIcon />
          </NavButton>
          <NavButton label="Hồ sơ tiếp" kbd="J" disabled={!nextId} onClick={() => go(nextId)}>
            <ChevronRightIcon />
          </NavButton>
        </div>
        <ShortcutHelp />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label="Thao tác khác">
              <MoreHorizontalIcon />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-52">
            <DropdownMenuItem onSelect={() => openCvInNewTab(appId)}>
              <ExternalLinkIcon /> Mở CV trong tab mới
            </DropdownMenuItem>
            <DropdownMenuItem
              onSelect={() =>
                downloadCv(appId, detail?.cvFileName ?? "CV.pdf").catch((e: Error) => toast.error(e.message))
              }
            >
              <DownloadIcon /> Tải file PDF
            </DropdownMenuItem>
            <DropdownMenuItem disabled={rescoring} onSelect={rescore}>
              <RefreshCwIcon /> Chấm lại CV này
            </DropdownMenuItem>
            <DropdownMenuItem disabled={!summary} onSelect={() => summary && setEditing(summary)}>
              <PencilIcon /> Sửa thông tin
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" onSelect={() => setDeleting(true)}>
              <Trash2Icon /> Xoá ứng viên
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Chọn khung trên màn hình hẹp */}
      <div className="border-b px-3 py-2 lg:hidden">
        <Tabs value={pane} onValueChange={(v) => setPane(v as "analysis" | "cv")}>
          <TabsList className="w-full">
            <TabsTrigger value="analysis">Đánh giá</TabsTrigger>
            <TabsTrigger value="cv">CV</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <div className="grid min-h-0 flex-1 lg:grid-cols-[minmax(0,1fr)_minmax(24rem,30rem)]">
        <section aria-label="CV" className={cn("min-h-0 bg-muted/60", pane === "cv" ? "block" : "hidden lg:block")}>
          <CvPages appId={appId} />
        </section>

        <section
          aria-label="Đánh giá"
          className={cn("min-h-0 flex-col border-l bg-background", pane === "analysis" ? "flex" : "hidden lg:flex")}
        >
          <ScrollArea className="min-h-0 flex-1">
            <div className="p-4">
              {loadError ? (
                <Alert variant="destructive">
                  <AlertCircleIcon />
                  <AlertTitle>Không tải được hồ sơ</AlertTitle>
                  <AlertDescription>{loadError}</AlertDescription>
                </Alert>
              ) : !detail ? (
                <div className="flex flex-col gap-3">
                  <Skeleton className="h-32 w-full" />
                  <Skeleton className="h-8 w-full" />
                  <Skeleton className="h-48 w-full" />
                </div>
              ) : r ? (
                <Analysis report={r} />
              ) : (
                <Alert>
                  <AlertTriangleIcon />
                  <AlertTitle>CV này chưa được chấm</AlertTitle>
                  <AlertDescription>Mở menu ở góc trên và chọn "Chấm lại CV này".</AlertDescription>
                </Alert>
              )}
            </div>
          </ScrollArea>

          {/* Quyết định */}
          <div className="flex flex-col gap-3 border-t bg-muted/30 p-4">
            <div className="flex items-center gap-2">
              <Input
                id="review-note"
                placeholder="Ghi chú nội bộ…"
                maxLength={500}
                autoComplete="off"
                value={note}
                onChange={(e) => {
                  setNote(e.target.value)
                  setNoteState("idle")
                }}
                onBlur={saveNote}
                onKeyDown={(e) => {
                  if (e.key === "Enter") (e.target as HTMLInputElement).blur()
                  if (e.key === "Escape") {
                    setNote(summary?.hrNote ?? "")
                    ;(e.target as HTMLInputElement).blur()
                  }
                }}
              />
              <span className="w-14 shrink-0 text-xs text-muted-foreground" aria-live="polite">
                {noteState === "saving" ? "Đang lưu…" : noteState === "saved" ? "Đã lưu" : ""}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                size="lg"
                aria-pressed={decision === "REJECTED"}
                onClick={() => choose("REJECTED")}
                className={cn(
                  decision === "REJECTED" &&
                    "border-destructive/40 bg-destructive/10 text-destructive hover:bg-destructive/15 dark:bg-destructive/20"
                )}
              >
                <XIcon data-icon="inline-start" />
                {decision === "REJECTED" ? "Đã từ chối" : "Từ chối"}
                <Kbd className="ml-auto hidden sm:inline-flex">R</Kbd>
              </Button>
              <Button
                size="lg"
                aria-pressed={decision === "SHORTLISTED"}
                onClick={() => choose("SHORTLISTED")}
                className={cn(
                  decision === "SHORTLISTED" && "bg-success text-white hover:bg-success/90 dark:text-background"
                )}
              >
                <CheckIcon data-icon="inline-start" />
                {decision === "SHORTLISTED" ? "Đã mời phỏng vấn" : "Mời phỏng vấn"}
                <Kbd className="ml-auto hidden bg-primary-foreground/20 text-current sm:inline-flex">S</Kbd>
              </Button>
            </div>
            <div className="flex items-center gap-2">
              <Checkbox
                id="auto-advance"
                checked={auto}
                onCheckedChange={(c) => {
                  setAuto(!!c)
                  try {
                    localStorage.setItem(AUTO_KEY, c ? "1" : "0")
                  } catch {
                    /* bỏ qua */
                  }
                }}
              />
              <Label htmlFor="auto-advance" className="text-xs font-normal text-muted-foreground">
                Quyết định xong thì sang CV chưa xử lý tiếp theo
              </Label>
            </div>
          </div>
        </section>
      </div>

      <EditContactDialog
        target={editing}
        onClose={() => setEditing(null)}
        onSaved={(a) => {
          patchApp(job.id, a.id, { candidateName: a.candidateName, email: a.email, phone: a.phone })
          details.set(a.id, a)
          setDetail(a)
          setEditing(null)
        }}
      />
      <DeleteCandidatesDialog names={deleting ? [name] : null} onCancel={() => setDeleting(false)} onConfirm={remove} />
    </div>
  )
}

function NavButton({
  label,
  kbd,
  disabled,
  onClick,
  children,
}: {
  label: string
  kbd: string
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={label} disabled={disabled} onClick={onClick}>
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        {label} <Kbd>{kbd}</Kbd>
      </TooltipContent>
    </Tooltip>
  )
}

function ShortcutHelp() {
  const rows: [string, string][] = [
    ["J  /  →", "Hồ sơ tiếp"],
    ["K  /  ←", "Hồ sơ trước"],
    ["S", "Mời phỏng vấn"],
    ["R", "Từ chối"],
    ["N", "Viết ghi chú"],
    ["Esc", "Về danh sách"],
  ]
  return (
    <HoverCard openDelay={150}>
      <HoverCardTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Phím tắt" className="hidden md:inline-flex">
          <KeyboardIcon />
        </Button>
      </HoverCardTrigger>
      <HoverCardContent align="end" className="w-56">
        <div className="mb-2 text-sm font-medium">Phím tắt</div>
        <ul className="flex flex-col gap-1.5 text-sm">
          {rows.map(([k, l]) => (
            <li key={l} className="flex items-center justify-between gap-3 text-muted-foreground">
              {l}
              <span className="flex gap-1">
                {k.split("  /  ").map((x) => (
                  <Kbd key={x}>{x}</Kbd>
                ))}
              </span>
            </li>
          ))}
        </ul>
      </HoverCardContent>
    </HoverCard>
  )
}

function CvPages({ appId }: { appId: string }) {
  const cv = useCvPages(appId)
  if (!cv) return null
  const loading = !cv.error && (!cv.total || cv.pages.length < cv.total)
  return (
    <ScrollArea className="h-full">
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-4 p-4 sm:p-6">
        {cv.error && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertTitle>Không mở được CV</AlertTitle>
            <AlertDescription>{cv.error}</AlertDescription>
          </Alert>
        )}
        {cv.pages.map((src, i) => (
          <img
            key={src}
            src={src}
            alt={`Trang ${i + 1} của ${cv.fileName}`}
            className="w-full rounded-md bg-white shadow-sm ring-1 ring-black/10"
          />
        ))}
        {loading && (
          <div className="flex w-full flex-col items-center gap-3">
            <Skeleton className="aspect-[1/1.414] w-full rounded-md" />
            <span className="flex items-center gap-2 text-xs text-muted-foreground">
              <Spinner />
              {cv.total > 1 ? `Đang tải trang ${cv.pages.length + 1}/${cv.total}…` : "Đang tải CV…"}
            </span>
          </div>
        )}
        {!loading && !cv.error && (
          <p className="text-xs text-muted-foreground tabular-nums">
            {cv.fileName} · {cv.total} trang · {cv.sizeKb} KB
          </p>
        )}
      </div>
    </ScrollArea>
  )
}

/* ---------- Khung đánh giá ---------- */

const SKILL_ORDER = { MISSING: 0, UNCERTAIN: 1, MATCHED: 2 } as const
const CRIT_ORDER = { NOT_MET: 0, UNKNOWN: 1, PARTIAL: 2, MET: 3 } as const

function Analysis({ report: r }: { report: MatchReport }) {
  const skills = [...(r.skillMatches ?? [])].sort(
    (a, b) =>
      (a.weight === "REQUIRED" ? 0 : 1) - (b.weight === "REQUIRED" ? 0 : 1) ||
      SKILL_ORDER[a.status] - SKILL_ORDER[b.status]
  )
  const criteria = [...(r.criteriaResults ?? [])].sort((a, b) => CRIT_ORDER[a.status] - CRIT_ORDER[b.status])
  const matched = skills.filter((s) => s.status === "MATCHED").length
  const met = criteria.filter((c) => c.status === "MET").length
  const missingRequired = skills.filter((s) => s.weight === "REQUIRED" && s.status === "MISSING")

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end gap-4 rounded-xl border bg-card p-4">
        <div>
          <div className="text-xs text-muted-foreground">Điểm tổng</div>
          <div className={cn("text-4xl font-bold tracking-tight tabular-nums", scoreText[scoreTone(r.finalScore)])}>
            {fmtScore(r.finalScore)}
            <span className="ml-1 text-sm font-medium text-muted-foreground">/100</span>
          </div>
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-2 pb-1">
          <MiniScore label="Từ khoá" value={r.keywordScore} />
          <MiniScore label="AI" value={r.aiStatus === "OK" ? r.aiScore : null} note={r.aiStatus !== "OK" ? AI_STATUS_LABEL[r.aiStatus] : undefined} />
          {criteria.length > 0 && <MiniScore label="Tiêu chí" value={r.criteriaScore >= 0 ? r.criteriaScore : null} />}
        </div>
      </div>

      {missingRequired.length > 0 && (
        <Alert className="border-destructive/30 bg-destructive/5">
          <AlertTriangleIcon className="text-destructive" />
          <AlertTitle>Thiếu {missingRequired.length} kỹ năng bắt buộc</AlertTitle>
          <AlertDescription>{missingRequired.map((s) => s.skillName).join(", ")}</AlertDescription>
        </Alert>
      )}

      <Tabs defaultValue="summary">
        <TabsList className="w-full">
          <TabsTrigger value="summary">Tóm tắt</TabsTrigger>
          <TabsTrigger value="skills">
            Kỹ năng <span className="text-muted-foreground tabular-nums">{matched}/{skills.length}</span>
          </TabsTrigger>
          {criteria.length > 0 && (
            <TabsTrigger value="criteria">
              Tiêu chí <span className="text-muted-foreground tabular-nums">{met}/{criteria.length}</span>
            </TabsTrigger>
          )}
        </TabsList>

        <TabsContent value="summary" className="mt-3 flex flex-col gap-4">
          {r.aiStatus === "OK" ? (
            <>
              {r.aiSummary && (
                <div>
                  <h3 className="mb-1.5 flex items-center gap-1.5 text-sm font-medium">
                    <SparklesIcon className="size-3.5 text-primary" /> Nhận xét của AI
                  </h3>
                  <p className="text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground">{r.aiSummary}</p>
                </div>
              )}
              {(r.aiHighlights?.length ?? 0) > 0 && <Points title="Điểm mạnh" items={r.aiHighlights!} good />}
              {(r.aiConcerns?.length ?? 0) > 0 && <Points title="Nên hỏi thêm khi phỏng vấn" items={r.aiConcerns!} />}
              {(r.aiDimensions?.length ?? 0) > 0 && (
                <div>
                  <h3 className="mb-2 text-sm font-medium">Điểm theo từng mặt</h3>
                  <div className="flex flex-col divide-y rounded-lg border">
                    {r.aiDimensions!.map((d) => (
                      <div key={d.name} className="p-3">
                        <div className="flex items-baseline justify-between gap-2 text-sm">
                          <span>{d.name}</span>
                          <span className="font-semibold tabular-nums">{fmtScore(d.score)}</span>
                        </div>
                        <ScoreProgress value={d.score} className="my-1.5" />
                        {d.comment && <p className="text-xs text-muted-foreground">{d.comment}</p>}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            <Alert>
              <AlertTriangleIcon />
              <AlertTitle>Chưa có nhận xét của AI ({AI_STATUS_LABEL[r.aiStatus] ?? r.aiStatus})</AlertTitle>
              <AlertDescription>
                {r.aiError && <p>{r.aiError}</p>}
                {r.aiStatus === "NOT_CONFIGURED" ? (
                  <p>
                    Điểm hiện chỉ dựa trên từ khoá và tiêu chí. Đặt <code>AI_API_KEY</code> trong{" "}
                    <code>config.properties</code> rồi khởi động lại server để bật AI.
                  </p>
                ) : (
                  <p>Thử "Chấm lại CV này" trong menu ở góc trên.</p>
                )}
              </AlertDescription>
            </Alert>
          )}
          <p className="text-xs text-muted-foreground">
            Chấm lúc {fmtTime(r.scoredAt)}. Điểm chỉ để tham khảo, HR là người quyết định.
          </p>
        </TabsContent>

        <TabsContent value="skills" className="mt-3">
          <ul className="flex flex-col divide-y rounded-lg border">
            {skills.map((s) => (
              <li key={s.skillName} className="p-3">
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="mr-auto font-medium">{s.skillName}</span>
                  {s.weight === "REQUIRED" && <Badge variant="outline">Bắt buộc</Badge>}
                  <SkillStatusBadge status={s.status} />
                </div>
                {s.evidence && <Quote text={s.evidence} />}
                {s.aiNote && (
                  <p className="mt-1.5 flex gap-1.5 text-xs text-muted-foreground">
                    <SparklesIcon className="mt-0.5 size-3 shrink-0" />
                    {s.aiNote}
                  </p>
                )}
                {!s.evidence && !s.aiNote && (
                  <p className="mt-1 text-xs text-muted-foreground">Không thấy đoạn nào trong CV nhắc tới.</p>
                )}
              </li>
            ))}
          </ul>
        </TabsContent>

        {criteria.length > 0 && (
          <TabsContent value="criteria" className="mt-3">
            <ul className="flex flex-col divide-y rounded-lg border">
              {criteria.map((c) => (
                <li key={c.name + c.requirement} className="p-3">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="mr-auto font-medium">{c.name}</span>
                    {c.source === "AI" && (
                      <Badge variant={c.inferred ? "warning" : "info"}>{c.inferred ? "AI suy ra" : "AI tìm thấy"}</Badge>
                    )}
                    <CriterionStatusBadge status={c.status} />
                  </div>
                  <dl className="mt-1.5 grid grid-cols-[5rem_1fr] gap-x-2 gap-y-0.5 text-xs">
                    <dt className="text-muted-foreground">Yêu cầu</dt>
                    <dd>
                      {c.requirement}{" "}
                      <span className="text-muted-foreground">
                        ({criterionTypeLabel(c.type).toLowerCase()}, {(WEIGHT_LABEL[c.weight] ?? "").toLowerCase()})
                      </span>
                    </dd>
                    <dt className="text-muted-foreground">Trong CV</dt>
                    <dd>{c.actual || <span className="text-muted-foreground">Không ghi</span>}</dd>
                  </dl>
                  {c.note && <p className="mt-1 text-xs text-muted-foreground">{c.note}</p>}
                  {c.evidence && <Quote text={c.evidence} />}
                </li>
              ))}
            </ul>
          </TabsContent>
        )}
      </Tabs>
    </div>
  )
}

function MiniScore({ label, value, note }: { label: string; value: number | null; note?: string }) {
  return (
    <div className="grid grid-cols-[3.5rem_1fr_2rem] items-center gap-2 text-xs">
      <span className="text-muted-foreground">{label}</span>
      {note ? <span className="truncate text-muted-foreground">{note}</span> : <ScoreProgress value={value} />}
      <span className="text-right font-medium tabular-nums">{fmtScore(value)}</span>
    </div>
  )
}

function Quote({ text }: { text: string }) {
  return (
    <blockquote className="mt-2 rounded-r-md border-l-2 bg-muted/60 px-2.5 py-1.5 text-xs leading-relaxed text-muted-foreground">
      "{text}"
    </blockquote>
  )
}

function Points({ title, items, good }: { title: string; items: string[]; good?: boolean }) {
  const Icon = good ? CheckIcon : AlertTriangleIcon
  return (
    <div>
      <h3 className="mb-1.5 text-sm font-medium">{title}</h3>
      <ul className="flex flex-col gap-1.5">
        {items.map((t) => (
          <li key={t} className="flex gap-2 text-sm text-muted-foreground">
            <Icon className={cn("mt-0.5 size-3.5 shrink-0", good ? "text-success" : "text-warning")} />
            <span>{t}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
