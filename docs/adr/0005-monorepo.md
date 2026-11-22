# ADR-0005: Một repo chung cho backend và web

- Trạng thái: Đã chấp nhận
- Ngày: 21/11/2026 (chốt trong tài liệu nghiên cứu, mục 8)

## Bối cảnh

- Một người làm cả backend và web. Mỗi module đi theo lát dọc: migration, endpoint, rồi màn hình web trong cùng một task.
- Backend phục vụ luôn `web/dist` (ADR-0002), nên một lần deploy gồm cả hai.
- Người xem CV cần mở một link là thấy toàn bộ dự án.

## Quyết định

Một repo `plandi` gồm:

- `backend/`: Spring Boot, Maven wrapper.
- `web/`: React, Vite, npm.
- `spec/`, `tasks/`, `docs/` ở gốc.

Một workflow CI chạy hai job: backend (`./mvnw verify`) và web (lint, test, build).

## Phương án khác

### Hai repo riêng
- Ưu: CI và lịch sử commit tách bạch.
- Nhược: một thay đổi API phải sửa hai repo và hai PR; spec không biết đặt ở đâu; deploy phải ghép hai bản build.
- Bỏ: không có đội riêng cho từng phần, nên tách chỉ thêm việc.

### Công cụ monorepo (Nx, Turborepo, Bazel)
- Bỏ: chỉ có hai project, mỗi project đã có công cụ build riêng. Thêm công cụ chỉ thêm thứ phải học.

## Hệ quả

- Một PR có thể chứa cả endpoint và màn hình dùng nó, review và revert cùng nhau.
- CI chạy cả hai job trên mỗi PR, kể cả khi chỉ sửa một bên. Chấp nhận được với cỡ dự án này; nếu chậm thì lọc theo đường dẫn sau.
- Lệnh chạy nằm trong thư mục con (`cd backend`, `cd web`), ghi trong `CLAUDE.md` và README.
