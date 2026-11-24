import { PencilIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Job } from "@/lib/api"
import { criterionTypeLabel, WEIGHT_LABEL } from "@/lib/format"

export function RequirementsView({ job, onEdit }: { job: Job; onEdit: () => void }) {
  const required = job.skills.filter((s) => s.weight === "REQUIRED")
  const preferred = job.skills.filter((s) => s.weight !== "REQUIRED")
  const criteria = job.criteria ?? []

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <Card>
        <CardHeader>
          <CardTitle>Mô tả công việc</CardTitle>
          <CardAction>
            <Button variant="outline" size="sm" onClick={onEdit}>
              <PencilIcon data-icon="inline-start" />
              Sửa
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          {job.description ? (
            <p className="text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground">{job.description}</p>
          ) : (
            <p className="text-sm text-muted-foreground">
              Chưa có mô tả. Thêm mô tả giúp AI đánh giá CV sát với công việc hơn.
            </p>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle>Kỹ năng</CardTitle>
            <CardDescription>{job.skills.length} kỹ năng được so khớp với CV.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {required.length > 0 && (
              <div>
                <div className="mb-1.5 text-xs font-medium text-muted-foreground">Bắt buộc</div>
                <div className="flex flex-wrap gap-1.5">
                  {required.map((s) => (
                    <Badge key={s.name} variant="info" className="h-6">
                      {s.name}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
            {preferred.length > 0 && (
              <div>
                <div className="mb-1.5 text-xs font-medium text-muted-foreground">Lợi thế</div>
                <div className="flex flex-wrap gap-1.5">
                  {preferred.map((s) => (
                    <Badge key={s.name} variant="secondary" className="h-6">
                      {s.name}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="pb-0">
          <CardHeader>
            <CardTitle>Tiêu chí bổ sung</CardTitle>
            {!criteria.length && <CardDescription>Vị trí này chưa có tiêu chí bổ sung.</CardDescription>}
          </CardHeader>
          {criteria.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Tiêu chí</TableHead>
                  <TableHead>Yêu cầu</TableHead>
                  <TableHead className="pr-4 text-right">Mức</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {criteria.map((c) => (
                  <TableRow key={c.name + c.requirement}>
                    <TableCell className="pl-4">
                      <div className="font-medium">{c.name}</div>
                      <div className="text-xs text-muted-foreground">{criterionTypeLabel(c.type)}</div>
                    </TableCell>
                    <TableCell className="whitespace-normal">{c.requirement}</TableCell>
                    <TableCell className="pr-4 text-right text-xs text-muted-foreground">
                      {WEIGHT_LABEL[c.weight] ?? c.weight}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Card>
      </div>
    </div>
  )
}
