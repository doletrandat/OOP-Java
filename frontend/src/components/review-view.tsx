import { useCallback, useState } from "react"
import { AlertCircleIcon, AlertTriangleIcon } from "lucide-react"
import { toast } from "sonner"

import { DeleteCandidatesDialog, EditContactDialog, type ContactTarget } from "@/components/candidate-dialogs"
import { AnalysisPanel } from "@/components/review/analysis-panel"
import { CvPages } from "@/components/review/cv-pages"
import { DecisionBar, type NoteState } from "@/components/review/decision-bar"
import { ReviewToolbar } from "@/components/review/review-toolbar"
import { useApplicationDetail, useReviewSequence } from "@/components/review/use-review-data"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { applicationsApi, type HrDecision, type Job } from "@/lib/api"
import { DECISION_META, fmtScore } from "@/lib/format"
import { navigate, paths } from "@/lib/router"
import { useStore } from "@/lib/store"
import { cn } from "@/lib/utils"

/** Duyệt từng CV: ảnh CV bên trái, kết quả chấm và nút quyết định bên phải. */
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
  const { detail, error: loadError, setDetail } = useApplicationDetail(appId)
  const { seq, idx, prevId, nextId } = useReviewSequence(appId, order, list)
  const [pane, setPane] = useState<"analysis" | "cv">("analysis")
  const [rescoring, setRescoring] = useState(false)
  const [editing, setEditing] = useState<ContactTarget | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Ghi chú: nạp lại khi đổi hồ sơ hoặc khi chi tiết hồ sơ vừa tải xong
  const [note, setNote] = useState("")
  const [noteState, setNoteState] = useState<NoteState>("idle")
  const [noteSource, setNoteSource] = useState<string | null>(null)
  const currentSource = `${appId}:${detail?.id ?? ""}`
  if (noteSource !== currentSource) {
    setNoteSource(currentSource)
    setNote(summary?.hrNote ?? detail?.hrNote ?? "")
    setNoteState("idle")
  }

  const go = useCallback((id?: string) => id && navigate(paths.review(job.id, id), { replace: true }), [job.id])

  const decision: HrDecision = summary?.hrDecision ?? detail?.hrDecision ?? "NEW"
  const name = summary?.candidateName ?? detail?.candidateName ?? ""

  /** Bấm lại nút đang chọn thì bỏ quyết định (về "chưa xử lý"). */
  async function choose(d: HrDecision) {
    const target = decision === d ? "NEW" : d
    try {
      await decide(job.id, [appId], target)
    } catch {
      return
    }
    toast.success(target === "NEW" ? `Đã bỏ quyết định cho ${name}.` : `${name}: ${DECISION_META[target].label.toLowerCase()}.`)
  }

  async function saveNote() {
    if (note === (summary?.hrNote ?? "")) return
    setNoteState("saving")
    try {
      await applicationsApi.decide(appId, decision, note)
      patchApp(job.id, appId, { hrNote: note })
      setNoteState("saved")
    } catch (e) {
      toast.error((e as Error).message)
      setNoteState("idle")
    }
  }

  async function rescore() {
    setRescoring(true)
    try {
      const a = await applicationsApi.rescore(appId)
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
      await applicationsApi.remove(appId)
      removeApps(job.id, [appId])
      toast.success(`Đã xoá ${name}.`)
      if (goTo) go(goTo)
      else onBack()
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  const report = detail?.report

  return (
    <div className="flex h-[calc(100dvh-3rem)] flex-col">
      <ReviewToolbar
        appId={appId}
        name={name}
        summary={summary}
        detail={detail}
        position={idx}
        total={seq.length}
        prevId={prevId}
        nextId={nextId}
        rescoring={rescoring}
        onBack={onBack}
        onGo={go}
        onEdit={setEditing}
        onRescore={rescore}
        onDelete={() => setDeleting(true)}
      />

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
              ) : report ? (
                <AnalysisPanel report={report} />
              ) : (
                <Alert>
                  <AlertTriangleIcon />
                  <AlertTitle>CV này chưa được chấm</AlertTitle>
                  <AlertDescription>Mở menu ở góc trên và chọn "Chấm lại CV này".</AlertDescription>
                </Alert>
              )}
            </div>
          </ScrollArea>

          <DecisionBar
            decision={decision}
            note={note}
            noteState={noteState}
            onNoteChange={(n) => {
              setNote(n)
              setNoteState("idle")
            }}
            onNoteSave={saveNote}
            onChoose={choose}
          />
        </section>
      </div>

      <EditContactDialog
        target={editing}
        onClose={() => setEditing(null)}
        onSaved={(a) => {
          patchApp(job.id, a.id, { candidateName: a.candidateName, email: a.email, phone: a.phone })
          setDetail(a)
          setEditing(null)
        }}
      />
      <DeleteCandidatesDialog names={deleting ? [name] : null} onCancel={() => setDeleting(false)} onConfirm={remove} />
    </div>
  )
}
