import { useState } from "react"
import {
  ChevronsUpDownIcon,
  LayoutGridIcon,
  LogOutIcon,
  PlusIcon,
  SparklesIcon,
} from "lucide-react"

import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarRail,
  useSidebar,
} from "@/components/ui/sidebar"
import type { Job } from "@/lib/api"
import { paths, type Route } from "@/lib/router"
import { pipeline } from "@/lib/pipeline"
import { useStore } from "@/lib/store"

export function BrandMark({ className = "size-8 text-xs" }: { className?: string }) {
  return (
    <span
      aria-hidden
      className={`grid shrink-0 place-items-center rounded-lg bg-primary font-bold tracking-wide text-primary-foreground ${className}`}
    >
      CV
    </span>
  )
}

export function AppSidebar({
  route,
  onCreateJob,
  onLogout,
}: {
  route: Route
  onCreateJob: () => void
  onLogout: () => void
}) {
  const { jobs, apps, aiStatus } = useStore()
  const { setOpenMobile } = useSidebar()
  const [closedOpen, setClosedOpen] = useState(false)
  const activeJob = route.name === "home" ? null : route.jobId
  const open = jobs?.filter((j) => j.status === "OPEN") ?? []
  const closed = jobs?.filter((j) => j.status !== "OPEN") ?? []

  const JobItem = ({ job }: { job: Job }) => {
    const pending = pipeline(apps[job.id]).NEW
    return (
      <SidebarMenuItem>
        <SidebarMenuButton
          asChild
          isActive={activeJob === job.id}
          tooltip={job.title}
          onClick={() => setOpenMobile(false)}
        >
          <a href={paths.job(job.id)}>
            <span className="truncate">{job.title}</span>
          </a>
        </SidebarMenuButton>
        {pending > 0 && (
          <SidebarMenuBadge title={`${pending} CV chưa xử lý`} className="tabular-nums">
            {pending}
          </SidebarMenuBadge>
        )}
      </SidebarMenuItem>
    )
  }

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <a href={paths.home()}>
                <BrandMark />
                <div className="grid flex-1 text-left leading-tight">
                  <span className="truncate font-semibold">CV Screener</span>
                  <span className="truncate text-xs text-muted-foreground">Sàng lọc hồ sơ</span>
                </div>
              </a>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton
                asChild
                isActive={route.name === "home"}
                tooltip="Tổng quan"
                onClick={() => setOpenMobile(false)}
              >
                <a href={paths.home()}>
                  <LayoutGridIcon />
                  <span>Tổng quan</span>
                </a>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>

        <SidebarGroup className="group-data-[collapsible=icon]:hidden">
          <SidebarGroupLabel>Đang tuyển</SidebarGroupLabel>
          <SidebarGroupAction title="Tạo vị trí" onClick={onCreateJob}>
            <PlusIcon />
            <span className="sr-only">Tạo vị trí</span>
          </SidebarGroupAction>
          <SidebarGroupContent>
            <SidebarMenu>
              {!jobs && Array.from({ length: 3 }).map((_, i) => <SidebarMenuSkeleton key={i} />)}
              {open.map((j) => (
                <JobItem key={j.id} job={j} />
              ))}
              {jobs && open.length === 0 && (
                <SidebarMenuItem>
                  <SidebarMenuButton onClick={onCreateJob} className="text-muted-foreground">
                    <PlusIcon />
                    <span>Tạo vị trí đầu tiên</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        {closed.length > 0 && (
          <Collapsible open={closedOpen} onOpenChange={setClosedOpen} className="group/closed">
            <SidebarGroup className="group-data-[collapsible=icon]:hidden">
              <SidebarGroupLabel asChild>
                <CollapsibleTrigger className="w-full">
                  Đã đóng ({closed.length})
                  <ChevronsUpDownIcon className="ml-auto size-3.5" />
                </CollapsibleTrigger>
              </SidebarGroupLabel>
              <CollapsibleContent>
                <SidebarGroupContent>
                  <SidebarMenu>
                    {closed.map((j) => (
                      <JobItem key={j.id} job={j} />
                    ))}
                  </SidebarMenu>
                </SidebarGroupContent>
              </CollapsibleContent>
            </SidebarGroup>
          </Collapsible>
        )}
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <AccountMenu aiStatus={aiStatus} onLogout={onLogout} />
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}

function AccountMenu({ aiStatus, onLogout }: { aiStatus: ReturnType<typeof useStore>["aiStatus"]; onLogout: () => void }) {
  const on = !!aiStatus?.configured
  const aiLine = on ? `${aiStatus!.displayName || aiStatus!.model}` : "AI đang tắt"
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <SidebarMenuButton size="lg" className="data-[state=open]:bg-sidebar-accent">
          <span className="grid size-8 shrink-0 place-items-center rounded-lg border bg-background">
            <SparklesIcon className={on ? "size-4 text-primary" : "size-4 text-muted-foreground"} />
          </span>
          <div className="grid flex-1 text-left leading-tight">
            <span className="truncate text-sm font-medium">Quản trị viên</span>
            <span className="flex items-center gap-1.5 truncate text-xs text-muted-foreground">
              <span aria-hidden className={`size-1.5 shrink-0 rounded-full ${on ? "bg-success" : "bg-warning"}`} />
              {aiLine}
            </span>
          </div>
          <ChevronsUpDownIcon className="ml-auto size-4" />
        </SidebarMenuButton>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start" className="w-(--radix-dropdown-menu-trigger-width) min-w-56">
        <DropdownMenuLabel className="font-normal">
          <div className="text-sm font-medium">Chấm điểm bằng AI</div>
          <div className="text-xs text-muted-foreground">
            {on
              ? `${aiLine}, chiếm ${Math.round(aiStatus!.aiWeight * 100)}% điểm tổng.`
              : "Hồ sơ chỉ được chấm theo từ khoá và tiêu chí. Đặt AI_API_KEY trong config.properties để bật."}
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={onLogout}>
          <LogOutIcon /> Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
