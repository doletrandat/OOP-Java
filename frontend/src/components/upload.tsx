import { useRef, useState, type DragEvent } from "react"
import { AlertCircleIcon, CheckIcon, FileTextIcon, UploadIcon } from "lucide-react"

import { ScoreBadge } from "@/components/status"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Progress } from "@/components/ui/progress"
import { Spinner } from "@/components/ui/spinner"
import type { UploadQueue } from "@/lib/use-upload-queue"
import { cn } from "@/lib/utils"

export function UploadDialog({
  open,
  onOpenChange,
  queue,
  onOpenCandidate,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  queue: UploadQueue
  onOpenCandidate: (appId: string) => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [drag, setDrag] = useState(false)
  const { items, add, clearFinished, finished, active } = queue

  function onDrop(e: DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    setDrag(false)
    if (e.dataTransfer.files.length) add(e.dataTransfer.files)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[calc(100dvh-2rem)] flex-col sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Tải CV lên</DialogTitle>
          <DialogDescription>
            Chọn một hoặc nhiều file PDF. Hệ thống tự đọc họ tên, email, số điện thoại và chấm điểm từng CV.
          </DialogDescription>
        </DialogHeader>

        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDrag(true)
          }}
          onDragLeave={() => setDrag(false)}
          onDrop={onDrop}
          className={cn(
            "flex flex-col items-center gap-2 rounded-lg border border-dashed border-input bg-muted/40 px-4 py-8 text-center transition-colors outline-none hover:border-primary/60 hover:bg-accent/50 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50",
            drag && "border-primary bg-accent"
          )}
        >
          <span className="grid size-10 place-items-center rounded-full border bg-background text-muted-foreground">
            <UploadIcon className="size-5" />
          </span>
          <span className="text-sm font-medium">Kéo file vào đây hoặc bấm để chọn</span>
          <span className="text-xs text-muted-foreground">
            PDF có chữ chọn được. Bản scan dạng ảnh sẽ không đọc được.
          </span>
        </button>
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,application/pdf"
          multiple
          hidden
          onChange={(e) => {
            if (e.target.files?.length) add(e.target.files)
            e.target.value = ""
          }}
        />

        {items.length > 0 && (
          <div className="-mx-4 min-h-0 flex-1 overflow-y-auto border-t px-4 pt-3">
            {active && (
              <div className="mb-3 flex items-center gap-3 text-xs text-muted-foreground">
                <Progress value={(finished / items.length) * 100} className="h-1.5" />
                <span className="shrink-0 tabular-nums">
                  {finished}/{items.length}
                </span>
              </div>
            )}
            <ul className="flex flex-col divide-y">
              {items.map((i) => (
                <li key={i.id} className="flex items-center gap-3 py-2.5 text-sm">
                  <span className="grid size-8 shrink-0 place-items-center rounded-md bg-muted text-muted-foreground">
                    {i.status === "uploading" ? (
                      <Spinner />
                    ) : i.status === "done" ? (
                      <CheckIcon className="size-4 text-success" />
                    ) : i.status === "error" ? (
                      <AlertCircleIcon className="size-4 text-destructive" />
                    ) : (
                      <FileTextIcon className="size-4" />
                    )}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium">{i.name ?? i.file.name}</div>
                    <div className={cn("truncate text-xs text-muted-foreground", i.status === "error" && "text-destructive")}>
                      {i.status === "waiting" && "Đang chờ"}
                      {i.status === "uploading" && "Đang đọc và chấm điểm…"}
                      {i.status === "done" && i.file.name}
                      {i.status === "error" && i.error}
                    </div>
                  </div>
                  {i.status === "done" && (
                    <>
                      <ScoreBadge value={i.score} />
                      <Button variant="ghost" size="sm" onClick={() => i.appId && onOpenCandidate(i.appId)}>
                        Xem
                      </Button>
                    </>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        <DialogFooter>
          {finished > 0 && !active && (
            <Button variant="ghost" onClick={clearFinished} className="sm:mr-auto">
              Xoá danh sách
            </Button>
          )}
          <Button variant={active ? "outline" : "default"} onClick={() => onOpenChange(false)}>
            {active ? "Chạy nền" : "Xong"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
