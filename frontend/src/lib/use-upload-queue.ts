import { useCallback, useEffect, useRef, useState } from "react"

import { applicationsApi } from "@/lib/api"

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
export type UploadQueue = ReturnType<typeof useUploadQueue>

export function useUploadQueue(jobId: string, onUploaded: () => Promise<void> | void) {
  const [items, setItems] = useState<UploadItem[]>([])
  const running = useRef(false)
  const onUploadedRef = useRef(onUploaded)
  useEffect(() => {
    onUploadedRef.current = onUploaded
  }, [onUploaded])

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
        const app = await applicationsApi.upload(jobId, fd)
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
