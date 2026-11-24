import { CheckIcon, CircleHelpIcon, MinusIcon, XIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import type { CriterionResult, HrDecision, SkillMatch } from "@/lib/api"
import { DECISION_META, fmtScore, progressTone, scoreTone } from "@/lib/format"
import { cn } from "@/lib/utils"

export function JobStatusBadge({ status }: { status: "OPEN" | "CLOSED" }) {
  return status === "OPEN" ? (
    <Badge variant="success">
      <span aria-hidden className="size-1.5 rounded-full bg-success" />
      Đang mở
    </Badge>
  ) : (
    <Badge variant="secondary">Đã đóng</Badge>
  )
}

export function DecisionBadge({ decision }: { decision: HrDecision }) {
  const m = DECISION_META[decision] ?? DECISION_META.NEW
  return <Badge variant={m.variant}>{m.label}</Badge>
}

export function ScoreBadge({ value, large }: { value: number | null | undefined; large?: boolean }) {
  const tone = scoreTone(value)
  return (
    <Badge
      variant={tone}
      className={cn("min-w-10 rounded-md font-semibold tabular-nums", large && "h-7 min-w-11 text-sm")}
    >
      {value == null ? "…" : fmtScore(value)}
    </Badge>
  )
}

export function ScoreProgress({ value, className }: { value: number | null | undefined; className?: string }) {
  const v = value == null || value < 0 ? 0 : Math.max(0, Math.min(100, value))
  return <Progress value={v} className={cn("h-1.5", progressTone[scoreTone(value)], className)} />
}

const SKILL_STATUS = {
  MATCHED: { label: "Khớp", variant: "success", Icon: CheckIcon },
  UNCERTAIN: { label: "Chưa chắc", variant: "warning", Icon: CircleHelpIcon },
  MISSING: { label: "Không thấy", variant: "destructive", Icon: XIcon },
} as const

const CRITERION_STATUS = {
  MET: { label: "Đạt", variant: "success", Icon: CheckIcon },
  PARTIAL: { label: "Đạt một phần", variant: "warning", Icon: MinusIcon },
  NOT_MET: { label: "Chưa đạt", variant: "destructive", Icon: XIcon },
  UNKNOWN: { label: "Không rõ", variant: "secondary", Icon: CircleHelpIcon },
} as const

export function SkillStatusBadge({ status }: { status: SkillMatch["status"] }) {
  const m = SKILL_STATUS[status] ?? SKILL_STATUS.MISSING
  return (
    <Badge variant={m.variant}>
      <m.Icon data-icon="inline-start" />
      {m.label}
    </Badge>
  )
}

export function CriterionStatusBadge({ status }: { status: CriterionResult["status"] }) {
  const m = CRITERION_STATUS[status] ?? CRITERION_STATUS.UNKNOWN
  return (
    <Badge variant={m.variant}>
      <m.Icon data-icon="inline-start" />
      {m.label}
    </Badge>
  )
}
