# Implementation Plan: Plandi V1

Source spec: [spec/SPEC.md](../spec/SPEC.md) + `spec/SPEC-<module>.md` (đã duyệt hết ngày 22/11/2026). Thứ tự module lấy theo [spec/capability-map.md](../spec/capability-map.md).

Status: approved (22/11/2026)

## Overview

Xây MVP Plandi trong 12 tuần, làm một mình, 12–15 giờ/tuần (khoảng 150–180 giờ). Mỗi task là một lát dọc gồm schema, API, test và màn hình web của phần đó. Sau mỗi phase có checkpoint chạy được từ đầu đến cuối. Danh sách task chi tiết nằm ở [todo.md](todo.md).

## Architecture Decisions (đã chốt trong spec, ghi thành ADR ở Task 2)

- Monolith chia module bằng Spring Modulith. Ranh giới module được kiểm tra bằng test, không phải microservices.
- Phiên lưu trong Redis, cookie httpOnly. Tự làm đăng nhập, không dùng Supabase.
- Google: chọn ứng viên rồi mới gọi Google để xếp hạng lại, chỉ lưu `place_id`. TMDB đồng bộ hằng tuần.
- Không dùng LLM trong V1.
- Một repo chứa cả `backend/` và `web/`.
- Dữ liệu người dùng ở trong Việt Nam. Trước khi có người dùng thật thì chạy staging tại nhà qua Cloudflare Tunnel, chỉ với dữ liệu giả. Khi mời người dùng thật thì thuê VPS Việt Nam.

## Lộ trình theo tuần

| Tuần | Phase | Kết quả cuối phase |
|---|---|---|
| 1 | 0 + 1: việc của chủ dự án + nền móng | Repo chạy được, CI xanh, staging tại nhà |
| 2–3 | 2: account + privacy | Đăng ký, đăng nhập, đồng ý, xuất và xóa dữ liệu |
| 4 | 3: fund | Hai điện thoại mời nhau vào ngân sách chung |
| 5–6 | 4: catalog | 50 quán và lịch chiếu CGV trong danh mục |
| 7–8 | 5: budget + preference | Ghi chi trong 3 lần chạm, xem số còn lại |
| 9–10 | 6: recommendation | Vòng lõi chạy từ đầu đến cuối |
| 11–12 | 7: ra mắt | VPS Việt Nam, HTTPS, có người dùng thật |

## Task List (chi tiết trong todo.md)

### Phase 0: Việc của chủ dự án (tuần 1, không code)
- [ ] O1–O7: phỏng vấn (Cổng 1), gọi thử Google, đếm suất CGV, danh sách vùng, tra luật về tuổi, tên miền, lấy API key

### Phase 1: Nền móng
- [ ] Task 1: Scaffold backend
- [ ] Task 2: 5 ADR
- [ ] Task 3: Scaffold web
- [ ] Task 4: CI GitHub Actions
- [ ] Task 5: Staging tại nhà (compose.prod + Cloudflare Tunnel)
### Checkpoint 1

### Phase 2: account + privacy
- [ ] Task 6: Đăng ký, đăng nhập, đăng xuất, /me
- [ ] Task 7: Khóa tạm khi sai, đổi mật khẩu, hồ sơ, khu vực
- [ ] Task 8: Đặt lại mật khẩu bằng mã admin
- [ ] Task 9: Đồng ý theo mục đích + chặn khi đổi chính sách
- [ ] Task 10: Mã hóa cột nhạy cảm
- [ ] Task 11: Xuất dữ liệu và xóa tài khoản (PersonalDataHandler)
- [ ] Task 12: Web: đăng ký, đăng nhập, hồ sơ, đồng ý, quyền riêng tư, trang Điều khoản
### Checkpoint 2

### Phase 3: fund
- [ ] Task 13: Quỹ Cá nhân + FundAccess
- [ ] Task 14: Quỹ Cặp đôi + link mời
- [ ] Task 15: Rời quỹ, lưu trữ, FundDeleted
- [ ] Task 16: Web: chọn chế độ, chia sẻ link, nhận lời mời, rời quỹ
### Checkpoint 3

### Phase 4: catalog
- [ ] Task 17: Danh mục quán + lọc ứng viên (chưa có Google)
- [ ] Task 18: Adapter Google Places
- [ ] Task 19: Admin thêm quán hàng loạt + màn hình admin quán
- [ ] Task 20: Thẻ quán cho người dùng
- [ ] Task 21: TMDB, phim, suất chiếu
- [ ] O8: Chủ dự án nhập 50 quán và đo thời gian
### Checkpoint 4

### Phase 5: budget + preference
- [ ] Task 22: Mục con và ngân sách tháng
- [ ] Task 23: Khoản chi và quy tắc truy cập
- [ ] Task 24: Web: ghi nhanh, ngân sách, tổng kết, lưu trữ
- [ ] Task 25: preference (backend + web)
### Checkpoint 5

### Phase 6: recommendation
- [ ] Task 26: Chấm điểm (hàm thuần)
- [ ] Task 27: Ghép kế hoạch (hàm thuần)
- [ ] Task 28: Pipeline gợi ý + bảo vệ hạn mức Google
- [ ] Task 29: Web: trang chủ và thẻ kế hoạch
### Checkpoint 6

### Phase 7: Ra mắt
- [ ] Task 30: VPS Việt Nam + deploy tự động + backup
- [ ] Task 31: Rà bảo mật và observability
- [ ] O9: Mời 5 cặp đầu tiên
### Checkpoint 7: V1 xong

## Risks and Mitigations

| Rủi ro | Tác động | Cách giảm |
|---|---|---|
| Không xong trong 12 tuần (31 task, khoảng 5 giờ mỗi task) | Cao | Wrapped và mọi mục Deferred giữ nguyên ngoài phạm vi. Trễ hơn 1 tuần ở checkpoint nào thì cắt phần web phụ (tổng kết, lưu trữ) trước, không cắt test |
| Cổng 1 trượt (cặp đôi không thật sự gặp khó) | Cao | Phase 1–2 vẫn dùng được. Chỉ đổi mẫu quỹ ở Phase 3 trở đi |
| Spring Boot 4, Spring Modulith, Testcontainers chưa tương thích nhau lúc scaffold | Trung bình | Task 1 kiểm tra phiên bản đầu tiên. Không hợp thì lùi về Boot 3.x mới nhất và ghi ADR |
| Hạn mức Google quá chặt so với nhu cầu | Trung bình | Gợi ý luôn chạy được bằng dữ liệu riêng. Đo số lượt thật ở Phase 6 |
| Staging tại nhà hay chết (cúp điện, rớt mạng) | Thấp | Chỉ dùng cho demo dữ liệu giả. Chuyển lên VPS ở Phase 7 |
| Không giải thích được code khi phỏng vấn | Rất cao | Chủ dự án tự viết hoặc đọc kỹ Task 26–28 (gợi ý) và FundAccess. ADR cho mọi quyết định lớn |

## Open Questions

1. **Phase 0 có việc chặn task code:**
   - Danh sách vùng chặn Task 6. Có thể dùng danh sách tạm rồi chốt trước Task 30.
   - API key Google chặn Task 18. Có thể dùng fake, nhưng phải có key thật trước Task 19.
2. ~~Máy staging~~: đã chốt ngày 22/11/2026, staging chạy trên chính máy dev của chủ dự án, máy bật thường xuyên.
