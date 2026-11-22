# Spec: fund — V1

Status: approved (21/11/2026)

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: account, privacy. Trên giao diện gọi là "Ngân sách cá nhân" / "Ngân sách chung", không dùng chữ "quỹ".

## Objective

Một mô hình quỹ dùng cho cả hai chế độ. Quỹ là cuốn sổ ghi ngân sách và chi tiêu, **không chứa tiền thật**.
- Mỗi tài khoản luôn có đúng một quỹ Cá nhân.
- Mỗi tài khoản tham gia tối đa một quỹ Cặp đôi đang hoạt động.
- Người yêu vào quỹ bằng link mời có hạn.
- Một trong hai người rời quỹ thì quỹ chung đóng, và người kia không còn thấy dữ liệu của mình.

Module này còn là **cổng kiểm tra quyền** cho mọi dữ liệu theo quỹ: `FundAccess`.

## Vòng đời

```
Đăng ký ──AccountRegistered──▶ quỹ PERSONAL (1 thành viên, không rời được)

Chọn "Cặp đôi" ──▶ quỹ COUPLE ACTIVE (1 thành viên) + link mời 7 ngày
        │  người yêu bấm link → đăng nhập/đăng ký → chấp nhận
        ▼
   COUPLE ACTIVE (2 thành viên)
        │  một trong hai bấm "Rời ngân sách chung" (hoặc xóa tài khoản)
        ▼
   COUPLE CLOSED: không ghi mới; mỗi người chỉ đọc được khoản mình đã ghi
```

- Quỹ Cặp đôi dùng được ngay khi chỉ có một thành viên, ví dụ để đặt ngân sách trước trong lúc chờ người yêu vào.
- Quay lại với nhau thì tạo quỹ mới bằng link mới. Quỹ đã đóng không mở lại được.

## API

| Method | Path | Body | Kết quả |
|---|---|---|---|
| GET | `/api/v1/funds` | — | quỹ đang hoạt động của người gọi: `id, kind, memberCount, partnerDisplayName?` |
| GET | `/api/v1/funds/{id}` | — | chi tiết quỹ; người ngoài quỹ nhận 404 |
| POST | `/api/v1/funds/couple` | — | 201 `fundId, inviteUrl, expiresAt`; 409 `ALREADY_IN_COUPLE` |
| POST | `/api/v1/funds/{id}/invites` | — | 201 link mới, link cũ bị thu hồi; 409 `FUND_FULL` |
| DELETE | `/api/v1/funds/{id}/invites` | — | 204, thu hồi link đang có |
| GET | `/api/v1/invites/{token}` | — | cần đăng nhập; `inviterDisplayName, expiresAt` |
| POST | `/api/v1/invites/{token}/accept` | — | 200 `fundId`; xem bảng lỗi |
| POST | `/api/v1/funds/{id}/leave` | — | 204; quỹ PERSONAL trả 400 |

Bảng lỗi khi chấp nhận lời mời:

| Tình huống | Status | Code |
|---|---|---|
| Token không tồn tại | 404 | — |
| Link hết hạn, đã dùng, hoặc đã bị thu hồi | 410 | `INVITE_EXPIRED` / `INVITE_USED` / `INVITE_REVOKED` |
| Tự chấp nhận lời mời của chính mình | 409 | `SELF_INVITE` |
| Người nhận đang ở một quỹ Cặp đôi khác | 409 | `ALREADY_IN_COUPLE` |
| Quỹ đã đủ 2 người hoặc đã đóng | 409 | `FUND_FULL` / `FUND_CLOSED` |

Người cầm link chính là người được mời, nên phân biệt từng lý do (410/409) không gây lộ thông tin. Nhờ vậy người dùng biết phải nhờ người yêu gửi lại link.

