import { useEffect, useState } from "react"

import { rawFetch } from "@/lib/api"

/**
 * Tải trang CV (ảnh PNG do server kết xuất) và giữ trong bộ nhớ đệm để chuyển qua lại
 * giữa các ứng viên không phải tải lại. Giữ tối đa 20 CV; CV cũ nhất bị giải phóng trước.
 */
interface CvEntry {
  fileName: string
  sizeKb: number
  total: number
  pages: string[]
  error?: string
  done: boolean
  listeners: Set<() => void>
}

const cache = new Map<string, CvEntry>()
const MAX = 20

function evict() {
  while (cache.size > MAX) {
    const [oldId, old] = cache.entries().next().value as [string, CvEntry]
    if (old.listeners.size) break
    old.pages.forEach((u) => URL.revokeObjectURL(u))
    cache.delete(oldId)
  }
}

function load(appId: string): CvEntry {
  const hit = cache.get(appId)
  if (hit) {
    cache.delete(appId)
    cache.set(appId, hit) // đánh dấu vừa dùng
    return hit
  }
  const entry: CvEntry = { fileName: "", sizeKb: 0, total: 0, pages: [], done: false, listeners: new Set() }
  cache.set(appId, entry)
  evict()
  const emit = () => entry.listeners.forEach((l) => l())
  ;(async () => {
    try {
      const info = await (await rawFetch(`/api/admin/applications/${appId}/cv/info`)).json()
      entry.total = Number(info.pages) || 0
      entry.fileName = info.fileName || "CV.pdf"
      entry.sizeKb = Math.round(Number(info.sizeBytes || 0) / 1024)
      if (!entry.total) throw new Error("File CV không có trang nào đọc được.")
      emit()
      for (let p = 1; p <= entry.total; p++) {
        const blob = await (await rawFetch(`/api/admin/applications/${appId}/cv/pages/${p}`)).blob()
        if (!blob.size) throw new Error(`Không đọc được trang ${p}.`)
        entry.pages = [...entry.pages, URL.createObjectURL(blob)]
        emit()
      }
    } catch (e) {
      entry.error = (e as Error).message
      cache.delete(appId) // lần sau thử lại
    } finally {
      entry.done = true
      emit()
    }
  })()
  return entry
}

export function useCvPages(appId: string | null) {
  const [, force] = useState(0)
  const entry = appId ? load(appId) : null
  useEffect(() => {
    if (!entry) return
    const l = () => force((n) => n + 1)
    entry.listeners.add(l)
    return () => {
      entry.listeners.delete(l)
    }
  }, [entry])
  return entry
}

/** Tải sẵn CV của ứng viên kế tiếp để bấm "Tiếp" là thấy ngay. */
export function prefetchCv(appId: string | undefined) {
  if (appId && !cache.has(appId)) load(appId)
}

export function forgetCv(appId: string) {
  const e = cache.get(appId)
  if (e) e.pages.forEach((u) => URL.revokeObjectURL(u))
  cache.delete(appId)
}

export async function downloadCv(appId: string, fileName: string) {
  const blob = await (await rawFetch(`/api/admin/applications/${appId}/cv`)).blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = fileName || "CV.pdf"
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}

/** Tab mới không gửi được header nên server xác thực bằng cookie HttpOnly đặt lúc đăng nhập. */
export function openCvInNewTab(appId: string) {
  return window.open(`/api/admin/applications/${appId}/cv`, "_blank")
}
