import { Trash2Icon } from "lucide-react"

import { REQUIREMENT_HELP, type CriterionRow } from "@/components/job-editor/job-draft"
import { Button } from "@/components/ui/button"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import type { Criterion, CriterionType } from "@/lib/api"
import { CRITERION_TYPES, WEIGHT_LABEL } from "@/lib/format"

/** Một tiêu chí bổ sung trong form: tên, loại, yêu cầu và mức quan trọng. */
export function CriterionCard({
  row,
  index,
  errors,
  onChange,
  onRemove,
}: {
  row: CriterionRow
  index: number
  errors: Record<string, boolean>
  onChange: (patch: Partial<Criterion>) => void
  onRemove: () => void
}) {
  const help = REQUIREMENT_HELP[row.type]
  const nameInvalid = errors[`name-${row.key}`] || undefined
  const reqInvalid = errors[`req-${row.key}`] || undefined
  return (
    <div className="flex flex-col gap-3 rounded-xl border bg-card p-4 shadow-xs">
      <div className="flex items-center gap-2">
        <span className="grid size-6 shrink-0 place-items-center rounded-full bg-muted text-xs font-medium text-muted-foreground tabular-nums">
          {index + 1}
        </span>
        <Input
          id={`je-name-${row.key}`}
          aria-label="Tên tiêu chí"
          autoComplete="off"
          placeholder="Tên tiêu chí, VD: Kinh nghiệm…"
          className="font-medium"
          value={row.name}
          aria-invalid={nameInvalid}
          onChange={(e) => onChange({ name: e.target.value })}
        />
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="shrink-0 text-muted-foreground hover:text-destructive"
              aria-label={`Xoá tiêu chí ${row.name || index + 1}`}
              onClick={onRemove}
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
          <Select value={row.type} onValueChange={(v) => onChange({ type: v as CriterionType })}>
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
        <Field data-invalid={reqInvalid}>
          <FieldLabel htmlFor={`je-req-${row.key}`} className="text-xs text-muted-foreground">
            Yêu cầu
          </FieldLabel>
          <Input
            id={`je-req-${row.key}`}
            autoComplete="off"
            placeholder={`${help.placeholder}…`}
            value={row.requirement}
            aria-invalid={reqInvalid}
            onChange={(e) => onChange({ requirement: e.target.value })}
          />
        </Field>
      </div>
      <p className="-mt-1 text-xs text-muted-foreground">{help.help}</p>

      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <span id={`je-w-${row.key}`} className="text-xs font-medium text-muted-foreground">
          Mức quan trọng
        </span>
        <ToggleGroup
          type="single"
          variant="outline"
          size="sm"
          spacing={0}
          value={String(row.weight)}
          onValueChange={(v) => v && onChange({ weight: Number(v) })}
          aria-labelledby={`je-w-${row.key}`}
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
}
