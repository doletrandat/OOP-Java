import { useEffect, useState, type FormEvent } from "react"
import { toast } from "sonner"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { api, put, type Application } from "@/lib/api"

export interface ContactTarget {
  id: string
  candidateName: string
  email?: string
  phone?: string
}

/** Sửa họ tên và liên hệ, dùng khi hệ thống đoán tên từ CV chưa đúng. */
export function EditContactDialog({
  target,
  onClose,
  onSaved,
}: {
  target: ContactTarget | null
  onClose: () => void
  onSaved: (app: Application) => void
}) {
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [phone, setPhone] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!target) return
    setName(target.candidateName)
    setEmail(target.email ?? "")
    setPhone(target.phone ?? "")
    setError(null)
    setBusy(false)
  }, [target])

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!target) return
    if (!name.trim()) return setError("Họ tên không được để trống.")
    setBusy(true)
    try {
      const app = await api<Application>(`/api/admin/applications/${target.id}`, put({ name, email, phone }))
      toast.success("Đã lưu thông tin ứng viên.")
      onSaved(app)
    } catch (err) {
      setError((err as Error).message)
      setBusy(false)
    }
  }

  return (
    <Dialog open={!!target} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={submit} noValidate className="contents">
          <DialogHeader>
            <DialogTitle>Thông tin ứng viên</DialogTitle>
            <DialogDescription>Hệ thống tự đọc từ CV. Sửa lại nếu chưa đúng.</DialogDescription>
          </DialogHeader>
          <FieldGroup>
            <Field data-invalid={!!error || undefined}>
              <FieldLabel htmlFor="ct-name">Họ tên</FieldLabel>
              <Input
                id="ct-name"
                autoComplete="off"
                autoFocus
                value={name}
                aria-invalid={!!error || undefined}
                onChange={(e) => setName(e.target.value)}
              />
              {error && <p className="text-sm text-destructive">{error}</p>}
            </Field>
            <Field>
              <FieldLabel htmlFor="ct-email">Email</FieldLabel>
              <Input
                id="ct-email"
                type="email"
                autoComplete="off"
                spellCheck={false}
                placeholder="ungvien@email.com…"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="ct-phone">Số điện thoại</FieldLabel>
              <Input
                id="ct-phone"
                type="tel"
                inputMode="tel"
                autoComplete="off"
                placeholder="09xx xxx xxx…"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </Field>
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Huỷ
              </Button>
            </DialogClose>
            <Button type="submit" disabled={busy}>
              {busy && <Spinner data-icon="inline-start" />}
              Lưu
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Xác nhận xoá một hoặc nhiều ứng viên. */
export function DeleteCandidatesDialog({
  names,
  onCancel,
  onConfirm,
}: {
  names: string[] | null
  onCancel: () => void
  onConfirm: () => void
}) {
  const n = names?.length ?? 0
  return (
    <AlertDialog open={n > 0} onOpenChange={(o) => !o && onCancel()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{n === 1 ? `Xoá ${names![0]}?` : `Xoá ${n} ứng viên?`}</AlertDialogTitle>
          <AlertDialogDescription>
            File CV và kết quả chấm điểm sẽ bị xoá. Bạn không thể hoàn tác.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Huỷ</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={onConfirm}>
            Xoá
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
