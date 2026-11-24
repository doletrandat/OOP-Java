import { AlertTriangleIcon, CheckIcon, SparklesIcon } from "lucide-react"

import { CriterionStatusBadge, ScoreProgress, SkillStatusBadge } from "@/components/status"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import type { MatchReport } from "@/lib/api"
import { AI_STATUS_LABEL, criterionTypeLabel, fmtScore, fmtTime, scoreText, scoreTone, WEIGHT_LABEL } from "@/lib/format"
import { cn } from "@/lib/utils"

/* ---------- Khung đánh giá ---------- */

const SKILL_ORDER = { MISSING: 0, UNCERTAIN: 1, MATCHED: 2 } as const
const CRIT_ORDER = { NOT_MET: 0, UNKNOWN: 1, PARTIAL: 2, MET: 3 } as const

/** Kết quả chấm điểm: điểm tổng, nhận xét AI, kỹ năng và tiêu chí kèm trích dẫn. */
export function AnalysisPanel({ report: r }: { report: MatchReport }) {
  const skills = [...(r.skillMatches ?? [])].sort(
    (a, b) =>
      (a.weight === "REQUIRED" ? 0 : 1) - (b.weight === "REQUIRED" ? 0 : 1) ||
      SKILL_ORDER[a.status] - SKILL_ORDER[b.status]
  )
  const criteria = [...(r.criteriaResults ?? [])].sort((a, b) => CRIT_ORDER[a.status] - CRIT_ORDER[b.status])
  const matched = skills.filter((s) => s.status === "MATCHED").length
  const met = criteria.filter((c) => c.status === "MET").length
  const missingRequired = skills.filter((s) => s.weight === "REQUIRED" && s.status === "MISSING")

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end gap-4 rounded-xl border bg-card p-4">
        <div>
          <div className="text-xs text-muted-foreground">Điểm tổng</div>
          <div className={cn("text-4xl font-bold tracking-tight tabular-nums", scoreText[scoreTone(r.finalScore)])}>
            {fmtScore(r.finalScore)}
            <span className="ml-1 text-sm font-medium text-muted-foreground">/100</span>
          </div>
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-2 pb-1">
          <MiniScore label="Kỹ năng" value={r.keywordScore} />
          <MiniScore label="AI" value={r.aiStatus === "OK" ? r.aiScore : null} note={r.aiStatus !== "OK" ? AI_STATUS_LABEL[r.aiStatus] : undefined} />
          {criteria.length > 0 && <MiniScore label="Tiêu chí" value={r.criteriaScore >= 0 ? r.criteriaScore : null} />}
        </div>
      </div>

      {missingRequired.length > 0 && (
        <Alert className="border-destructive/30 bg-destructive/5">
          <AlertTriangleIcon className="text-destructive" />
          <AlertTitle>Thiếu {missingRequired.length} kỹ năng bắt buộc</AlertTitle>
          <AlertDescription>{missingRequired.map((s) => s.skillName).join(", ")}</AlertDescription>
        </Alert>
      )}

      <Tabs defaultValue="summary">
        <TabsList className="w-full">
          <TabsTrigger value="summary">Tóm tắt</TabsTrigger>
          <TabsTrigger value="skills">
            Kỹ năng <span className="text-muted-foreground tabular-nums">{matched}/{skills.length}</span>
          </TabsTrigger>
          {criteria.length > 0 && (
            <TabsTrigger value="criteria">
              Tiêu chí <span className="text-muted-foreground tabular-nums">{met}/{criteria.length}</span>
            </TabsTrigger>
          )}
        </TabsList>

        <TabsContent value="summary" className="mt-3 flex flex-col gap-4">
          {r.aiStatus === "OK" ? (
            <>
              {r.aiSummary && (
                <div>
                  <h3 className="mb-1.5 flex items-center gap-1.5 text-sm font-medium">
                    <SparklesIcon className="size-3.5 text-primary" /> Nhận xét của AI
                  </h3>
                  <p className="text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground">{r.aiSummary}</p>
                </div>
              )}
              {(r.aiHighlights?.length ?? 0) > 0 && <Points title="Điểm mạnh" items={r.aiHighlights!} good />}
              {(r.aiConcerns?.length ?? 0) > 0 && <Points title="Nên hỏi thêm khi phỏng vấn" items={r.aiConcerns!} />}
              {(r.aiDimensions?.length ?? 0) > 0 && (
                <div>
                  <h3 className="mb-2 text-sm font-medium">Điểm theo từng mặt</h3>
                  <div className="flex flex-col divide-y rounded-lg border">
                    {r.aiDimensions!.map((d) => (
                      <div key={d.name} className="p-3">
                        <div className="flex items-baseline justify-between gap-2 text-sm">
                          <span>{d.name}</span>
                          <span className="font-semibold tabular-nums">{fmtScore(d.score)}</span>
                        </div>
                        <ScoreProgress value={d.score} className="my-1.5" />
                        {d.comment && <p className="text-xs text-muted-foreground">{d.comment}</p>}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            <Alert>
              <AlertTriangleIcon />
              <AlertTitle>Chưa có nhận xét của AI ({AI_STATUS_LABEL[r.aiStatus] ?? r.aiStatus})</AlertTitle>
              <AlertDescription>
                {r.aiError && <p>{r.aiError}</p>}
                {r.aiStatus === "NOT_CONFIGURED" ? (
                  <p>
                    Điểm hiện chỉ dựa trên từ khoá và tiêu chí. Đặt <code>AI_API_KEY</code> trong{" "}
                    <code>config.properties</code> rồi khởi động lại server để bật AI.
                  </p>
                ) : (
                  <p>Thử "Chấm lại CV này" trong menu ở góc trên.</p>
                )}
              </AlertDescription>
            </Alert>
          )}
          <p className="text-xs text-muted-foreground">
            Chấm lúc {fmtTime(r.scoredAt)}. Điểm chỉ để tham khảo, HR là người quyết định.
          </p>
        </TabsContent>

        <TabsContent value="skills" className="mt-3">
          <ul className="flex flex-col divide-y rounded-lg border">
            {skills.map((s) => (
              <li key={s.skillName} className="p-3">
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="mr-auto font-medium">{s.skillName}</span>
                  {s.weight === "REQUIRED" && <Badge variant="outline">Bắt buộc</Badge>}
                  <SkillStatusBadge status={s.status} />
                </div>
                {s.evidence && <Quote text={s.evidence} />}
                {s.aiNote && (
                  <p className="mt-1.5 flex gap-1.5 text-xs text-muted-foreground">
                    <SparklesIcon className="mt-0.5 size-3 shrink-0" />
                    {s.aiNote}
                  </p>
                )}
                {!s.evidence && !s.aiNote && (
                  <p className="mt-1 text-xs text-muted-foreground">Không thấy đoạn nào trong CV nhắc tới.</p>
                )}
              </li>
            ))}
          </ul>
        </TabsContent>

        {criteria.length > 0 && (
          <TabsContent value="criteria" className="mt-3">
            <ul className="flex flex-col divide-y rounded-lg border">
              {criteria.map((c) => (
                <li key={c.name + c.requirement} className="p-3">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="mr-auto font-medium">{c.name}</span>
                    {c.source === "AI" && (
                      <Badge variant={c.inferred ? "warning" : "info"}>{c.inferred ? "AI suy ra" : "Theo AI"}</Badge>
                    )}
                    <CriterionStatusBadge status={c.status} />
                  </div>
                  <dl className="mt-1.5 grid grid-cols-[5rem_1fr] gap-x-2 gap-y-0.5 text-xs">
                    <dt className="text-muted-foreground">Yêu cầu</dt>
                    <dd>
                      {c.requirement}{" "}
                      <span className="text-muted-foreground">
                        ({criterionTypeLabel(c.type).toLowerCase()}, {(WEIGHT_LABEL[c.weight] ?? "").toLowerCase()})
                      </span>
                    </dd>
                    <dt className="text-muted-foreground">Trong CV</dt>
                    <dd>{c.actual || <span className="text-muted-foreground">Không ghi</span>}</dd>
                  </dl>
                  {c.note && <p className="mt-1 text-xs text-muted-foreground">{c.note}</p>}
                  {c.evidence && <Quote text={c.evidence} />}
                </li>
              ))}
            </ul>
          </TabsContent>
        )}
      </Tabs>
    </div>
  )
}

function MiniScore({ label, value, note }: { label: string; value: number | null; note?: string }) {
  return (
    <div className="grid grid-cols-[3.5rem_1fr_2rem] items-center gap-2 text-xs">
      <span className="text-muted-foreground">{label}</span>
      {note ? <span className="truncate text-muted-foreground">{note}</span> : <ScoreProgress value={value} />}
      <span className="text-right font-medium tabular-nums">{fmtScore(value)}</span>
    </div>
  )
}

function Quote({ text }: { text: string }) {
  return (
    <blockquote className="mt-2 rounded-r-md border-l-2 bg-muted/60 px-2.5 py-1.5 text-xs leading-relaxed text-muted-foreground">
      "{text}"
    </blockquote>
  )
}

function Points({ title, items, good }: { title: string; items: string[]; good?: boolean }) {
  const Icon = good ? CheckIcon : AlertTriangleIcon
  return (
    <div>
      <h3 className="mb-1.5 text-sm font-medium">{title}</h3>
      <ul className="flex flex-col gap-1.5">
        {items.map((t) => (
          <li key={t} className="flex gap-2 text-sm text-muted-foreground">
            <Icon className={cn("mt-0.5 size-3.5 shrink-0", good ? "text-success" : "text-warning")} />
            <span>{t}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
