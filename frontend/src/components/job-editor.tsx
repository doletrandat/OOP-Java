import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react"
import { ArrowDownIcon, ArrowUpIcon, PlusIcon, Trash2Icon, XIcon } from "lucide-react"
import { toast } from "sonner"

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
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { api, post, put, type Criterion, type CriterionType, type Job, type SkillWeight } from "@/lib/api"
import { CRITERION_TYPES, WEIGHT_LABEL } from "@/lib/format"
import { cn } from "@/lib/utils"

type CriterionRow = Criterion & { key: number }
let seq = 0
const newCriterion = (c?: Partial<Criterion>): CriterionRow => ({
  key: ++seq,
  name: c?.name ?? "",
  type: c?.type ?? "EXPERIENCE",
  requirement: c?.requirement ?? "",
  weight: c?.weight ?? 2,
})

/** Gợi ý nhanh cho tiêu chí hay dùng, bấm là thêm. */
const CRITERION_PRESETS: Partial<Criterion>[] = [
  { name: "Kinh nghiệm", type: "EXPERIENCE", requirement: "1" },
  { name: "Trình độ", type: "EDUCATION", requirement: "Đại học" },
  { name: "Tiếng Anh", type: "LANGUAGE", requirement: "TOEIC 600" },
  { name: "Chứng chỉ", type: "CERTIFICATION", requirement: "" },
]

/** Hướng dẫn nhập yêu cầu theo từng loại tiêu chí. */
const REQUIREMENT_HELP: Record<CriterionType, { placeholder: string; help: string }> = {
  EXPERIENCE: { placeholder: "2", help: "Số năm tối thiểu. Hệ thống tính từ các mốc thời gian trong CV." },
  EDUCATION: { placeholder: "Đại học", help: "Trung cấp, Cao đẳng, Đại học, Thạc sĩ hoặc Tiến sĩ." },
  LANGUAGE: { placeholder: "TOEIC 600", help: "Chứng chỉ và mức tối thiểu, ví dụ TOEIC 600, IELTS 6.0, JLPT N3." },
  CERTIFICATION: {
    placeholder: "Kế toán trưởng, ACCA, CPA",
    help: "Tên chứng chỉ, cách nhau bằng dấu phẩy. Có một trong số đó là đạt.",
  },
  OTHER: { placeholder: "làm việc nhóm, thuyết trình", help: "Từ khoá cần tìm, cách nhau bằng dấu phẩy." },
}

interface Draft {
  title: string
  department: string
  description: string
  required: string[]
  preferred: string[]
  criteria: CriterionRow[]
}

function draftFrom(job: Job | null): Draft {
  return {
    title: job?.title ?? "",
    department: job?.department ?? "",
    description: job?.description ?? "",
    required: job?.skills.filter((s) => s.weight === "REQUIRED").map((s) => s.name) ?? [],
    preferred: job?.skills.filter((s) => s.weight !== "REQUIRED").map((s) => s.name) ?? [],
    criteria: (job?.criteria ?? []).map(newCriterion),
  }
}

