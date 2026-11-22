# Spec: account — V1

Status: approved (21/11/2026)

Kế thừa [SPEC.md](SPEC.md). Module đầu tiên trong [capability-map.md](capability-map.md), không phụ thuộc module nào.

## Objective

Người dùng tạo tài khoản bằng email và mật khẩu, đăng nhập rồi ở trạng thái đăng nhập lâu dài trên điện thoại (PWA), và tự chọn khu vực sinh sống. Server không nhận GPS (ADR-0006). Các module khác lấy "người đang gọi là ai" từ module này, không tự đọc session.

## Phạm vi

| Có trong V1 | Không có trong V1 |
|---|---|
| Đăng ký, đăng nhập, đăng xuất | Đăng nhập bằng Google, Zalo |
| Hồ sơ: tên hiển thị, khu vực | Xác minh email |
| Đổi mật khẩu (đăng xuất các phiên khác) | 2FA |
| Vai trò USER, ADMIN | Đổi email |
| Đặt lại mật khẩu bằng mã do admin tạo | Quên mật khẩu tự động qua email (V2, cần SMTP) |
| Giới hạn số lần đăng nhập sai | |

Xóa tài khoản và xuất dữ liệu thuộc module privacy. Account đưa ra `AccountService.exportData` và `deleteData` (public), và privacy bọc chúng thành handler "account". Account không tự cài `PersonalDataHandler` vì privacy đã phụ thuộc account, cài trực tiếp sẽ thành vòng phụ thuộc (đổi ngày 22/11/2026).

## API

| Method | Path | Body | Kết quả |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `email, password, displayName, area, adultConfirmed` | 201 + cookie phiên; 409 nếu email đã có |
| POST | `/api/v1/auth/login` | `email, password` | 204 + cookie phiên; 401 dùng chung một thông báo cho sai email và sai mật khẩu; 429 khi bị khóa tạm |
| POST | `/api/v1/auth/logout` | — | 204 |
| POST | `/api/v1/auth/reset-password` | `email, code, newPassword` | 204; hủy mọi phiên của tài khoản; 400 dùng chung một thông báo cho mã sai, hết hạn hoặc đã dùng |
| GET | `/api/v1/me` | — | `id, email, displayName, area, role` |
| PATCH | `/api/v1/me` | `displayName?, area?` | 200 |
| PUT | `/api/v1/me/password` | `currentPassword, newPassword` | 204; hủy mọi phiên khác |
| GET | `/api/v1/areas` | — | danh sách khu vực `code, name, lat, lng` (tọa độ tâm do Plandi đặt, để web tự tính khu gần nhất) |
| POST | `/api/v1/admin/password-resets` | `email` | 201 + `code` (8 ký tự), chỉ ADMIN gọi được |

**API Java cho module khác** (package `com.plandi.account`):
- `CurrentAccount.id()`: lấy id người đang gọi từ security context; ném 401 nếu chưa đăng nhập.
- `AccountDirectory.displayName(long accountId)`: dùng khi quỹ Cặp đôi cần hiện tên người yêu.
- `PasswordVerifier.matches(long accountId, String password)`: privacy dùng khi người dùng xác nhận xóa tài khoản.
- Event `AccountRegistered(long accountId)`: phát **đồng bộ, trong cùng transaction** với lúc đăng ký. Module fund nghe event này để tạo quỹ Cá nhân. Fund tạo lỗi thì đăng ký rollback, nên không có tài khoản nào thiếu quỹ.

## Dữ liệu

```sql
CREATE TABLE account (
    id                  BIGSERIAL PRIMARY KEY,
    email               TEXT NOT NULL,          -- lưu dạng lowercase, đã trim
    password_hash       TEXT NOT NULL,          -- bcrypt qua DelegatingPasswordEncoder
    display_name        TEXT NOT NULL,          -- 1–50 ký tự
    area                TEXT NOT NULL,          -- code của enum Area
    role                TEXT NOT NULL DEFAULT 'USER',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX account_email_uq ON account (email);

CREATE TABLE password_reset (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT NOT NULL REFERENCES account (id),
    code_hash   TEXT NOT NULL,                  -- chỉ lưu hash, không lưu mã gốc
    expires_at  TIMESTAMPTZ NOT NULL,           -- created + 1 giờ
    used_at     TIMESTAMPTZ
);
```

- **Khu vực:** enum `Area` đặt trong `shared` vì catalog cũng dùng để gắn khu vực cho quán. Danh sách là các vùng quen gọi, cố định trong code. Đổi danh sách thì phải deploy lại, chấp nhận được khi chỉ có một thành phố. Mỗi khu vực có tọa độ tâm cố định trong enum (ADR-0006).
- **Danh sách khu vực (chốt 22/11/2026, chủ dự án giao Claude tự tìm):** TP Trà Vinh cũ nay là 4 phường của tỉnh Vĩnh Long (Nghị quyết 1687/NQ-UBTVQH15). Tên phường mới còn lạ, nên tên hiển thị dùng mốc quen thuộc. Tọa độ lấy từ Wikipedia và Google Maps, chỉ cần đúng khoảng vài trăm mét.

  | code | Tên hiển thị | Phường mới (phường cũ) | Tâm (lat, lng) |
  |---|---|---|---|
  | `TRUNG_TAM` | Trung tâm (chợ Trà Vinh, Vincom) | Trà Vinh (P1, P3, P9) | 9.9356, 106.3397 |
  | `DAI_HOC` | Khu ĐH Trà Vinh | Hòa Thuận (P5) | 9.9234, 106.3469 |
  | `HOA_THUAN` | Hòa Thuận (phía Đông) | Hòa Thuận (xã Hòa Thuận cũ) | 9.9528, 106.3661 |
  | `AO_BA_OM` | Ao Bà Om, Bảo tàng Khmer | Nguyệt Hóa (P7, P8, xã Nguyệt Hóa) | 9.9176, 106.3041 |
  | `LONG_DUC` | Long Đức, ven sông Cổ Chiên | Long Đức (P4, xã Long Đức) | 9.9817, 106.3450 |