**API Java cho module khác** (package `com.plandi.fund`):
- `FundAccess.requireMember(long accountId, long fundId)`: người gọi phải là thành viên hiện tại của một quỹ ACTIVE, nếu không ném `FundNotFoundException` (404). Mọi thao tác đọc hoặc ghi dữ liệu theo quỹ đều gọi hàm này.
- `FundAccess.requireArchiveAccess(long accountId, long fundId)`: người gọi là thành viên cũ của một quỹ CLOSED. Budget dùng hàm này để cho đọc **chỉ những khoản do chính người đó ghi**.
- `FundQuery.personalFundOf(long accountId)`, `FundQuery.activeCoupleFundOf(long accountId): Optional<Long>`, `FundQuery.activeMemberIds(long fundId)`.
- Event `FundClosed(long fundId, List<Long> formerMemberIds)`: phát đồng bộ trong transaction của thao tác rời quỹ.
- Event `FundDeleted(long fundId)`: phát đồng bộ ngay trước khi xóa dòng `fund`. Budget nghe event này để xóa danh mục, ngân sách và khoản chi của quỹ đó. Nhờ vậy không cần khóa ngoại giữa bảng của hai module. (Bổ sung ngày 21/11/2026 khi viết SPEC-budget.)
- Nghe event `AccountRegistered` để tạo quỹ PERSONAL, chạy trong cùng transaction với đăng ký.

## Dữ liệu

```sql
CREATE TABLE fund (
    id         BIGSERIAL PRIMARY KEY,
    kind       TEXT NOT NULL,              -- PERSONAL | COUPLE
    status     TEXT NOT NULL,              -- ACTIVE | CLOSED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at  TIMESTAMPTZ
);

CREATE TABLE fund_member (
    fund_id    BIGINT NOT NULL REFERENCES fund (id),
    account_id BIGINT NOT NULL,
    fund_kind  TEXT NOT NULL,              -- chép từ fund.kind để dùng partial unique index
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at    TIMESTAMPTZ,
    PRIMARY KEY (fund_id, account_id)
);
-- tối đa một quỹ PERSONAL và một quỹ COUPLE đang hoạt động cho mỗi tài khoản, chặn ngay ở tầng DB
CREATE UNIQUE INDEX fund_member_one_active_per_kind
    ON fund_member (account_id, fund_kind) WHERE left_at IS NULL;

CREATE TABLE fund_invite (
    id                    BIGSERIAL PRIMARY KEY,
    fund_id               BIGINT NOT NULL REFERENCES fund (id),
    token_hash            TEXT NOT NULL UNIQUE,   -- SHA-256 của token; DB không lưu token gốc
    created_by_account_id BIGINT NOT NULL,
    expires_at            TIMESTAMPTZ NOT NULL,   -- created + 7 ngày
    used_at               TIMESTAMPTZ,
    used_by_account_id    BIGINT,
    revoked_at            TIMESTAMPTZ
);
```

- **Token mời:** 32 byte ngẫu nhiên, mã hóa base64url, chỉ nằm trong URL. Link có dạng `https://<domain>/moi/<token>`.
- **Số link mời:** mỗi quỹ chỉ có một link còn hiệu lực. Tạo link mới thì link cũ bị thu hồi. Mỗi tài khoản tạo tối đa 10 link/ngày.
- **Thời gian:** dùng một bean `java.time.Clock` để test được trường hợp hết hạn.

## Rời quỹ (luồng chia tay)

Một transaction, thực hiện lần lượt:
1. Đặt `left_at` cho **mọi** thành viên.
2. Chuyển quỹ sang CLOSED.
3. Thu hồi các link mời còn hiệu lực.
4. Phát event `FundClosed`.

| Sau khi đóng | Hành vi |
|---|---|
| Ghi chi, đặt ngân sách, gợi ý | Không được: `requireMember` trả 404 |
| Xem lịch sử | Mỗi người chỉ thấy khoản **mình** ghi, dạng chỉ đọc (`requireArchiveAccess`); tự xóa được phần của mình |
| Người kia | Lần mở app sau thấy "Ngân sách chung đã đóng". Không hiện lý do, không gửi thông báo |
| Quỹ Cá nhân | Không bị ảnh hưởng; khoản chi chung không được chuyển sang |

## Quyền riêng tư (`PersonalDataHandler`, `deleteOrder = 80`)

