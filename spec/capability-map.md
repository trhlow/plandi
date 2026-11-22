# Capability Map: Plandi — V1 (MVP 12 tuần)

Status: approved (21/11/2026)

Nguồn: [Tài liệu nghiên cứu & khả thi](https://claude.ai/artifact/GpXGUfRAYgNu3gKYXwHpNn) (rev 37, 21/11/2026).

| Module id | Trách nhiệm | Phụ thuộc |
|---|---|---|
| account | Đăng ký, đăng nhập, phiên; hồ sơ; khu vực người dùng tự chọn (không GPS); vai trò ADMIN | — |
| privacy | Đồng ý theo từng mục đích (lưu chi tiêu, cá nhân hóa, bộ lọc món ăn); điều phối xuất và xóa dữ liệu qua interface `PersonalDataHandler` mà mỗi module tự cài | account |
| fund | Quỹ Cá nhân tự tạo khi đăng ký; quỹ Cặp đôi; link mời có hạn; rời quỹ và chia tay (mỗi người giữ phần mình, quỹ chung đóng); kiểm tra "người gọi có thuộc quỹ này không" cho mọi module khác | account, privacy |
| catalog | Địa điểm (place_id, khoảng giá, nhãn, link TikTok, ngày kiểm tra); phim và suất chiếu CGV Vincom Trà Vinh; trang admin nhập liệu; adapter Google Places (đếm lượt bằng Redis) và adapter TMDB (job đồng bộ hằng tuần) | account |
| budget | Mục con mặc định theo mẫu, ngân sách theo tháng, ghi chi nhanh (có thể gắn địa điểm), số tiền còn lại | fund, catalog, privacy |
| preference | Sở thích tự khai khi mới vào; bộ lọc món ăn (nhãn, không hỏi lý do); màn hình xem, sửa, xóa. Chế độ Cặp đôi gộp sở thích tự khai của cả hai người (chỉ của người đã đồng ý mục đích cá nhân hóa) | account, privacy |
| recommendation | Pipeline: lọc danh mục còn ~10 ứng viên → lấy số sao Google → xếp hạng lại (Bayes + nhãn + sở thích + chỗ mới) → ghép 2–3 hoạt động vừa ngân sách → lý do; vẫn chạy khi tắt adapter | fund, budget, catalog, preference |

Build order: account → privacy → fund, catalog (song song được) → budget, preference → recommendation

Ghi chú:
- Mọi module được dùng `account` (để biết ai đang gọi qua `CurrentAccount`) và `shared`. Bảng trên chỉ ghi phụ thuộc nghiệp vụ. Danh sách đầy đủ nằm trong `@ApplicationModule(allowedDependencies)` ở `package-info.java` của từng module, và `ModularityTest` kiểm tra danh sách đó.
- `web/` (React PWA) không phải module. Mỗi module làm kèm màn hình của nó, đi theo từng lát dọc.
- privacy không phụ thuộc vào module nào khác: các module tự cài `PersonalDataHandler`, privacy chỉ gọi interface đó. Nhờ vậy không có vòng phụ thuộc privacy ↔ module.
- Tách các module bằng package trong một Spring Boot app (`com.plandi.<module>`). Module khác chỉ gọi qua API public của module (package gốc), không truy cập thẳng repository hay entity của nhau.
- Những phần chung (stack, lệnh, cấu trúc, code style, test, boundaries) viết một lần trong `spec/SPEC.md`. Spec của từng module (`spec/SPEC-<id>.md`) chỉ ghi phần riêng của module.
