import { useEffect, useMemo, useRef, useState } from "react"
import {
  CheckIcon,
  ExternalLinkIcon,
  InboxIcon,
  MoreHorizontalIcon,
  PencilIcon,
  RotateCcwIcon,
  SearchIcon,
  SearchXIcon,
  Trash2Icon,
  UploadIcon,
  XIcon,
} from "lucide-react"
import { toast } from "sonner"

import { DeleteCandidatesDialog, EditContactDialog, type ContactTarget } from "@/components/candidate-dialogs"
import { DecisionBadge, ScoreBadge } from "@/components/status"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Kbd } from "@/components/ui/kbd"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { api, type ApplicationSummary, type HrDecision, type Job } from "@/lib/api"
import { forgetCv, openCvInNewTab } from "@/lib/cv-cache"
import { AI_STATUS_LABEL, DECISION_META, fmtScore, fmtTime, fold, initials, relativeTime } from "@/lib/format"
import { pipeline, useStore } from "@/lib/store"
import { cn } from "@/lib/utils"

export type Filter = "all" | HrDecision
export type SortKey = "score" | "recent" | "name"

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
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [deleting, setDeleting] = useState<ApplicationSummary[] | null>(null)
  const [editing, setEditing] = useState<ContactTarget | null>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const p = pipeline(list)

  // Hạng theo điểm trên toàn bộ ứng viên, không đổi khi lọc
  const rank = useMemo(() => {
    const m = new Map<string, number>()
    ;[...(list ?? [])]
      .sort((a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1))
      .forEach((a, i) => m.set(a.id, i + 1))
    return m
  }, [list])

  const visible = useMemo(() => {
    const q = fold(query.trim())
    const rows = (list ?? []).filter(
      (a) =>
        (filter === "all" || a.hrDecision === filter) &&
        (!q || fold(`${a.candidateName} ${a.email ?? ""} ${a.phone ?? ""}`).includes(q))
    )
    const by: Record<SortKey, (a: ApplicationSummary, b: ApplicationSummary) => number> = {
      score: (a, b) => (b.finalScore ?? -1) - (a.finalScore ?? -1),
      recent: (a, b) => (b.submittedAt ?? "").localeCompare(a.submittedAt ?? ""),
      name: (a, b) => a.candidateName.localeCompare(b.candidateName, "vi"),
    }
    return rows.sort(by[sort])
  }, [list, filter, query, sort])

  const visibleKey = visible.map((a) => a.id).join(",")
  useEffect(() => {
    onVisibleChange(visibleKey ? visibleKey.split(",") : [])
  }, [visibleKey, onVisibleChange])

  // Bỏ chọn những dòng đã biến mất (bị xoá hoặc lọc đi)
  useEffect(() => {
    setSelected((s) => {
      const ids = new Set(visibleKey.split(","))
      const next = new Set([...s].filter((id) => ids.has(id)))
      return next.size === s.size ? s : next
    })
  }, [visibleKey])

  // Phím "/" để tìm kiếm
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement
      if (e.key === "/" && !/INPUT|TEXTAREA|SELECT/.test(t.tagName) && !t.isContentEditable) {
        e.preventDefault()
        searchRef.current?.focus()
      }
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [])

  async function setDecision(ids: string[], decision: HrDecision) {
    const prev = new Map((list ?? []).map((a) => [a.id, a.hrDecision]))
    try {
      await decide(job.id, ids, decision)
      setSelected(new Set())
      const label = DECISION_META[decision].label.toLowerCase()
      toast.success(ids.length === 1 ? `Đã chuyển sang "${label}".` : `Đã chuyển ${ids.length} ứng viên sang "${label}".`, {
        duration: 8000,
        action: {
          label: "Hoàn tác",
          onClick: () => {
            const groups = new Map<HrDecision, string[]>()
            ids.forEach((id) => {
              const d = prev.get(id) ?? "NEW"
              groups.set(d, [...(groups.get(d) ?? []), id])
            })
            groups.forEach((g, d) => decide(job.id, g, d).catch(() => {}))
          },
        },
      })
    } catch {
      /* decide đã báo lỗi */
    }
  }

  async function deleteRows(rows: ApplicationSummary[]) {
    setDeleting(null)
    const ids = rows.map((r) => r.id)
    removeApps(job.id, ids)
    setSelected(new Set())
    const results = await Promise.allSettled(ids.map((id) => api(`/api/admin/applications/${id}`, { method: "DELETE" })))
    ids.forEach(forgetCv)
    const failed = results.filter((r) => r.status === "rejected").length
    if (failed) {
      toast.error(`Không xoá được ${failed} ứng viên.`)
      reloadJob(job.id)
    } else {
      toast.success(ids.length === 1 ? `Đã xoá ${rows[0].candidateName}.` : `Đã xoá ${ids.length} ứng viên.`)
    }
  }

  const allChecked = visible.length > 0 && visible.every((a) => selected.has(a.id))
  const someChecked = !allChecked && visible.some((a) => selected.has(a.id))
  const selectedRows = visible.filter((a) => selected.has(a.id))

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
              Kéo các file PDF vào bất kỳ đâu trên trang này. Hệ thống tự đọc tên, liên hệ và chấm điểm từng CV.
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
              ref={searchRef}
              placeholder="Tìm tên, email, số điện thoại…"
              aria-label="Tìm ứng viên"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Escape" && setQuery("")}
            />
            <InputGroupAddon align="inline-end">
              {query ? (
                <button
                  type="button"
                  aria-label="Xoá tìm kiếm"
                  className="rounded-sm p-0.5 hover:bg-muted"
                  onClick={() => setQuery("")}
                >
                  <XIcon className="size-3.5" />
                </button>
              ) : (
                <Kbd className="hidden sm:inline-flex">/</Kbd>
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
                    onCheckedChange={(c) => setSelected(c ? new Set(visible.map((a) => a.id)) : new Set())}
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
              {visible.map((a) => {
                const checked = selected.has(a.id)
                return (
                  <TableRow
                    key={a.id}
                    data-state={checked ? "selected" : undefined}
                    tabIndex={0}
                    className="group cursor-pointer"
                    aria-label={`Mở hồ sơ ${a.candidateName}`}
                    onClick={(e) => {
                      if ((e.target as HTMLElement).closest("button,[role=checkbox],[role=menu],a")) return
                      onOpen(a.id)
                    }}
                    onKeyDown={(e) => {
                      if ((e.key === "Enter" || e.key === " ") && e.target === e.currentTarget) {
                        e.preventDefault()
                        onOpen(a.id)
                      }
                    }}
                  >
                    <TableCell className="pl-4">
                      <Checkbox
                        aria-label={`Chọn ${a.candidateName}`}
                        checked={checked}
                        onCheckedChange={(c) =>
                          setSelected((s) => {
                            const n = new Set(s)
                            if (c) n.add(a.id)
                            else n.delete(a.id)
                            return n
                          })
                        }
                      />
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground tabular-nums">{rank.get(a.id)}</TableCell>
                    <TableCell className="py-2.5">
                      <div className="flex items-center gap-3">
                        <Avatar className="size-8">
                          <AvatarFallback className="text-xs">{initials(a.candidateName)}</AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <div className="truncate font-medium">{a.candidateName}</div>
                          <div className="truncate text-xs text-muted-foreground">
                            {[a.email, a.phone].filter(Boolean).join(" · ") || "Chưa có liên hệ"}
                          </div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <ScoreBadge value={a.finalScore} large />
                    </TableCell>
                    <TableCell className="hidden text-right tabular-nums md:table-cell">
                      {a.matchedCount != null ? (
                        <>
                          {a.matchedCount}
                          <span className="text-muted-foreground">/{a.totalSkills}</span>
                        </>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                    <TableCell className="hidden text-right tabular-nums lg:table-cell">
                      {a.criteriaTotal ? (
                        <>
                          {a.criteriaMet}
                          <span className="text-muted-foreground">/{a.criteriaTotal}</span>
                        </>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell className="hidden text-center tabular-nums lg:table-cell">
                      {a.aiStatus === "OK" ? (
                        <span className="text-muted-foreground">{fmtScore(a.aiScore)}</span>
                      ) : (
                        <Badge variant={a.aiStatus === "ERROR" ? "destructive" : "secondary"}>
                          {AI_STATUS_LABEL[a.aiStatus] ?? "—"}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="hidden sm:table-cell">
                      <DecisionBadge decision={a.hrDecision} />
                    </TableCell>
                    <TableCell className="hidden text-xs text-muted-foreground xl:table-cell" title={fmtTime(a.submittedAt)}>
                      {relativeTime(a.submittedAt)}
                    </TableCell>
                    <TableCell className="pr-4">
                      <div className="flex items-center justify-end gap-0.5">
                        <div className="flex gap-0.5 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100 sm:opacity-0 [@media(hover:none)]:opacity-100">
                          <QuickAction
                            label="Mời phỏng vấn"
                            active={a.hrDecision === "SHORTLISTED"}
                            onClick={() => setDecision([a.id], a.hrDecision === "SHORTLISTED" ? "NEW" : "SHORTLISTED")}
                            className="data-[active=true]:bg-success/15 data-[active=true]:text-success-foreground"
                          >
                            <CheckIcon />
                          </QuickAction>
                          <QuickAction
                            label="Từ chối"
                            active={a.hrDecision === "REJECTED"}
                            onClick={() => setDecision([a.id], a.hrDecision === "REJECTED" ? "NEW" : "REJECTED")}
                            className="data-[active=true]:bg-destructive/10 data-[active=true]:text-destructive"
                          >
                            <XIcon />
                          </QuickAction>
                        </div>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon-sm" aria-label={`Thao tác với ${a.candidateName}`}>
                              <MoreHorizontalIcon />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48">
                            <DropdownMenuItem onSelect={() => openCvInNewTab(a.id)}>
                              <ExternalLinkIcon /> Mở CV trong tab mới
                            </DropdownMenuItem>
                            <DropdownMenuItem onSelect={() => setEditing(a)}>
                              <PencilIcon /> Sửa thông tin
                            </DropdownMenuItem>
                            {a.hrDecision !== "NEW" && (
                              <DropdownMenuItem onSelect={() => setDecision([a.id], "NEW")}>
                                <RotateCcwIcon /> Đặt lại "chưa xử lý"
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem variant="destructive" onSelect={() => setDeleting([a])}>
                              <Trash2Icon /> Xoá ứng viên
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        )}
      </Card>

      {/* Thanh thao tác hàng loạt */}
      <div
        className={cn(
          "fixed inset-x-0 bottom-4 z-40 mx-auto flex w-fit max-w-[calc(100%-2rem)] items-center gap-1 rounded-xl border bg-popover p-1.5 pl-4 text-sm shadow-lg transition-[opacity,transform] duration-200 ease-out",
          selectedRows.length ? "translate-y-0 opacity-100" : "pointer-events-none translate-y-2 opacity-0"
        )}
        role="toolbar"
        aria-label="Thao tác với ứng viên đã chọn"
        aria-hidden={!selectedRows.length}
      >
        <span className="mr-2 font-medium tabular-nums">Đã chọn {selectedRows.length}</span>
        <Button size="sm" variant="ghost" onClick={() => setDecision(selectedRows.map((a) => a.id), "SHORTLISTED")}>
          <CheckIcon data-icon="inline-start" className="text-success" />
          <span className="hidden sm:inline">Mời phỏng vấn</span>
          <span className="sm:hidden">Mời PV</span>
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setDecision(selectedRows.map((a) => a.id), "REJECTED")}>
          <XIcon data-icon="inline-start" className="text-destructive" />
          Từ chối
        </Button>
        <Button size="sm" variant="ghost" className="hidden sm:inline-flex" onClick={() => setDeleting(selectedRows)}>
          <Trash2Icon data-icon="inline-start" />
          Xoá
        </Button>
        <Button size="icon-sm" variant="ghost" aria-label="Bỏ chọn" onClick={() => setSelected(new Set())}>
          <XIcon />
        </Button>
      </div>

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

function Count({ n }: { n: number | null }) {
  if (n == null) return null
  return <span className="text-muted-foreground tabular-nums">{n}</span>
}

function QuickAction({
  label,
  active,
  onClick,
  className,
  children,
}: {
  label: string
  active: boolean
  onClick: () => void
  className?: string
  children: React.ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={active ? `Bỏ "${label}"` : label}
          aria-pressed={active}
          data-active={active}
          onClick={onClick}
          className={cn(className)}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{active ? `Bỏ "${label}"` : label}</TooltipContent>
    </Tooltip>
  )
}