- **Export:** danh sách quỹ đã tham gia (`kind`, ngày vào, ngày rời) và các link mời do người này tạo. Không xuất tên hay id của người yêu.
- **Delete:**
  1. Nếu đang ở quỹ Cặp đôi ACTIVE thì chạy luồng rời quỹ.
  2. Xóa dòng `fund_member` và `fund_invite` của người này.
  3. Xóa quỹ PERSONAL của họ.
  4. Quỹ COUPLE không còn dòng `fund_member` nào thì xóa luôn.

  Budget (`deleteOrder` nhỏ hơn) đã xóa khoản chi của người này trước đó.

## Hợp đồng với budget (ghi lại để `SPEC-budget` làm đúng)

- Mọi thao tác của budget đi qua `FundAccess`.
- Với quỹ CLOSED, budget chỉ trả về khoản có `created_by_account_id` là người gọi.
- Không ghi "ai trả" và không tính ai nợ ai. App chỉ ghi khoản chi của chung, không xử lý chuyện tiền giữa hai người.

## Test bắt buộc

1. Đăng ký xong có đúng một quỹ PERSONAL, và người đăng ký là thành viên ACTIVE.
2. Tạo quỹ Cặp đôi thì sinh quỹ kèm link mời. Đang có quỹ Cặp đôi ACTIVE mà tạo tiếp thì nhận 409 `ALREADY_IN_COUPLE`.
3. Chấp nhận link hợp lệ thì quỹ có 2 thành viên, và `requireMember` thành công cho cả hai.
4. Link hết hạn (Clock tiến 7 ngày + 1 giây), link đã dùng, link bị thu hồi thì nhận 410 đúng code. Token lạ nhận 404.
5. Các trường hợp `SELF_INVITE`, `ALREADY_IN_COUPLE` và `FUND_FULL` (tạo link khi đã đủ 2 người) đều nhận 409.
6. **Truy cập chéo:** với mọi endpoint `/funds/{id}/**`: thành viên quỹ Cặp đôi A gọi vào quỹ B nhận 404; người yêu gọi vào quỹ PERSONAL của người kia nhận 404.
7. Rời quỹ: quỹ thành CLOSED, link bị thu hồi, `FundClosed` được phát. Sau đó `requireMember` trả 404 cho cả hai, còn `requireArchiveAccess` thành công cho cả hai.
8. Sau khi rời, cả hai đều tạo được quỹ Cặp đôi mới.
9. Rời quỹ PERSONAL nhận 400.
10. DB không chứa token gốc, chỉ có hash.
11. Ghi 2 dòng `fund_member` ACTIVE cùng `account_id` và cùng `fund_kind` thì DB báo lỗi unique, tức là chặn được ngay cả khi code có bug.
12. Hai người cùng chấp nhận một link cùng lúc: chỉ một người vào được quỹ.

## Màn hình (web)

1. **Chọn chế độ** (sau màn hình đồng ý):
   - "Đi một mình": vào thẳng app.
   - "Cặp đôi": tạo quỹ chung và hiện link mời. Có nút chia sẻ (Web Share API, gửi qua Zalo hoặc Messenger) và nút sao chép link.
2. **Mở link mời:** chưa đăng nhập thì chuyển sang đăng ký hoặc đăng nhập, xong quay lại. Màn hình hiện "<tên> mời bạn dùng ngân sách chung", bấm "Đồng ý" để vào.
3. **Ngân sách chung:** tên người yêu, trạng thái chờ hoặc đã vào, nút gửi lại link.
4. **Rời ngân sách chung:** hộp xác nhận giải thích hậu quả: đóng vĩnh viễn, mỗi người chỉ giữ khoản mình ghi, tiền thật không bị ảnh hưởng.
5. **Banner** "Ngân sách chung đã đóng" cho người còn lại.

## Success Criteria

- Cả 12 test bắt buộc đều xanh.
- Trên hai điện thoại thật: A tạo link, gửi qua Zalo. B chưa có tài khoản, mở link, đăng ký và vào quỹ chung trong tối đa 5 màn hình.
- A rời quỹ: B mở app thấy banner, và không còn đọc được khoản nào A đã ghi.

## Open Questions

Không có. Luồng rời quỹ và chữ "Ngân sách chung" đã được chủ dự án đồng ý ngày 21/11/2026. Chủ dự án quyết định không ghi "ai trả".
