import { useEffect, useState } from "react"
import { InboxIcon, SearchXIcon, UploadIcon } from "lucide-react"
import { toast } from "sonner"

import { DeleteCandidatesDialog, EditContactDialog, type ContactTarget } from "@/components/candidate-dialogs"
import { BulkActionBar } from "@/components/candidates/bulk-action-bar"
import { CandidateRow } from "@/components/candidates/candidate-row"
import { CandidateToolbar } from "@/components/candidates/candidate-toolbar"
import {
  useCandidateList,
  useSelection,
  type Filter,
  type SortKey,
} from "@/components/candidates/use-candidate-list"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { applicationsApi, type ApplicationSummary, type HrDecision, type Job } from "@/lib/api"
import { DECISION_META } from "@/lib/format"
import { useStore } from "@/lib/store"

/** Bảng ứng viên của một vị trí: lọc, chọn nhiều, quyết định nhanh, xoá. */
export function CandidateTable({
  job,
  list,
  filter,
  setFilter,
  query,
  setQuery,
  sort,
  setSort,
  onVisibleChange,
  onOpen,
  onUpload,
}: {
  job: Job
  list: ApplicationSummary[] | undefined
  filter: Filter
  setFilter: (f: Filter) => void
  query: string
  setQuery: (q: string) => void
  sort: SortKey
  setSort: (s: SortKey) => void
  onVisibleChange: (ids: string[]) => void
  onOpen: (id: string) => void
  onUpload: () => void
}) {
  const { decide, removeApps, patchApp, reloadJob } = useStore()
  const [deleting, setDeleting] = useState<ApplicationSummary[] | null>(null)
  const [editing, setEditing] = useState<ContactTarget | null>(null)
  const { rank, visible } = useCandidateList(list, filter, query, sort)
  const visibleIds = visible.map((a) => a.id)
  const selection = useSelection(visibleIds)

  const visibleKey = visibleIds.join(",")
  useEffect(() => {
    onVisibleChange(visibleKey ? visibleKey.split(",") : [])
  }, [visibleKey, onVisibleChange])

  async function setDecision(ids: string[], decision: HrDecision) {
    try {
      await decide(job.id, ids, decision)
      selection.clear()
      const label = DECISION_META[decision].label.toLowerCase()
      toast.success(ids.length === 1 ? `Đã chuyển sang "${label}".` : `Đã chuyển ${ids.length} ứng viên sang "${label}".`)
    } catch {
      /* decide đã báo lỗi */
    }
  }

  async function deleteRows(rows: ApplicationSummary[]) {
    setDeleting(null)
    const ids = rows.map((r) => r.id)
    removeApps(job.id, ids)
    selection.clear()
    const results = await Promise.allSettled(ids.map((id) => applicationsApi.remove(id)))
    const failed = results.filter((r) => r.status === "rejected").length
    if (failed) {
      toast.error(`Không xoá được ${failed} ứng viên.`)
      reloadJob(job.id)
    } else {
      toast.success(ids.length === 1 ? `Đã xoá ${rows[0].candidateName}.` : `Đã xoá ${ids.length} ứng viên.`)
    }
  }

  const allChecked = visible.length > 0 && visible.every((a) => selection.selected.has(a.id))
  const someChecked = !allChecked && visible.some((a) => selection.selected.has(a.id))
  const selectedRows = visible.filter((a) => selection.selected.has(a.id))
  const selectedIds = selectedRows.map((a) => a.id)

  if (list && list.length === 0) {
    return (
      <Card>
        <Empty>
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <InboxIcon />
            </EmptyMedia>
            <EmptyTitle>Chưa có CV nào</EmptyTitle>
            <EmptyDescription>
              Tải các file PDF lên, hệ thống tự đọc tên, liên hệ và chấm điểm từng CV.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button onClick={onUpload}>
              <UploadIcon data-icon="inline-start" />
              Chọn file CV
            </Button>
          </EmptyContent>
        </Empty>
      </Card>
    )
  }

  return (
    <>
      <CandidateToolbar
        list={list}
        filter={filter}
        setFilter={setFilter}
        query={query}
        setQuery={setQuery}
        sort={sort}
        setSort={setSort}
      />

      <Card className="py-0">
        {list && visible.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <SearchXIcon />
              </EmptyMedia>
              <EmptyTitle>Không có ứng viên phù hợp</EmptyTitle>
              <EmptyDescription>
                {query ? `Không ai khớp với "${query}"` : "Chưa có ai ở trạng thái này"}
                {filter !== "all" && query ? " trong mục đang lọc" : ""}.
              </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <Button
                variant="outline"
                onClick={() => {
                  setQuery("")
                  setFilter("all")
                }}
              >
                Xem tất cả ứng viên
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50 hover:bg-muted/50">
                <TableHead className="w-10 pl-4">
                  <Checkbox
                    aria-label="Chọn tất cả"
                    checked={allChecked ? true : someChecked ? "indeterminate" : false}
                    onCheckedChange={(c) => selection.setAll(!!c)}
                  />
                </TableHead>
                <TableHead className="w-8 text-right">#</TableHead>
                <TableHead>Ứng viên</TableHead>
                <TableHead className="text-center">Điểm</TableHead>
                <TableHead className="hidden text-right md:table-cell">Kỹ năng</TableHead>
                <TableHead className="hidden text-right lg:table-cell">Tiêu chí</TableHead>
                <TableHead className="hidden text-center lg:table-cell">AI</TableHead>
                <TableHead className="hidden sm:table-cell">Trạng thái</TableHead>
                <TableHead className="hidden xl:table-cell">Tải lên</TableHead>
                <TableHead className="w-28 pr-4">
                  <span className="sr-only">Thao tác</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!list &&
                Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell colSpan={10} className="px-4">
                      <Skeleton className="h-10 w-full" />
                    </TableCell>
                  </TableRow>
                ))}
              {visible.map((a) => (
                <CandidateRow
                  key={a.id}
                  app={a}
                  rank={rank.get(a.id)}
                  checked={selection.selected.has(a.id)}
                  onCheckedChange={(c) => selection.toggle(a.id, c)}
                  onOpen={() => onOpen(a.id)}
                  onDecide={(d) => setDecision([a.id], d)}
                  onEdit={() => setEditing(a)}
                  onDelete={() => setDeleting([a])}
                />
              ))}
            </TableBody>
          </Table>
        )}
      </Card>

      <BulkActionBar
        count={selectedRows.length}
        onShortlist={() => setDecision(selectedIds, "SHORTLISTED")}
        onReject={() => setDecision(selectedIds, "REJECTED")}
        onDelete={() => setDeleting(selectedRows)}
        onClear={selection.clear}
      />

      <DeleteCandidatesDialog
        names={deleting?.map((d) => d.candidateName) ?? null}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && deleteRows(deleting)}
      />
      <EditContactDialog
        target={editing}
        onClose={() => setEditing(null)}
        onSaved={(app) => {
          patchApp(job.id, app.id, { candidateName: app.candidateName, email: app.email, phone: app.phone })
          setEditing(null)
        }}
      />
    </>
  )
}
