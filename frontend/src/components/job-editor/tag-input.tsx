import { useRef, useState, type KeyboardEvent } from "react"
import { XIcon, type LucideIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Field, FieldLabel } from "@/components/ui/field"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"

/** Ô nhập dạng thẻ: Enter hoặc dấu phẩy để thêm, Backspace ở ô trống để xoá thẻ cuối. */
export function TagInput({
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
  MoveIcon: LucideIcon
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
          "flex min-h-11 flex-wrap items-center gap-1.5 rounded-lg border border-input bg-background p-2 transition-colors focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50",
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
