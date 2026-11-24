# Giao diện quản trị

Giao diện viết bằng React, Vite, Tailwind CSS và shadcn/ui. Bản build được ghi vào
`../src/main/resources/static`, nên server Java phục vụ nó tại `/admin`.

## Chạy khi phát triển

Chạy server Java trước (cổng 8080), rồi:

```
npm install
npm run dev
```

Vite chuyển các request `/api` sang `http://localhost:8080`.

## Build

```
npm run build
```

Sau đó build lại file jar (`mvnw package`) để bản mới nằm trong jar.

## Thêm component shadcn

```
npx shadcn@latest add <tên-component>
```

Lưu ý: CLI có thể ghi `import { cn } from "cn"`. Hãy sửa thành `@/lib/utils`.
