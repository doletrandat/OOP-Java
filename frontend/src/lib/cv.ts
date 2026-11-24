import { useEffect, useState } from "react"

import { cvApi } from "@/lib/api"

export interface CvPagesState {
  fileName: string
  sizeKb: number
  total: number
  /** Ảnh từng trang (object URL), tải lần lượt nên có thể chưa đủ. */
  pages: string[]
  error?: string
}

const EMPTY: CvPagesState = { fileName: "", sizeKb: 0, total: 0, pages: [] }

/** Tải CV dưới dạng ảnh từng trang do server kết xuất. Đổi hồ sơ thì tải lại từ đầu. */
export function useCvPages(appId: string) {
  const [state, setState] = useState<{ id: string; cv: CvPagesState }>({ id: appId, cv: EMPTY })
  if (state.id !== appId) setState({ id: appId, cv: EMPTY })

  useEffect(() => {
    let cancelled = false
    const urls: string[] = []
    const update = (patch: Partial<CvPagesState>) =>
      !cancelled && setState((s) => (s.id === appId ? { id: appId, cv: { ...s.cv, ...patch } } : s))
    ;(async () => {
      try {
        const info = await cvApi.info(appId)
        const total = Number(info.pages) || 0
        update({
          total,
          fileName: info.fileName || "CV.pdf",
          sizeKb: Math.round(Number(info.sizeBytes || 0) / 1024),
        })
        if (!total) throw new Error("File CV không có trang nào đọc được.")
        for (let p = 1; p <= total && !cancelled; p++) {
          const blob = await cvApi.page(appId, p)
          if (!blob.size) throw new Error(`Không đọc được trang ${p}.`)
          urls.push(URL.createObjectURL(blob))
          update({ pages: [...urls] })
        }
      } catch (e) {
        update({ error: (e as Error).message })
      }
    })()
    return () => {
      cancelled = true
      urls.forEach((u) => URL.revokeObjectURL(u))
    }
  }, [appId])

  return state.id === appId ? state.cv : EMPTY
}

/** Tải file PDF gốc về máy. */
export async function downloadCv(appId: string, fileName: string) {
  const url = URL.createObjectURL(await cvApi.pdf(appId))
  const a = document.createElement("a")
  a.href = url
  a.download = fileName || "CV.pdf"
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}
