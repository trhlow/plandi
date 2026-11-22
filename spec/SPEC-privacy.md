# Spec: privacy — V1

Status: approved (21/11/2026)

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: account. Các module khác dùng privacy qua API public. Privacy không import module nào khác ngoài account.

## Objective

Người dùng kiểm soát được dữ liệu của mình theo NĐ 356/2025:
- Đồng ý hoặc rút lại theo từng mục đích.
- Tải về toàn bộ dữ liệu.
- Xóa tài khoản, và sau khi xóa thì DB không còn dòng nào gắn với họ.

Privacy cũng cung cấp bộ mã hóa cột nhạy cảm để các module khác dùng chung.

## Mục đích xử lý

| Purpose | Bắt buộc? | Mặc định | Khi rút lại |
|---|---|---|---|
| `TERMS`: điều khoản và chính sách | Có, để dùng app | Ghi nhận lúc đăng ký | Không rút riêng được; muốn rút thì xóa tài khoản |
| `SPENDING_LOG`: lưu chi tiêu | Không | Chưa đồng ý | Không ghi chi mới được. Có nút xóa toàn bộ chi tiêu đã ghi (budget xử lý) |
| `PERSONALIZATION`: cá nhân hóa gợi ý | Không | Chưa đồng ý | Gợi ý không dùng sở thích và lịch sử, chỉ dùng ngân sách và danh mục |
| `FOOD_FILTERS`: bộ lọc món ăn | Không | Chưa đồng ý | Bộ lọc đã lưu bị xóa ngay (preference xử lý) |

Từng module tự xử lý việc rút đồng ý bằng cách nghe event `ConsentWithdrawn`. Cách xử lý cụ thể ghi trong spec của module đó.

## API

| Method | Path | Body | Kết quả |
|---|---|---|---|
| GET | `/api/v1/consents` | — | trạng thái hiện tại của từng purpose + `policyVersion` |
| PUT | `/api/v1/consents/{purpose}` | `granted` | 204; `TERMS` không nhận `false` |
| GET | `/api/v1/me/export` | — | file JSON tải về; tối đa 5 lần/ngày |
| DELETE | `/api/v1/me` | `password` | 204; xóa tài khoản và đăng xuất |

**API Java cho module khác** (package `com.plandi.privacy`):
- `ConsentService.isGranted(long accountId, Purpose purpose)`.
- Event `ConsentWithdrawn(long accountId, Purpose purpose)`.
- Interface `PersonalDataHandler`. Mỗi module có dữ liệu cá nhân tự cài (trừ account: privacy tự bọc `AccountService.exportData`/`deleteData` thành handler "account", vì account không được phụ thuộc privacy):
  ```java
  public interface PersonalDataHandler {
      String section();                       // "account", "fund", "budget"…
      Object export(long accountId);          // record, được serialize vào file JSON
      void delete(long accountId);            // chạy trong transaction của privacy
      int deleteOrder();                      // số nhỏ chạy trước; account chạy cuối
  }
  ```
- `EncryptedStringConverter`: JPA `AttributeConverter` cho cột nhạy cảm. Spec của từng module ghi rõ cột nào dùng converter này.

## Dữ liệu

```sql
CREATE TABLE consent_event (
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    purpose        TEXT NOT NULL,
    granted        BOOLEAN NOT NULL,
    policy_version TEXT NOT NULL,      -- ví dụ "2026-11"
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX consent_event_account_idx ON consent_event (account_id, purpose, created_at DESC);
```

- **Lịch sử đồng ý:** bảng chỉ thêm dòng, không sửa. Lịch sử này là bằng chứng người dùng đã đồng ý. Trạng thái hiện tại là dòng mới nhất của mỗi purpose. Khi xóa tài khoản thì lịch sử bị xóa theo.
- **Đổi phiên bản chính sách:** `policyVersion` là một hằng số trong code. Đổi chính sách thì tăng version. Lần đăng nhập sau, người dùng phải đồng ý lại `TERMS` mới dùng tiếp được.

## Mã hóa cột nhạy cảm

