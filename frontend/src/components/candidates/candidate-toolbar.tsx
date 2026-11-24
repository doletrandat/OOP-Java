import { SearchIcon, XIcon } from "lucide-react"

import type { Filter, SortKey } from "@/components/candidates/use-candidate-list"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import type { ApplicationSummary } from "@/lib/api"
import { pipeline } from "@/lib/pipeline"

/** Lọc theo trạng thái, tìm kiếm và sắp xếp danh sách ứng viên. */
export function CandidateToolbar({
  list,
  filter,
  setFilter,
  query,
  setQuery,
  sort,
  setSort,
}: {
  list: ApplicationSummary[] | undefined
  filter: Filter
  setFilter: (f: Filter) => void
  query: string
  setQuery: (q: string) => void
  sort: SortKey
  setSort: (s: SortKey) => void
}) {
  const p = pipeline(list)
  return (
    <div className="mb-3 flex flex-wrap items-center gap-2">
      <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)}>
        <TabsList>
          <TabsTrigger value="all">
            Tất cả <Count n={list ? p.total : null} />
          </TabsTrigger>
          <TabsTrigger value="NEW">
            Chưa xử lý <Count n={list ? p.NEW : null} />
          </TabsTrigger>
          <TabsTrigger value="SHORTLISTED">
            <span className="hidden sm:inline">Mời phỏng vấn</span>
            <span className="sm:hidden">Mời PV</span> <Count n={list ? p.SHORTLISTED : null} />
          </TabsTrigger>
          <TabsTrigger value="REJECTED">
            Từ chối <Count n={list ? p.REJECTED : null} />
          </TabsTrigger>
        </TabsList>
      </Tabs>
      <div className="ml-auto flex w-full items-center gap-2 sm:w-auto">
        <InputGroup className="sm:w-64">
          <InputGroupAddon>
            <SearchIcon />
          </InputGroupAddon>
          <InputGroupInput
            placeholder="Tìm tên, email, số điện thoại…"
            aria-label="Tìm ứng viên"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <InputGroupAddon align="inline-end">
            {query && (
              <button
                type="button"
                aria-label="Xoá tìm kiếm"
                className="rounded-sm p-0.5 hover:bg-muted"
                onClick={() => setQuery("")}
              >
                <XIcon className="size-3.5" />
              </button>
            )}
          </InputGroupAddon>
        </InputGroup>
        <Select value={sort} onValueChange={(v) => setSort(v as SortKey)}>
          <SelectTrigger className="w-40 shrink-0" aria-label="Sắp xếp">
            <SelectValue />
          </SelectTrigger>
          <SelectContent align="end">
            <SelectItem value="score">Điểm cao trước</SelectItem>
            <SelectItem value="recent">Mới tải lên</SelectItem>
            <SelectItem value="name">Tên A đến Z</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  )
}

function Count({ n }: { n: number | null }) {
  if (n == null) return null
  return <span className="text-muted-foreground tabular-nums">{n}</span>
}
