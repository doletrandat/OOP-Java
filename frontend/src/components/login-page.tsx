import { useState, type FormEvent } from "react"
import { AlertCircleIcon } from "lucide-react"

import { BrandMark } from "@/components/app-sidebar"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Spinner } from "@/components/ui/spinner"
import { api, post } from "@/lib/api"

export function LoginPage({ onLogin }: { onLogin: (token: string) => void }) {
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!password) return setError("Bạn chưa nhập mật khẩu.")
    setBusy(true)
    setError(null)
    try {
      const res = await api<{ token: string }>("/api/auth/login", post({ password }))
      onLogin(res.token)
    } catch (err) {
      setError((err as Error).message)
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-dvh place-items-center px-4 py-10">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <BrandMark className="mb-2 size-9 text-xs" />
          <CardTitle className="text-lg">Đăng nhập quản trị</CardTitle>
          <CardDescription>
            Trang nội bộ để HR tạo vị trí tuyển dụng, tải CV lên và xem bảng xếp hạng ứng viên.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={submit} noValidate>
            <FieldGroup>
              <Field data-invalid={!!error || undefined}>
                <FieldLabel htmlFor="password">Mật khẩu</FieldLabel>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  autoFocus
                  placeholder="Mật khẩu admin…"
                  value={password}
                  aria-invalid={!!error || undefined}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </Field>
              {error && (
                <Alert variant="destructive">
                  <AlertCircleIcon />
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
              <Button type="submit" disabled={busy} className="w-full">
                {busy && <Spinner data-icon="inline-start" />}
                {busy ? "Đang đăng nhập…" : "Đăng nhập"}
              </Button>
            </FieldGroup>
          </form>
        </CardContent>
        <CardFooter className="text-xs text-muted-foreground">
          <p>
            Mật khẩu mặc định là <code className="rounded bg-muted px-1 py-0.5">admin123</code>. Đổi bằng biến môi
            trường <code className="rounded bg-muted px-1 py-0.5">ADMIN_PASSWORD</code>.
          </p>
        </CardFooter>
      </Card>
    </div>
  )
}