/** So sánh nội dung, bỏ qua khoá nội bộ của dòng tiêu chí. */
function snapshot(d: Draft) {
  return JSON.stringify({ ...d, criteria: d.criteria.map(({ key: _key, ...c }) => c) })
}

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

  useEffect(() => {
    if (!open) return
    const d = draftFrom(job)
    setDraft(d)
    setInitial(snapshot(d))
    setErrors({})
    setBusy(false)
  }, [open, job])

  const dirty = useMemo(() => open && snapshot(draft) !== initial, [open, draft, initial])
  const set = <K extends keyof Draft>(k: K, v: Draft[K]) => setDraft((d) => ({ ...d, [k]: v }))
  const patch = (key: number, p: Partial<Criterion>) =>
    setDraft((d) => ({ ...d, criteria: d.criteria.map((r) => (r.key === key ? { ...r, ...p } : r)) }))

  function requestClose() {
    if (dirty && !busy) setConfirmClose(true)
    else onOpenChange(false)
  }

  async function save() {
    const errs: Record<string, boolean> = {}
    if (!draft.title.trim()) errs.title = true
    if (!draft.required.length && !draft.preferred.length) errs.skills = true
    for (const c of draft.criteria) {
      if (c.name.trim() && !c.requirement.trim()) errs[`req-${c.key}`] = true
      if (!c.name.trim() && c.requirement.trim()) errs[`name-${c.key}`] = true
    }
    setErrors(errs)
    const first = Object.keys(errs)[0]
    if (first) {
      document.getElementById(`je-${first}`)?.focus()
      toast.error(
        errs.title
          ? "Vị trí cần có tên."
          : errs.skills
            ? "Thêm ít nhất một kỹ năng."
            : "Mỗi tiêu chí cần có cả tên lẫn yêu cầu."
      )
      return
    }

    const payload = {
      title: draft.title.trim(),
      department: draft.department.trim(),
      description: draft.description,
      skills: [
        ...draft.required.map((name) => ({ name, weight: "REQUIRED" as SkillWeight })),
        ...draft.preferred.map((name) => ({ name, weight: "PREFERRED" as SkillWeight })),
      ],
      criteria: draft.criteria
        .filter((c) => c.name.trim() && c.requirement.trim())
        .map(({ name, type, requirement, weight }) => ({
          name: name.trim(),
          type,
          requirement: requirement.trim(),
          weight,
        })),
    }
    setBusy(true)
    try {
      const saved = job
        ? await api<Job>(`/api/admin/jobs/${job.id}`, put(payload))
        : await api<Job>("/api/admin/jobs", post(payload))
      toast.success(job ? "Đã lưu thay đổi." : `Đã tạo vị trí "${payload.title}".`, {
        description: job && job.applicationCount ? "Điểm cũ chưa đổi. Bấm Chấm lại tất cả để cập nhật." : undefined,
      })
      onSaved(saved, !job)
    } catch (e) {
      toast.error((e as Error).message)
      setBusy(false)
    }
  }

  const move = (name: string, to: "required" | "preferred") =>
    setDraft((d) =>
      to === "required"
        ? { ...d, preferred: d.preferred.filter((x) => x !== name), required: [...d.required, name] }
        : { ...d, required: d.required.filter((x) => x !== name), preferred: [...d.preferred, name] }
    )

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

                {draft.criteria.map((c, i) => {
                  const help = REQUIREMENT_HELP[c.type]
                  return (
                    <div key={c.key} className="flex flex-col gap-3 rounded-xl border bg-card p-4 shadow-xs">
                      <div className="flex items-center gap-2">
                        <span className="grid size-6 shrink-0 place-items-center rounded-full bg-muted text-xs font-medium text-muted-foreground tabular-nums">
                          {i + 1}
                        </span>
                        <Input
                          id={`je-name-${c.key}`}
                          aria-label="Tên tiêu chí"
                          autoComplete="off"
                          placeholder="Tên tiêu chí, VD: Kinh nghiệm…"
                          className="font-medium"
                          value={c.name}
                          aria-invalid={errors[`name-${c.key}`] || undefined}
                          onChange={(e) => patch(c.key, { name: e.target.value })}
                        />
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="shrink-0 text-muted-foreground hover:text-destructive"
                              aria-label={`Xoá tiêu chí ${c.name || i + 1}`}
                              onClick={() =>
                                setDraft((d) => ({ ...d, criteria: d.criteria.filter((r) => r.key !== c.key) }))
                              }
                            >
                              <Trash2Icon />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Xoá tiêu chí</TooltipContent>
                        </Tooltip>
                      </div>

                      <div className="grid gap-3 sm:grid-cols-[12rem_minmax(0,1fr)]">
                        <Field>
                          <FieldLabel className="text-xs text-muted-foreground">Loại</FieldLabel>
                          <Select value={c.type} onValueChange={(v) => patch(c.key, { type: v as CriterionType })}>
                            <SelectTrigger className="w-full" aria-label="Loại tiêu chí">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {CRITERION_TYPES.map((t) => (
                                <SelectItem key={t.value} value={t.value}>
                                  {t.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </Field>
                        <Field data-invalid={errors[`req-${c.key}`] || undefined}>
                          <FieldLabel htmlFor={`je-req-${c.key}`} className="text-xs text-muted-foreground">
                            Yêu cầu
                          </FieldLabel>
                          <Input
                            id={`je-req-${c.key}`}
                            autoComplete="off"
                            placeholder={`${help.placeholder}…`}
                            value={c.requirement}
                            aria-invalid={errors[`req-${c.key}`] || undefined}
                            onChange={(e) => patch(c.key, { requirement: e.target.value })}
                          />
                        </Field>
                      </div>
                      <p className="-mt-1 text-xs text-muted-foreground">{help.help}</p>

                      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                        <span id={`je-w-${c.key}`} className="text-xs font-medium text-muted-foreground">
                          Mức quan trọng
                        </span>
                        <ToggleGroup
                          type="single"
                          variant="outline"
                          size="sm"
                          spacing={0}
                          value={String(c.weight)}
                          onValueChange={(v) => v && patch(c.key, { weight: Number(v) })}
                          aria-labelledby={`je-w-${c.key}`}
                        >
                          {[1, 2, 3].map((w) => (
                            <ToggleGroupItem key={w} value={String(w)} className="px-3 text-xs">
                              {WEIGHT_LABEL[w]}
                            </ToggleGroupItem>
                          ))}
                        </ToggleGroup>
                      </div>
                    </div>
                  )
                })}

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

/** Ô nhập dạng thẻ: Enter hoặc dấu phẩy để thêm, Backspace ở ô trống để xoá thẻ cuối. */
function TagInput({
  id,
  label,
  hint,
  tags,
  other,
  onChange,
  invalid,
  variant,
  moveLabel,
  MoveIcon,
  onMove,
}: {
  id: string
  label: string
  hint: string
  tags: string[]
  other: string[]
  onChange: (tags: string[]) => void
  invalid?: boolean
  variant: "info" | "secondary"
  moveLabel: string
  MoveIcon: typeof ArrowUpIcon
  onMove: (name: string) => void
}) {
  const [draft, setDraft] = useState("")
  const inputRef = useRef<HTMLInputElement>(null)

  function add(raw: string) {
    const taken = new Set([...tags, ...other].map((t) => t.toLowerCase()))
    const next = raw
      .split(/[,;\n]/)
      .map((s) => s.trim())
      .filter((s) => s && !taken.has(s.toLowerCase()) && (taken.add(s.toLowerCase()), true))
    if (next.length) onChange([...tags, ...next])
    setDraft("")
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if ((e.key === "Enter" || e.key === ",") && draft.trim()) {
      e.preventDefault()
      add(draft)
    } else if (e.key === "Enter") {
      e.preventDefault()
    } else if (e.key === "Backspace" && !draft && tags.length) {
      onChange(tags.slice(0, -1))
    }
  }

  return (
    <Field data-invalid={invalid || undefined}>
      <div className="flex items-baseline justify-between gap-2">
        <FieldLabel htmlFor={id}>
          {label} <span className="font-normal text-muted-foreground tabular-nums">({tags.length})</span>
        </FieldLabel>
        <span className="text-xs text-muted-foreground">{hint}</span>
      </div>
      <div
        className={cn(
          "flex min-h-11 flex-wrap items-center gap-1.5 rounded-lg border border-input bg-background p-2 transition-colors focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 dark:bg-input/30",
          invalid && "border-destructive"
        )}
        onClick={() => inputRef.current?.focus()}
      >
        {tags.map((t) => (
          <Badge key={t} variant={variant} className="h-7 gap-0.5 pr-1 pl-2.5 text-sm">
            {t}
            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  type="button"
                  className="grid size-5 place-items-center rounded-sm opacity-60 hover:bg-foreground/10 hover:opacity-100"
                  aria-label={`${moveLabel}: ${t}`}
                  onClick={(e) => {
                    e.stopPropagation()
                    onMove(t)
                  }}
                >
                  <MoveIcon className="size-3.5!" />
                </button>
              </TooltipTrigger>
              <TooltipContent>{moveLabel}</TooltipContent>
            </Tooltip>
            <button
              type="button"
              className="grid size-5 place-items-center rounded-sm opacity-60 hover:bg-foreground/10 hover:opacity-100"
              aria-label={`Xoá ${t}`}
              onClick={(e) => {
                e.stopPropagation()
                onChange(tags.filter((x) => x !== t))
              }}
            >
              <XIcon className="size-3.5!" />
            </button>
          </Badge>
        ))}
        <input
          ref={inputRef}
          id={id}
          autoComplete="off"
          className="h-7 min-w-40 flex-1 bg-transparent px-1 text-sm outline-none placeholder:text-muted-foreground"
          placeholder={tags.length ? "Thêm kỹ năng…" : "VD: Excel, Báo cáo tài chính, Thuế…"}
          value={draft}
          aria-invalid={invalid || undefined}
          onChange={(e) => {
            const v = e.target.value
            if (v.includes(",")) add(v)
            else setDraft(v)
          }}
          onKeyDown={onKeyDown}
          onBlur={() => draft.trim() && add(draft)}
        />
      </div>
    </Field>
  )
}
