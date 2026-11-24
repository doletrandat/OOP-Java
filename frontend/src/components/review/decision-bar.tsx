import { CheckIcon, XIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import type { HrDecision } from "@/lib/api"
import { cn } from "@/lib/utils"

export type NoteState = "idle" | "saving" | "saved"

/** Ghi chú nội bộ và hai nút quyết định ở cuối khung đánh giá. */
export function DecisionBar({
  decision,
  note,
  noteState,
  onNoteChange,
  onNoteSave,
  onChoose,
}: {
  decision: HrDecision
  note: string
  noteState: NoteState
  onNoteChange: (note: string) => void
  onNoteSave: () => void
  onChoose: (decision: HrDecision) => void
}) {
  return (
    <div className="flex flex-col gap-3 border-t bg-muted/30 p-4">
      <div className="flex items-center gap-2">
        <Input
          id="review-note"
          placeholder="Ghi chú nội bộ…"
          maxLength={500}
          autoComplete="off"
          value={note}
          onChange={(e) => onNoteChange(e.target.value)}
          onBlur={onNoteSave}
        />
        <span className="w-14 shrink-0 text-xs text-muted-foreground" aria-live="polite">
          {noteState === "saving" ? "Đang lưu…" : noteState === "saved" ? "Đã lưu" : ""}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Button
          variant="outline"
          size="lg"
          aria-pressed={decision === "REJECTED"}
          onClick={() => onChoose("REJECTED")}
          className={cn(
            decision === "REJECTED" &&
              "border-destructive/40 bg-destructive/10 text-destructive hover:bg-destructive/15"
          )}
        >
          <XIcon data-icon="inline-start" />
          {decision === "REJECTED" ? "Đã từ chối" : "Từ chối"}
        </Button>
        <Button
          size="lg"
          aria-pressed={decision === "SHORTLISTED"}
          onClick={() => onChoose("SHORTLISTED")}
          className={cn(decision === "SHORTLISTED" && "bg-success text-white hover:bg-success/90")}
        >
          <CheckIcon data-icon="inline-start" />
          {decision === "SHORTLISTED" ? "Đã mời phỏng vấn" : "Mời phỏng vấn"}
        </Button>
      </div>
    </div>
  )
}
