import { useEffect, useRef, useState, type KeyboardEvent } from "react"
import { ArrowDownIcon, ArrowUpIcon, PlusIcon, Trash2Icon, XIcon } from "lucide-react"
import { toast } from "sonner"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSeparator,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Spinner } from "@/components/ui/spinner"
import { Textarea } from "@/components/ui/textarea"
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
]

export function JobEditorSheet({
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
  const [title, setTitle] = useState("")
  const [department, setDepartment] = useState("")
  const [description, setDescription] = useState("")
  const [required, setRequired] = useState<string[]>([])
  const [preferred, setPreferred] = useState<string[]>([])
  const [criteria, setCriteria] = useState<CriterionRow[]>([])
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, boolean>>({})

  useEffect(() => {
    if (!open) return
    setTitle(job?.title ?? "")
    setDepartment(job?.department ?? "")
    setDescription(job?.description ?? "")
    setRequired(job?.skills.filter((s) => s.weight === "REQUIRED").map((s) => s.name) ?? [])
    setPreferred(job?.skills.filter((s) => s.weight !== "REQUIRED").map((s) => s.name) ?? [])
    setCriteria((job?.criteria ?? []).map(newCriterion))
    setErrors({})
    setBusy(false)
  }, [open, job])

  const patch = (key: number, p: Partial<Criterion>) =>
    setCriteria((rows) => rows.map((r) => (r.key === key ? { ...r, ...p } : r)))

  async function save() {
    const errs: Record<string, boolean> = {}
    if (!title.trim()) errs.title = true
    if (!required.length && !preferred.length) errs.skills = true
    for (const c of criteria) {
      if (c.name.trim() && !c.requirement.trim()) errs[`req-${c.key}`] = true
      if (!c.name.trim() && c.requirement.trim()) errs[`name-${c.key}`] = true
    }
    setErrors(errs)
    const first = Object.keys(errs)[0]
    if (first) {
      document.getElementById(`je-${first}`)?.focus()
      toast.error(
        errs.title ? "Vị trí cần có tên." : errs.skills ? "Thêm ít nhất một kỹ năng." : "Tiêu chí cần có cả tên lẫn yêu cầu."
      )
      return
    }

    const payload = {
      title: title.trim(),
      department: department.trim(),
      description,
      skills: [
        ...required.map((name) => ({ name, weight: "REQUIRED" as SkillWeight })),
        ...preferred.map((name) => ({ name, weight: "PREFERRED" as SkillWeight })),
      ],
      criteria: criteria
        .filter((c) => c.name.trim() && c.requirement.trim())
        .map(({ name, type, requirement, weight }) => ({ name: name.trim(), type, requirement: requirement.trim(), weight })),
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

  const move = (name: string, to: "required" | "preferred") => {
    if (to === "required") {
      setPreferred((l) => l.filter((x) => x !== name))
      setRequired((l) => [...l, name])
    } else {
      setRequired((l) => l.filter((x) => x !== name))
      setPreferred((l) => [...l, name])
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col gap-0 p-0 sm:max-w-xl">
        <SheetHeader className="border-b">
          <SheetTitle>{job ? "Sửa vị trí" : "Tạo vị trí mới"}</SheetTitle>
          <SheetDescription>CV tải lên được so khớp với kỹ năng và tiêu chí ở đây.</SheetDescription>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto overscroll-contain p-4">
          <FieldGroup>
            <Field data-invalid={errors.title || undefined}>
              <FieldLabel htmlFor="je-title">Tên vị trí</FieldLabel>
              <Input
                id="je-title"
                autoComplete="off"
                autoFocus
                placeholder="VD: Lập trình viên Java Backend…"
                value={title}
                aria-invalid={errors.title || undefined}
                onChange={(e) => setTitle(e.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="je-dept">Phòng ban</FieldLabel>
              <Input
                id="je-dept"
                autoComplete="off"
                placeholder="VD: Phòng Kỹ thuật…"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              />
            </Field>

            <FieldSeparator />

            <FieldSet>
              <FieldLegend>Kỹ năng cần tìm</FieldLegend>
              <FieldDescription>
                Gõ rồi nhấn Enter, hoặc dán cả danh sách cách nhau bằng dấu phẩy. Hệ thống nhận ra từ đồng nghĩa tiếng Việt và tiếng Anh
                như "cơ sở dữ liệu" và "database".
              </FieldDescription>
              <TagInput
                id="je-skills"
                label="Bắt buộc"
                hint="Thiếu kỹ năng này thì điểm giảm mạnh."
                tags={required}
                onChange={setRequired}
                other={preferred}
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
                tags={preferred}
                onChange={setPreferred}
                other={required}
                variant="secondary"
                moveLabel="Chuyển sang bắt buộc"
                MoveIcon={ArrowUpIcon}
                onMove={(n) => move(n, "required")}
              />
            </FieldSet>

            <FieldSeparator />

            <FieldSet>
              <FieldLegend>
                Tiêu chí bổ sung <span className="font-normal text-muted-foreground">(không bắt buộc)</span>
              </FieldLegend>
              <FieldDescription>
                Số năm kinh nghiệm, bằng cấp, ngoại ngữ hay chứng chỉ. Kết quả mỗi tiêu chí kèm đoạn trích từ CV để bạn
                kiểm tra.
              </FieldDescription>
              {criteria.length > 0 && (
                <div className="flex flex-col gap-2">
                  {criteria.map((c) => {
                    const hint = CRITERION_TYPES.find((t) => t.value === c.type)?.hint ?? ""
                    return (
                      <div key={c.key} className="grid grid-cols-[1fr_1fr_auto] gap-2 rounded-lg border bg-muted/30 p-2">
                        <Input
                          id={`je-name-${c.key}`}
                          aria-label="Tên tiêu chí"
                          autoComplete="off"
                          placeholder="Tên tiêu chí…"
                          value={c.name}
                          aria-invalid={errors[`name-${c.key}`] || undefined}
                          onChange={(e) => patch(c.key, { name: e.target.value })}
                        />
                        <Input
                          id={`je-req-${c.key}`}
                          aria-label="Yêu cầu"
                          autoComplete="off"
                          placeholder={`${hint}…`}
                          value={c.requirement}
                          aria-invalid={errors[`req-${c.key}`] || undefined}
                          onChange={(e) => patch(c.key, { requirement: e.target.value })}
                        />
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Xoá tiêu chí"
                          onClick={() => setCriteria((rows) => rows.filter((r) => r.key !== c.key))}
                        >
                          <Trash2Icon />
                        </Button>
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
                        <Select value={String(c.weight)} onValueChange={(v) => patch(c.key, { weight: Number(v) })}>
                          <SelectTrigger className="w-full" aria-label="Mức quan trọng">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {[1, 2, 3].map((w) => (
                              <SelectItem key={w} value={String(w)}>
                                {WEIGHT_LABEL[w]}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    )
                  })}
                </div>
              )}
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    const row = newCriterion()
                    setCriteria((r) => [...r, row])
                    requestAnimationFrame(() => document.getElementById(`je-name-${row.key}`)?.focus())
                  }}
                >
                  <PlusIcon data-icon="inline-start" />
                  Thêm tiêu chí
                </Button>
                {CRITERION_PRESETS.filter((p) => !criteria.some((c) => c.type === p.type)).map((p) => (
                  <Button key={p.name} variant="ghost" size="sm" onClick={() => setCriteria((r) => [...r, newCriterion(p)])}>
                    <PlusIcon data-icon="inline-start" />
                    {p.name}
                  </Button>
                ))}
              </div>
            </FieldSet>

            <FieldSeparator />

            <Field>
              <FieldLabel htmlFor="je-desc">Mô tả công việc</FieldLabel>
              <Textarea
                id="je-desc"
                rows={6}
                placeholder="Dán mô tả công việc vào đây. AI dùng phần này khi đánh giá CV…"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Field>
          </FieldGroup>
        </div>

        <SheetFooter className="flex-row justify-end border-t bg-muted/50">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Huỷ
          </Button>
          <Button onClick={save} disabled={busy}>
            {busy && <Spinner data-icon="inline-start" />}
            {busy ? "Đang lưu…" : job ? "Lưu thay đổi" : "Tạo vị trí"}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
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
      <div className="flex flex-col gap-0.5">
        <FieldLabel htmlFor={id}>{label}</FieldLabel>
        <span className="text-xs text-muted-foreground">{hint}</span>
      </div>
      <div
        className={cn(
          "flex min-h-9 flex-wrap items-center gap-1.5 rounded-lg border border-input bg-transparent p-1.5 transition-colors focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 dark:bg-input/30",
          invalid && "border-destructive"
        )}
        onClick={() => inputRef.current?.focus()}
      >
        {tags.map((t) => (
          <Badge key={t} variant={variant} className="h-6 gap-0.5 pr-0.5 pl-2 text-[0.8rem]">
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
                  <MoveIcon className="size-3!" />
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
              <XIcon className="size-3!" />
            </button>
          </Badge>
        ))}
        <input
          ref={inputRef}
          id={id}
          autoComplete="off"
          className="h-6 min-w-32 flex-1 bg-transparent px-1 text-sm outline-none placeholder:text-muted-foreground"
          placeholder={tags.length ? "Thêm…" : "VD: Java, SQL, REST API…"}
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
