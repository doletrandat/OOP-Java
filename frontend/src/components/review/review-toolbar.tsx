import type { ReactNode } from "react"
import {
  ArrowLeftIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  DownloadIcon,
  MailIcon,
  MoreHorizontalIcon,
  PencilIcon,
  PhoneIcon,
  RefreshCwIcon,
  Trash2Icon,
} from "lucide-react"
import { toast } from "sonner"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Skeleton } from "@/components/ui/skeleton"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import type { Application, ApplicationSummary } from "@/lib/api"
import { downloadCv } from "@/lib/cv"
import { fmtTime, initials } from "@/lib/format"

/** Thanh trên cùng khi duyệt CV: tên ứng viên, chuyển hồ sơ và menu thao tác. */
export function ReviewToolbar({
  appId,
  name,
  summary,
  detail,
  position,
  total,
  prevId,
  nextId,
  rescoring,
  onBack,
  onGo,
  onEdit,
  onRescore,
  onDelete,
}: {
  appId: string
  name: string
  summary?: ApplicationSummary
  detail: Application | null
  /** Vị trí hiện tại, tính từ 0. */
  position: number
  total: number
  prevId?: string
  nextId?: string
  rescoring: boolean
  onBack: () => void
  onGo: (id?: string) => void
  onEdit: (target: ApplicationSummary) => void
  onRescore: () => void
  onDelete: () => void
}) {
  return (
    <div className="flex items-center gap-2 border-b px-3 py-2 sm:px-4">
      <Tooltip>
        <TooltipTrigger asChild>
          <Button variant="ghost" size="icon" onClick={onBack} aria-label="Về danh sách">
            <ArrowLeftIcon />
          </Button>
        </TooltipTrigger>
        <TooltipContent>Về danh sách</TooltipContent>
      </Tooltip>
      <Avatar className="hidden size-9 sm:flex">
        <AvatarFallback>{initials(name || "?")}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 items-center gap-1">
          <h1 className="truncate text-base font-semibold">{name || <Skeleton className="h-5 w-40" />}</h1>
          {summary && (
            <Button variant="ghost" size="icon-xs" aria-label="Sửa thông tin ứng viên" onClick={() => onEdit(summary)}>
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
        {position + 1} / {total}
      </span>
      <div className="flex">
        <NavButton label="Hồ sơ trước" disabled={!prevId} onClick={() => onGo(prevId)}>
          <ChevronLeftIcon />
        </NavButton>
        <NavButton label="Hồ sơ tiếp" disabled={!nextId} onClick={() => onGo(nextId)}>
          <ChevronRightIcon />
        </NavButton>
      </div>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" aria-label="Thao tác khác">
            <MoreHorizontalIcon />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-52">
          <DropdownMenuItem
            onSelect={() => downloadCv(appId, detail?.cvFileName ?? "CV.pdf").catch((e: Error) => toast.error(e.message))}
          >
            <DownloadIcon /> Tải file PDF
          </DropdownMenuItem>
          <DropdownMenuItem disabled={rescoring} onSelect={onRescore}>
            <RefreshCwIcon /> Chấm lại CV này
          </DropdownMenuItem>
          <DropdownMenuItem disabled={!summary} onSelect={() => summary && onEdit(summary)}>
            <PencilIcon /> Sửa thông tin
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onSelect={onDelete}>
            <Trash2Icon /> Xoá ứng viên
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}

function NavButton({
  label,
  disabled,
  onClick,
  children,
}: {
  label: string
  disabled: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={label} disabled={disabled} onClick={onClick}>
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  )
}
