import { useCallback, useEffect, useRef, useState, type DragEvent } from "react"
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
import { api, post, type Application } from "@/lib/api"
import { cn } from "@/lib/utils"

export interface UploadItem {
  id: number
  file: File
  status: "waiting" | "uploading" | "done" | "error"
  appId?: string
  name?: string
  score?: number | null
  error?: string
}

let seq = 0

/**
 * Hàng đợi tải CV: tải lần lượt từng file (chấm AI tốn thời gian, gửi song song dễ quá tải).
 * Tên, email, số điện thoại do server tự đọc từ CV.
 */
export function useUploadQueue(jobId: string, onUploaded: () => Promise<void> | void) {
  const [items, setItems] = useState<UploadItem[]>([])
  const running = useRef(false)
  const onUploadedRef = useRef(onUploaded)
  onUploadedRef.current = onUploaded

  const add = useCallback((files: FileList | File[]) => {
    const next = Array.from(files).map<UploadItem>((file) =>
      /\.pdf$/i.test(file.name) || file.type === "application/pdf"
        ? { id: ++seq, file, status: "waiting" }
        : { id: ++seq, file, status: "error", error: "Không phải file PDF" }
    )
    setItems((prev) => [...prev, ...next])
  }, [])

  useEffect(() => {
    if (running.current) return
    const next = items.find((i) => i.status === "waiting")
    if (!next) return
    running.current = true
    const update = (p: Partial<UploadItem>) => setItems((prev) => prev.map((i) => (i.id === next.id ? { ...i, ...p } : i)))
    update({ status: "uploading" })
    ;(async () => {
      try {
        const fd = new FormData()
        fd.append("file", next.file)
        const app = await api<Application>(`/api/admin/jobs/${jobId}/cv`, post(fd))
        update({
          status: "done",
          appId: app.id,
          name: app.candidateName,
          score: app.report ? app.report.finalScore : null,
        })
        await onUploadedRef.current()
      } catch (e) {
        update({ status: "error", error: (e as Error).message })
      } finally {
        running.current = false
        setItems((prev) => [...prev]) // chạy lại effect để lấy file kế tiếp
      }
    })()
  }, [items, jobId])

  const clearFinished = () => setItems((prev) => prev.filter((i) => i.status === "waiting" || i.status === "uploading"))
  const finished = items.filter((i) => i.status === "done" || i.status === "error").length
  const active = items.some((i) => i.status === "waiting" || i.status === "uploading")

  return { items, add, clearFinished, finished, active }
}

export function UploadDialog({
  open,
  onOpenChange,
  queue,
  onOpenCandidate,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  queue: ReturnType<typeof useUploadQueue>
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

/** Lớp phủ khi kéo file vào bất kỳ đâu trên trang vị trí. */
export function DropOverlay({ show }: { show: boolean }) {
  if (!show) return null
  return (
    <div className="pointer-events-none fixed inset-0 z-50 grid place-items-center bg-background/80 p-6">
      <div className="flex flex-col items-center gap-3 rounded-2xl border-2 border-dashed border-primary bg-card px-10 py-12 text-center shadow-lg">
        <UploadIcon className="size-8 text-primary" />
        <div className="text-base font-semibold">Thả file để tải CV lên</div>
        <div className="text-sm text-muted-foreground">Có thể thả nhiều file PDF cùng lúc.</div>
      </div>
    </div>
  )
}
