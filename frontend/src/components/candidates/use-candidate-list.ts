import { useMemo, useState } from "react"

import type { ApplicationSummary, HrDecision } from "@/lib/api"
import { fold } from "@/lib/format"

export type Filter = "all" | HrDecision
export type SortKey = "score" | "recent" | "name"

const SORTERS: Record<SortKey, (a: ApplicationSummary, b: ApplicationSummary) => number> = {
  score: (a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1),
  recent: (a, b) => (b.submittedAt ?? "").localeCompare(a.submittedAt ?? ""),
  name: (a, b) => a.candidateName.localeCompare(b.candidateName, "vi"),
}

/**
 * Danh sách ứng viên sau khi lọc, tìm và sắp xếp, kèm hạng theo điểm trên toàn bộ
 * ứng viên (không đổi khi lọc).
 */
export function useCandidateList(
  list: ApplicationSummary[] | undefined,
  filter: Filter,
  query: string,
  sort: SortKey
) {
  const rank = useMemo(() => {
    const m = new Map<string, number>()
    ;[...(list ?? [])].sort(SORTERS.score).forEach((a, i) => m.set(a.id, i + 1))
    return m
  }, [list])

  const visible = useMemo(() => {
    const q = fold(query.trim())
    return (list ?? [])
      .filter(
        (a) =>
          (filter === "all" || a.hrDecision === filter) &&
          (!q || fold(`${a.candidateName} ${a.email ?? ""} ${a.phone ?? ""}`).includes(q))
      )
      .sort(SORTERS[sort])
  }, [list, filter, query, sort])

  return { rank, visible }
}

/**
 * Các dòng đang được chọn. Dòng biến mất khỏi danh sách hiển thị (bị xoá hoặc bị lọc đi)
 * thì tự bỏ chọn.
 */
export function useSelection(visibleIds: string[]) {
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const key = visibleIds.join(",")
  const [prevKey, setPrevKey] = useState(key)
  if (prevKey !== key) {
    setPrevKey(key)
    const ids = new Set(visibleIds)
    const next = new Set([...selected].filter((id) => ids.has(id)))
    if (next.size !== selected.size) setSelected(next)
  }

  const toggle = (id: string, on: boolean) =>
    setSelected((s) => {
      const n = new Set(s)
      if (on) n.add(id)
      else n.delete(id)
      return n
    })
  const setAll = (on: boolean) => setSelected(on ? new Set(visibleIds) : new Set())
  const clear = () => setSelected(new Set())
  return { selected, toggle, setAll, clear }
}
