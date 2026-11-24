import path from "node:path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// Bản build được ghi thẳng vào thư mục static mà server Java phục vụ.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
    chunkSizeWarningLimit: 700,
  },
  server: {
    proxy: { "/api": "http://localhost:8080" },
  },
})
