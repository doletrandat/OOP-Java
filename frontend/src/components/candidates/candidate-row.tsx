import type { ReactNode } from "react"
import { CheckIcon, MoreHorizontalIcon, PencilIcon, RotateCcwIcon, Trash2Icon, XIcon } from "lucide-react"

import { DecisionBadge, ScoreBadge } from "@/components/status"
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
import { TableCell, TableRow } from "@/components/ui/table"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import type { ApplicationSummary, HrDecision } from "@/lib/api"
import { AI_STATUS_LABEL, fmtScore, fmtTime, initials, relativeTime } from "@/lib/format"
import { cn } from "@/lib/utils"

/** Một dòng ứng viên: bấm để mở, nút quyết định nhanh hiện khi rê chuột. */
export function CandidateRow({
  app: a,
  rank,
  checked,
  onCheckedChange,
  onOpen,
  onDecide,
  onEdit,
  onDelete,
}: {
  app: ApplicationSummary
  rank?: number
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  onOpen: () => void
  onDecide: (decision: HrDecision) => void
  onEdit: () => void
  onDelete: () => void
}) {
  return (
    <TableRow
      data-state={checked ? "selected" : undefined}
      tabIndex={0}
      className="group cursor-pointer"
      aria-label={`Mở hồ sơ ${a.candidateName}`}
      onClick={(e) => {
        if ((e.target as HTMLElement).closest("button,[role=checkbox],[role=menu],a")) return
        onOpen()
      }}
      onKeyDown={(e) => {
        if ((e.key === "Enter" || e.key === " ") && e.target === e.currentTarget) {
          e.preventDefault()
          onOpen()
        }
      }}
    >
      <TableCell className="pl-4">
        <Checkbox
          aria-label={`Chọn ${a.candidateName}`}
          checked={checked}
          onCheckedChange={(c) => onCheckedChange(!!c)}
        />
      </TableCell>
      <TableCell className="text-right text-muted-foreground tabular-nums">{rank}</TableCell>
      <TableCell className="py-2.5">
        <div className="flex items-center gap-3">
          <Avatar className="size-8">
            <AvatarFallback className="text-xs">{initials(a.candidateName)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <div className="truncate font-medium">{a.candidateName}</div>
            <div className="truncate text-xs text-muted-foreground">
              {[a.email, a.phone].filter(Boolean).join(" · ") || "Chưa có liên hệ"}
            </div>
          </div>
        </div>
      </TableCell>
      <TableCell className="text-center">
        <ScoreBadge value={a.finalScore} large />
      </TableCell>
      <TableCell className="hidden text-right tabular-nums md:table-cell">
        {a.matchedCount != null ? (
          <>
            {a.matchedCount}
            <span className="text-muted-foreground">/{a.totalSkills}</span>
          </>
        ) : (
          "—"
        )}
      </TableCell>
      <TableCell className="hidden text-right tabular-nums lg:table-cell">
        {a.criteriaTotal ? (
          <>
            {a.criteriaMet}
            <span className="text-muted-foreground">/{a.criteriaTotal}</span>
          </>
        ) : (
          <span className="text-muted-foreground">—</span>
        )}
      </TableCell>
      <TableCell className="hidden text-center tabular-nums lg:table-cell">
        {a.aiStatus === "OK" ? (
          <span className="text-muted-foreground">{fmtScore(a.aiScore)}</span>
        ) : (
          <Badge variant={a.aiStatus === "ERROR" ? "destructive" : "secondary"}>
            {AI_STATUS_LABEL[a.aiStatus] ?? "—"}
          </Badge>
        )}
      </TableCell>
      <TableCell className="hidden sm:table-cell">
        <DecisionBadge decision={a.hrDecision} />
      </TableCell>
      <TableCell className="hidden text-xs text-muted-foreground xl:table-cell" title={fmtTime(a.submittedAt)}>
        {relativeTime(a.submittedAt)}
      </TableCell>
      <TableCell className="pr-4">
        <div className="flex items-center justify-end gap-0.5">
          <div className="flex gap-0.5 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100 sm:opacity-0 [@media(hover:none)]:opacity-100">
            <QuickAction
              label="Mời phỏng vấn"
              active={a.hrDecision === "SHORTLISTED"}
              onClick={() => onDecide(a.hrDecision === "SHORTLISTED" ? "NEW" : "SHORTLISTED")}
              className="data-[active=true]:bg-success/15 data-[active=true]:text-success-foreground"
            >
              <CheckIcon />
            </QuickAction>
            <QuickAction
              label="Từ chối"
              active={a.hrDecision === "REJECTED"}
              onClick={() => onDecide(a.hrDecision === "REJECTED" ? "NEW" : "REJECTED")}
              className="data-[active=true]:bg-destructive/10 data-[active=true]:text-destructive"
            >
              <XIcon />
            </QuickAction>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${a.candidateName}`}>
                <MoreHorizontalIcon />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuItem onSelect={onEdit}>
                <PencilIcon /> Sửa thông tin
              </DropdownMenuItem>
              {a.hrDecision !== "NEW" && (
                <DropdownMenuItem onSelect={() => onDecide("NEW")}>
                  <RotateCcwIcon /> Đặt lại "chưa xử lý"
                </DropdownMenuItem>
              )}
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onSelect={onDelete}>
                <Trash2Icon /> Xoá ứng viên
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </TableCell>
    </TableRow>
  )
}

function QuickAction({
  label,
  active,
  onClick,
  className,
  children,
}: {
  label: string
  active: boolean
  onClick: () => void
  className?: string
  children: ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={active ? `Bỏ "${label}"` : label}
          aria-pressed={active}
          data-active={active}
          onClick={onClick}
          className={cn(className)}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{active ? `Bỏ "${label}"` : label}</TooltipContent>
    </Tooltip>
  )
}