- **Cấp quyền ADMIN:** chạy một lệnh SQL, ghi trong README. Không làm UI cho việc này.
- **Phiên:** dùng Spring Session lưu trong Redis (`spring-session-data-redis`, đã duyệt), nên deploy lại không làm người dùng bị đăng xuất. Cookie `HttpOnly`, `Secure`, `SameSite=Lax`, tự gia hạn, hết hạn sau 30 ngày không dùng.
- **CSRF:** bật CSRF của Spring Security, token để trong cookie (`CookieCsrfTokenRepository`) để SPA đọc và gửi lại.
- **Quy trình đặt lại mật khẩu:** người dùng nhắn admin qua Zalo. Admin tạo mã trên trang admin rồi gửi lại cho người dùng. Người dùng nhập mã cùng mật khẩu mới. Tạo mã mới thì mã cũ chưa dùng của tài khoản đó hết hiệu lực.

## Quy tắc

- **Email:** dài tối đa 254 ký tự. Lưu sau khi trim và chuyển lowercase.
- **Mật khẩu:** ít nhất 8 ký tự, tối đa 72 byte UTF-8 (khoảng 72 ký tự không dấu), không được trùng email. Giới hạn 72 byte là của bcrypt: Spring Security từ chối mật khẩu dài hơn (đổi từ 128 ký tự ngày 22/11/2026).
- **Xác nhận tuổi:** `adultConfirmed` phải là `true`, tức người dùng tự xác nhận đủ 18 tuổi. App không thu ngày sinh.
- **Khóa tạm khi đăng nhập sai:** sai 5 lần trong 15 phút (đếm theo email + IP trên Redis) thì bị khóa 15 phút, trả 429. Nhập mã đặt lại sai cũng áp dụng giới hạn tương tự.
- **Đăng ký bằng email đã có trả 409:** việc này làm lộ một email đã đăng ký hay chưa. Đã chấp nhận rủi ro vì app quy mô nhỏ.
- **Log:** không ghi email, mật khẩu, tên hay mã đặt lại. Chỉ ghi `accountId` và mã lỗi.

## Test bắt buộc

1. Đăng ký thành công phát `AccountRegistered`. Test dùng Modulith `Scenario`.
2. Đăng ký lại cùng email, khác chữ hoa/thường, nhận 409.
3. Đăng nhập sai mật khẩu và đăng nhập email không tồn tại trả cùng status và cùng body.
4. Lần sai thứ 6 trong 15 phút trả 429. Đúng mật khẩu cũng bị chặn cho đến hết thời gian khóa.
5. Phiên vẫn hợp lệ sau khi khởi động lại app. Test dùng Testcontainers Redis.
6. Sau đăng xuất, cookie cũ gọi `/me` nhận 401.
7. Đổi mật khẩu thì phiên ở thiết bị khác nhận 401, phiên hiện tại vẫn dùng được.
8. Gọi POST thiếu CSRF token nhận 403. Gọi `/me` khi chưa đăng nhập nhận 401.
9. Endpoint `/api/v1/admin/**` trả 403 cho USER, 200 cho ADMIN.
10. Log khi đăng ký, đăng nhập và đặt lại mật khẩu không chứa email hay mã. Kiểm tra bằng `OutputCaptureExtension`.
11. Mã đặt lại hết hạn, đã dùng, hoặc đã bị mã mới thay thế đều nhận 400 với cùng một body.
12. Đặt lại mật khẩu thành công thì mọi phiên cũ nhận 401, và mật khẩu cũ không đăng nhập được nữa.

## Màn hình (web)

1. **Đăng ký:** email, mật khẩu, tên, khu vực, ô xác nhận đủ 18 tuổi, link Điều khoản và Chính sách. Sau khi đăng ký chuyển sang màn hình đồng ý của privacy.
2. **Đăng nhập:** có link "Quên mật khẩu?". Link này hướng dẫn nhắn admin qua Zalo và mở form nhập mã.
3. **Hồ sơ:** sửa tên, sửa khu vực, đổi mật khẩu, đăng xuất.
4. **Admin, đặt lại mật khẩu:** nhập email, nhận mã. Màn hình này không hiện thông tin gì khác của tài khoản.

## Success Criteria

- Cả 12 test bắt buộc đều xanh trong CI.
- Trên điện thoại: đăng ký → đóng trình duyệt → mở lại sau 1 ngày vẫn còn đăng nhập.
- Không có endpoint nào ngoài `/auth/**`, `/areas` và `/actuator/health` trả dữ liệu khi chưa đăng nhập.

## Open Questions

1. ~~**Danh sách vùng**~~: đã chốt 5 khu ở mục Khu vực. Thêm hoặc đổi tên khu sau này chỉ cần sửa enum `Area` và deploy lại.
2. **Ngưỡng 18 tuổi:** kiểm tra quy định về dữ liệu trẻ em trong Luật 91/2025 trước khi mời người dùng thật. Spec hiện giữ ngưỡng 18.