- **Thuật toán:** AES-256-GCM dùng `javax.crypto`, không thêm dependency. Mỗi giá trị có IV 12 byte ngẫu nhiên. Định dạng lưu là `v1:` + base64(iv‖ciphertext). Tiền tố `v1` để sau này đổi khóa được.
- **Khóa:** lấy từ biến môi trường `PLANDI_DATA_KEY` (32 byte, base64). Profile `prod` thiếu khóa thì app không khởi động.
- **Giới hạn đã biết:** cột mã hóa không tìm kiếm và không SUM được trong SQL. Vì vậy số tiền (`amount_vnd`) không mã hóa, vì budget cần SUM. Bù lại: Postgres không mở port ra ngoài, và file backup được mã hóa.

## Xóa tài khoản

1. Kiểm tra mật khẩu bằng `PasswordVerifier`. Sai thì trả 403 và không xóa gì.
2. Trong **một transaction**, gọi `delete()` của mọi `PersonalDataHandler` theo `deleteOrder` tăng dần. Account chạy cuối. Một handler lỗi thì rollback toàn bộ.
3. Hủy mọi phiên của tài khoản trong Redis.
4. **Log:** theo quy ước của `SPEC.md`, log không chứa dữ liệu cá nhân, chỉ có `accountId`. Log giữ 30 ngày rồi tự xóa.
5. **Backup:** Postgres backup giữ 7 ngày. Dữ liệu đã xóa còn trong backup tối đa 7 ngày. Chính sách quyền riêng tư phải nói rõ điều này.

Quỹ Cặp đôi khi một người xóa tài khoản sẽ do handler của fund xử lý, xem `SPEC-fund` (luồng chia tay).

## Test bắt buộc

1. Tài khoản mới có `TERMS` được ghi kèm `policyVersion`. Ba purpose còn lại ở trạng thái chưa đồng ý.
2. Bật rồi tắt một purpose tạo 2 dòng lịch sử. GET trả trạng thái của dòng mới nhất.
3. Rút đồng ý thì phát `ConsentWithdrawn`. PUT `TERMS` với `false` nhận 400.
4. File export có đủ section của mọi handler đã đăng ký. Export của A không chứa dữ liệu nào của B (hai tài khoản trong cùng test).
5. DELETE sai mật khẩu nhận 403 và dữ liệu còn nguyên.
6. Sau khi xóa: quét mọi bảng có cột tên kết thúc bằng `account_id` (qua `information_schema`), không còn dòng nào chứa id đó.
7. Sau khi xóa: cookie cũ nhận 401, đăng nhập lại bằng email đó nhận 401.
8. Một handler ném lỗi thì không dòng nào bị xóa (rollback).
9. Converter: giải mã ra đúng giá trị gốc; cùng một chuỗi mã hóa 2 lần ra 2 kết quả khác nhau; sửa 1 byte ciphertext thì giải mã ném lỗi; giá trị thô trong DB không chứa chuỗi gốc.
10. Profile `prod` thiếu `PLANDI_DATA_KEY` thì context không khởi động.
11. Tăng `policyVersion` thì người dùng nhận 403 với mã `TERMS_REQUIRED` cho đến khi đồng ý lại.

## Màn hình (web)

1. **Đồng ý:** hiện ngay sau khi đăng ký. Ba công tắc, mỗi công tắc có một câu giải thích app dùng dữ liệu đó để làm gì. Tất cả mặc định tắt.
2. **Quyền riêng tư (trong Hồ sơ):** gồm các công tắc như trên, nút tải dữ liệu, và nút xóa tài khoản (phải nhập lại mật khẩu).
3. **Trang Điều khoản và Chính sách:** trang tĩnh tiếng Việt. Có mục ghi nguồn TMDB và Google Maps, và nói rõ vị trí chỉ dùng trên điện thoại để chọn khu vực, không gửi đi (ADR-0006).

## Success Criteria

- Cả 11 test bắt buộc đều xanh. Test số 6 tự áp dụng cho mọi bảng mà các module sau thêm vào.
- Xóa một tài khoản thật trên staging, rồi chạy lệnh SQL tìm id đó trên toàn DB, không ra dòng nào.

## Decisions (21/11/2026)

- **Export của quỹ Cặp đôi:** chỉ gồm khoản do chính người dùng ghi, cộng với tổng chi của quỹ theo tháng. Khoản người yêu ghi là dữ liệu của người yêu.
- **Điều khoản và Chính sách:** Claude soạn bản nháp tiếng Việt khớp với spec, làm thành một task trong plan. Bản nháp không phải tư vấn pháp lý.
- **Backup:** giữ 7 ngày.
