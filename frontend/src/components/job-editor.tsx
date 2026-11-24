import { useState } from "react"
import { ArrowDownIcon, ArrowUpIcon, PlusIcon } from "lucide-react"
import { toast } from "sonner"

import { CriterionCard } from "@/components/job-editor/criterion-card"
import {
  CRITERION_PRESETS,
  draftFrom,
  moveSkill,
  newCriterion,
  snapshot,
  toPayload,
  validate,
  type Draft,
} from "@/components/job-editor/job-draft"
import { TagInput } from "@/components/job-editor/tag-input"
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
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { jobsApi, type Criterion, type Job } from "@/lib/api"

/** Hộp thoại tạo / sửa vị trí: thông tin chung bên trái, kỹ năng và tiêu chí bên phải. */
export function JobEditor({
  open,
  job,
  onOpenChange,
  onSaved,
}: {
  open: boolean
  job: Job | null
  onOpenChange: (open: boolean) => void
  onSaved: (job: Job, isNew: boolean) => void
}) {
  const [draft, setDraft] = useState<Draft>(() => draftFrom(job))
  const [initial, setInitial] = useState("")
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, boolean>>({})
  const [confirmClose, setConfirmClose] = useState(false)

  // Mỗi lần mở (hoặc đổi vị trí khi đang mở) thì nạp lại form từ dữ liệu vị trí
  const [loaded, setLoaded] = useState<{ open: boolean; job: Job | null }>({ open: false, job: null })
  if (loaded.open !== open || loaded.job !== job) {
    setLoaded({ open, job })
    if (open) {
      const d = draftFrom(job)
      setDraft(d)
      setInitial(snapshot(d))
      setErrors({})
      setBusy(false)
    }
  }

  const dirty = open && snapshot(draft) !== initial
  const set = <K extends keyof Draft>(k: K, v: Draft[K]) => setDraft((d) => ({ ...d, [k]: v }))
  const patchCriterion = (key: number, p: Partial<Criterion>) =>
    setDraft((d) => ({ ...d, criteria: d.criteria.map((r) => (r.key === key ? { ...r, ...p } : r)) }))
  const removeCriterion = (key: number) =>
    setDraft((d) => ({ ...d, criteria: d.criteria.filter((r) => r.key !== key) }))
  const move = (name: string, to: "required" | "preferred") => setDraft((d) => moveSkill(d, name, to))

  function requestClose() {
    if (dirty && !busy) setConfirmClose(true)
    else onOpenChange(false)
  }

  async function save() {
    const { errors: errs, message } = validate(draft)
    setErrors(errs)
    if (message) {
      document.getElementById(`je-${Object.keys(errs)[0]}`)?.focus()
      toast.error(message)
      return
    }

    const payload = toPayload(draft)
    setBusy(true)
    try {
      const saved = job ? await jobsApi.update(job.id, payload) : await jobsApi.create(payload)
      toast.success(job ? "Đã lưu thay đổi." : `Đã tạo vị trí "${payload.title}".`, {
        description: job && job.applicationCount ? "Điểm cũ chưa đổi. Bấm Chấm lại tất cả để cập nhật." : undefined,
      })
      onSaved(saved, !job)
    } catch (e) {
      toast.error((e as Error).message)
      setBusy(false)
    }
  }

  const addCriterion = (preset?: Partial<Criterion>) => {
    const row = newCriterion(preset)
    setDraft((d) => ({ ...d, criteria: [...d.criteria, row] }))
    requestAnimationFrame(() =>
      document.getElementById(preset?.name ? `je-req-${row.key}` : `je-name-${row.key}`)?.focus()
    )
  }

  const presets = CRITERION_PRESETS.filter((p) => !draft.criteria.some((c) => c.type === p.type))

  return (
    <>
      <Dialog open={open} onOpenChange={(o) => (o ? onOpenChange(true) : requestClose())}>
        <DialogContent
          className="flex h-[min(92dvh,920px)] w-[calc(100%-1rem)] flex-col gap-0 p-0 sm:max-w-6xl"
          onEscapeKeyDown={(e) => {
            if (dirty) {
              e.preventDefault()
              setConfirmClose(true)
            }
          }}
        >
          <DialogHeader className="border-b px-6 py-4">
            <DialogTitle className="text-lg">{job ? `Sửa vị trí: ${job.title}` : "Tạo vị trí mới"}</DialogTitle>
            <DialogDescription>
              CV tải lên được so khớp với kỹ năng và tiêu chí bên phải. AI đọc thêm phần mô tả công việc khi đánh giá.
            </DialogDescription>
          </DialogHeader>

          <div className="grid min-h-0 flex-1 overflow-y-auto overscroll-contain lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:overflow-hidden">
            {/* Cột trái: thông tin chung và mô tả */}
            <section className="flex min-w-0 flex-col gap-5 p-6 lg:overflow-y-auto">
              <h3 className="text-sm font-semibold text-muted-foreground">Thông tin vị trí</h3>
              <Field data-invalid={errors.title || undefined}>
                <FieldLabel htmlFor="je-title">Tên vị trí</FieldLabel>
                <Input
                  id="je-title"
                  autoComplete="off"
                  autoFocus={!job}
                  placeholder="VD: Kế toán tổng hợp…"
                  value={draft.title}
                  aria-invalid={errors.title || undefined}
                  onChange={(e) => set("title", e.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="je-dept">Phòng ban, công ty</FieldLabel>
                <Input
                  id="je-dept"
                  autoComplete="off"
                  placeholder="VD: Phòng Tài chính Kế toán…"
                  value={draft.department}
                  onChange={(e) => set("department", e.target.value)}
                />
              </Field>
              <Field className="flex min-h-80 flex-1 flex-col">
                <div className="flex items-baseline justify-between gap-2">
                  <FieldLabel htmlFor="je-desc">Mô tả công việc</FieldLabel>
                  <span className="text-xs text-muted-foreground tabular-nums">
                    {draft.description.length.toLocaleString("vi-VN")} ký tự
                  </span>
                </div>
                <Textarea
                  id="je-desc"
                  className="min-h-72 flex-1 resize-none leading-relaxed"
                  placeholder="Dán mô tả công việc vào đây: nhiệm vụ, yêu cầu, quyền lợi…"
                  value={draft.description}
                  onChange={(e) => set("description", e.target.value)}
                />
                <FieldDescription>
                  Không bắt buộc. Mô tả càng cụ thể thì AI đánh giá CV càng sát với công việc.
                </FieldDescription>
              </Field>
            </section>

            {/* Cột phải: kỹ năng và tiêu chí */}
            <section className="flex min-w-0 flex-col gap-8 border-t bg-muted/30 p-6 lg:overflow-y-auto lg:border-t-0 lg:border-l">
              <div className="flex flex-col gap-4">
                <div>
                  <h3 className="font-semibold">Kỹ năng cần tìm</h3>
                  <p className="mt-0.5 text-sm text-muted-foreground">
                    Gõ rồi nhấn Enter, hoặc dán cả danh sách cách nhau bằng dấu phẩy. Bấm mũi tên trên thẻ để chuyển giữa
                    bắt buộc và lợi thế.
                  </p>
                </div>
                <TagInput
                  id="je-skills"
                  label="Bắt buộc"
                  hint="Thiếu kỹ năng này thì điểm giảm mạnh."
                  tags={draft.required}
                  other={draft.preferred}
                  onChange={(v) => set("required", v)}
                  invalid={errors.skills}
                  variant="info"
                  moveLabel="Chuyển sang lợi thế"
                  MoveIcon={ArrowDownIcon}
                  onMove={(n) => move(n, "preferred")}
                />
                <TagInput
                  id="je-skills-pref"
                  label="Lợi thế"
                  hint="Có thì cộng điểm, không có cũng không sao."
                  tags={draft.preferred}
                  other={draft.required}
                  onChange={(v) => set("preferred", v)}
                  variant="secondary"
                  moveLabel="Chuyển sang bắt buộc"
                  MoveIcon={ArrowUpIcon}
                  onMove={(n) => move(n, "required")}
                />
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <h3 className="font-semibold">
                    Tiêu chí bổ sung{" "}
                    <span className="font-normal text-muted-foreground">
                      {draft.criteria.length ? `(${draft.criteria.length})` : "(không bắt buộc)"}
                    </span>
                  </h3>
                  <p className="mt-0.5 text-sm text-muted-foreground">
                    Kinh nghiệm, bằng cấp, ngoại ngữ, chứng chỉ. Mỗi tiêu chí được đối chiếu với CV và kèm đoạn trích để
                    bạn kiểm tra.
                  </p>
                </div>

                {draft.criteria.map((c, i) => (
                  <CriterionCard
                    key={c.key}
                    row={c}
                    index={i}
                    errors={errors}
                    onChange={(p) => patchCriterion(c.key, p)}
                    onRemove={() => removeCriterion(c.key)}
                  />
                ))}

                <div className="flex flex-wrap items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => addCriterion()}>
                    <PlusIcon data-icon="inline-start" />
                    Thêm tiêu chí
                  </Button>
                  {presets.length > 0 && <span className="text-xs text-muted-foreground">hoặc thêm nhanh:</span>}
                  {presets.map((p) => (
                    <Button key={p.name} variant="ghost" size="sm" onClick={() => addCriterion(p)}>
                      <PlusIcon data-icon="inline-start" />
                      {p.name}
                    </Button>
                  ))}
                </div>
              </div>
            </section>
          </div>

          <DialogFooter className="m-0 flex-row items-center rounded-b-xl border-t bg-muted/50 px-6 py-3 sm:justify-between">
            <span className="hidden text-xs text-muted-foreground sm:inline">
              {draft.required.length + draft.preferred.length} kỹ năng · {draft.criteria.length} tiêu chí
              {dirty ? " · Có thay đổi chưa lưu" : ""}
            </span>
            <div className="ml-auto flex gap-2">
              <Button variant="outline" onClick={requestClose}>
                Huỷ
              </Button>
              <Button onClick={save} disabled={busy}>
                {busy && <Spinner data-icon="inline-start" />}
                {busy ? "Đang lưu…" : job ? "Lưu thay đổi" : "Tạo vị trí"}
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={confirmClose} onOpenChange={setConfirmClose}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Bỏ các thay đổi chưa lưu?</AlertDialogTitle>
            <AlertDialogDescription>Những gì bạn vừa sửa trong vị trí này sẽ mất.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Tiếp tục sửa</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => {
                setConfirmClose(false)
                onOpenChange(false)
              }}
            >
              Bỏ thay đổi
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
