import { useEffect, useState } from "react"

/** Định tuyến bằng hash để server Java chỉ cần phục vụ một trang /admin. */
export type Route =
  | { name: "home" }
  | { name: "job"; jobId: string; tab?: "requirements" }
  | { name: "review"; jobId: string; appId: string }

export function parseRoute(hash = location.hash): Route {
  const parts = hash.replace(/^#\/?/, "").split("/").filter(Boolean).map(decodeURIComponent)
  if (parts[0] === "jobs" && parts[1]) {
    if (parts[2] === "c" && parts[3]) return { name: "review", jobId: parts[1], appId: parts[3] }
    if (parts[2] === "requirements") return { name: "job", jobId: parts[1], tab: "requirements" }
    return { name: "job", jobId: parts[1] }
  }
  return { name: "home" }
}

export const paths = {
  home: () => "#/",
  job: (jobId: string) => `#/jobs/${encodeURIComponent(jobId)}`,
  requirements: (jobId: string) => `#/jobs/${encodeURIComponent(jobId)}/requirements`,
  review: (jobId: string, appId: string) => `#/jobs/${encodeURIComponent(jobId)}/c/${encodeURIComponent(appId)}`,
}

export function navigate(to: string, { replace = false } = {}) {
  if (replace) {
    history.replaceState(null, "", to)
    window.dispatchEvent(new HashChangeEvent("hashchange"))
  } else {
    location.hash = to
  }
}

export function useRoute(): Route {
  const [route, setRoute] = useState(parseRoute)
  useEffect(() => {
    const on = () => setRoute(parseRoute())
    window.addEventListener("hashchange", on)
    return () => window.removeEventListener("hashchange", on)
  }, [])
  return route
}
