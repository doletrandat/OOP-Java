import { useCallback, useEffect, useState } from "react"

import { AppSidebar } from "@/components/app-sidebar"
import { JobEditorSheet } from "@/components/job-editor-sheet"
import { JobWorkspace } from "@/components/job-workspace"
import { LoginPage } from "@/components/login-page"
import { OverviewPage } from "@/components/overview-page"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Separator } from "@/components/ui/separator"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { api, auth, post, setUnauthorizedHandler, type Job } from "@/lib/api"
import { navigate, paths, useRoute } from "@/lib/router"
import { StoreProvider, useStore } from "@/lib/store"

export default function App() {
  const [token, setToken] = useState(auth.token)

  const logout = useCallback((callServer = true) => {
    if (callServer && auth.token) api("/api/auth/logout", post()).catch(() => {})
    auth.set(null)
    setToken(null)
  }, [])

  useEffect(() => setUnauthorizedHandler(() => logout(false)), [logout])

  if (!token) {
    return (
      <LoginPage
        onLogin={(t) => {
          auth.set(t)
          setToken(t)
        }}
      />
    )
  }

  return (
    <StoreProvider>
      <Shell onLogout={() => logout()} />
    </StoreProvider>
  )
}

function Shell({ onLogout }: { onLogout: () => void }) {
  const route = useRoute()
  const { jobs, reloadJob, reloadJobs } = useStore()
  // undefined = đóng; null = tạo mới; Job = đang sửa
  const [editing, setEditing] = useState<Job | null | undefined>(undefined)

  const job = route.name === "home" ? null : jobs?.find((j) => j.id === route.jobId)

  // Vị trí đã bị xoá hoặc đường dẫn sai thì quay về trang tổng quan
  useEffect(() => {
    if (route.name !== "home" && jobs && !job) navigate(paths.home(), { replace: true })
  }, [route, jobs, job])

  return (
    <SidebarProvider>
      <AppSidebar route={route} onCreateJob={() => setEditing(null)} onLogout={onLogout} />
      <SidebarInset className="min-w-0">
        <header className="sticky top-0 z-30 flex h-12 shrink-0 items-center gap-2 border-b bg-background/95 px-3 backdrop-blur-sm">
          <SidebarTrigger aria-label="Ẩn/hiện thanh bên" />
          <Separator orientation="vertical" className="mr-1 h-4!" />
          <Breadcrumb className="min-w-0">
            <BreadcrumbList className="flex-nowrap">
              {route.name === "home" ? (
                <BreadcrumbItem>
                  <BreadcrumbPage>Tổng quan</BreadcrumbPage>
                </BreadcrumbItem>
              ) : (
                <>
                  <BreadcrumbItem className="hidden sm:inline-flex">
                    <BreadcrumbLink href={paths.home()}>Tổng quan</BreadcrumbLink>
                  </BreadcrumbItem>
                  <BreadcrumbSeparator className="hidden sm:block" />
                  <BreadcrumbItem className="min-w-0">
                    {route.name === "review" ? (
                      <BreadcrumbLink href={paths.job(route.jobId)} className="truncate">
                        {job?.title ?? "…"}
                      </BreadcrumbLink>
                    ) : (
                      <BreadcrumbPage className="truncate">{job?.title ?? "…"}</BreadcrumbPage>
                    )}
                  </BreadcrumbItem>
                  {route.name === "review" && (
                    <>
                      <BreadcrumbSeparator className="hidden sm:block" />
                      <BreadcrumbItem className="hidden whitespace-nowrap sm:inline-flex">
                        <BreadcrumbPage>Xem hồ sơ</BreadcrumbPage>
                      </BreadcrumbItem>
                    </>
                  )}
                </>
              )}
            </BreadcrumbList>
          </Breadcrumb>
        </header>

        {route.name === "home" ? (
          <OverviewPage onCreateJob={() => setEditing(null)} />
        ) : job ? (
          <JobWorkspace key={job.id} job={job} route={route} onEditJob={() => setEditing(job)} />
        ) : null}
      </SidebarInset>

      <JobEditorSheet
        open={editing !== undefined}
        job={editing ?? null}
        onOpenChange={(o) => !o && setEditing(undefined)}
        onSaved={async (saved, isNew) => {
          setEditing(undefined)
          if (isNew) {
            await reloadJobs()
            navigate(paths.job(saved.id))
          } else {
            await reloadJob(saved.id)
          }
        }}
      />
    </SidebarProvider>
  )
}
