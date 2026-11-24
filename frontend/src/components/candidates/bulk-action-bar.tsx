import { CheckIcon, Trash2Icon, XIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/** Thanh nổi ở cuối trang khi có ứng viên được chọn. */
export function BulkActionBar({
  count,
  onShortlist,
  onReject,
  onDelete,
  onClear,
}: {
  count: number
  onShortlist: () => void
  onReject: () => void
  onDelete: () => void
  onClear: () => void
}) {
  return (
    <div
      className={cn(
        "fixed inset-x-0 bottom-4 z-40 mx-auto flex w-fit max-w-[calc(100%-2rem)] items-center gap-1 rounded-xl border bg-popover p-1.5 pl-4 text-sm shadow-lg transition-[opacity,transform] duration-200 ease-out",
        count ? "translate-y-0 opacity-100" : "pointer-events-none translate-y-2 opacity-0"
      )}
      role="toolbar"
      aria-label="Thao tác với ứng viên đã chọn"
      aria-hidden={!count}
    >
      <span className="mr-2 font-medium tabular-nums">Đã chọn {count}</span>
      <Button size="sm" variant="ghost" onClick={onShortlist}>
        <CheckIcon data-icon="inline-start" className="text-success" />
        <span className="hidden sm:inline">Mời phỏng vấn</span>
        <span className="sm:hidden">Mời PV</span>
      </Button>
      <Button size="sm" variant="ghost" onClick={onReject}>
        <XIcon data-icon="inline-start" className="text-destructive" />
        Từ chối
      </Button>
      <Button size="sm" variant="ghost" className="hidden sm:inline-flex" onClick={onDelete}>
        <Trash2Icon data-icon="inline-start" />
        Xoá
      </Button>
      <Button size="icon-sm" variant="ghost" aria-label="Bỏ chọn" onClick={onClear}>
        <XIcon />
      </Button>
    </div>
  )
}
