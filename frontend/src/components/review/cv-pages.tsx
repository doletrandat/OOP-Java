import { AlertCircleIcon } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { useCvPages } from "@/lib/cv"

/** CV hiển thị dưới dạng ảnh từng trang (trình duyệt nào cũng xem được). */
export function CvPages({ appId }: { appId: string }) {
  const cv = useCvPages(appId)
  const loading = !cv.error && (!cv.total || cv.pages.length < cv.total)
  return (
    <ScrollArea className="h-full">
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-4 p-4 sm:p-6">
        {cv.error && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertTitle>Không mở được CV</AlertTitle>
            <AlertDescription>{cv.error}</AlertDescription>
          </Alert>
        )}
        {cv.pages.map((src, i) => (
          <img
            key={src}
            src={src}
            alt={`Trang ${i + 1} của ${cv.fileName}`}
            className="w-full rounded-md bg-white shadow-sm ring-1 ring-black/10"
          />
        ))}
        {loading && (
          <div className="flex w-full flex-col items-center gap-3">
            <Skeleton className="aspect-[1/1.414] w-full rounded-md" />
            <span className="flex items-center gap-2 text-xs text-muted-foreground">
              <Spinner />
              {cv.total > 1 ? `Đang tải trang ${cv.pages.length + 1}/${cv.total}…` : "Đang tải CV…"}
            </span>
          </div>
        )}
        {!loading && !cv.error && (
          <p className="text-xs text-muted-foreground tabular-nums">
            {cv.fileName} · {cv.total} trang · {cv.sizeKb} KB
          </p>
        )}
      </div>
    </ScrollArea>
  )
}
