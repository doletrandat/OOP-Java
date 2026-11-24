import { useEffect, useMemo, useState } from "react"

import { applicationsApi, type Application, type ApplicationSummary } from "@/lib/api"

/** Chi tiết hồ sơ đang xem; đổi hồ sơ thì tải lại. */
export function useApplicationDetail(appId: string) {
  const [state, setState] = useState<{ id: string; detail: Application | null; error: string | null }>({
    id: appId,
    detail: null,
    error: null,
  })
  if (state.id !== appId) setState({ id: appId, detail: null, error: null })

  useEffect(() => {
    let cancelled = false
    applicationsApi
      .get(appId)
      .then((a) => !cancelled && setState({ id: appId, detail: a, error: null }))
      .catch((e: Error) => !cancelled && setState({ id: appId, detail: null, error: e.message }))
    return () => {
      cancelled = true
    }
  }, [appId])

  const setDetail = (a: Application) => {
    if (a.id === appId) setState({ id: appId, detail: a, error: null })
  }
  const current = state.id === appId
  return { detail: current ? state.detail : null, error: current ? state.error : null, setDetail }
}

/** Thứ tự duyệt: theo danh sách đang lọc lúc mở; nếu hồ sơ không nằm trong đó thì theo điểm. */
export function useReviewSequence(appId: string, order: string[], list: ApplicationSummary[] | undefined) {
  const seq = useMemo(() => {
    const ids = new Set(list?.map((a) => a.id))
    const o = order.filter((id) => ids.has(id))
    return o.includes(appId)
      ? o
      : [...(list ?? [])].sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1)).map((a) => a.id)
  }, [order, list, appId])
  const idx = seq.indexOf(appId)
  const prevId = idx > 0 ? seq[idx - 1] : undefined
  const nextId = idx >= 0 && idx < seq.length - 1 ? seq[idx + 1] : undefined
  return { seq, idx, prevId, nextId }
}
