# Spec: budget — V1

Status: approved (21/11/2026)

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: fund (`FundAccess`, `FundQuery`, các event), catalog (kiểm tra `venueId`), privacy (đồng ý, mã hóa, `PersonalDataHandler`).

## Objective

Mỗi quỹ có các mục con với ngân sách theo tháng. Người dùng ghi một khoản chi trong tối đa 3 lần chạm (dưới 10 giây) và thấy ngay số tiền còn lại. Recommendation lấy số tiền còn lại ở đây để gợi ý kế hoạch vừa túi.

Đây là **sổ ghi**, không chứa tiền thật. Không ghi "ai trả" và không tính ai nợ ai.

## Mục con mặc định

Mục con được tạo lần đầu khi quỹ được mở trên màn hình (khởi tạo lười), theo mẫu của loại quỹ. Người dùng sửa tên, ẩn, hoặc thêm mục mới được.

| Quỹ Cá nhân | Quỹ Cặp đôi | Có gợi ý? | Loại quán tương ứng |
|---|---|---|---|
| Cafe, đi chơi | Hẹn hò, đi chơi | Có | `CAFE`, `DRINK`, `ACTIVITY` |
| Ăn uống | Ăn uống | Có | `FOOD` |
| Xem phim, giải trí | Xem phim | Có | `CINEMA` |
| Mua sắm | Quà tặng, dịp đặc biệt | Không | — |
| Tiền trọ, sinh hoạt | Tiền trọ, sinh hoạt chung | Không | — |

- **Mục có gợi ý** là nguồn ngân sách cho recommendation. Một kế hoạch "phim + trà sữa + ăn tối" lấy tiền từ ba mục có gợi ý.
- **Mục người dùng tự thêm** mặc định không có gợi ý.

## Dữ liệu

```sql
CREATE TABLE budget_category (
    id               BIGSERIAL PRIMARY KEY,
    fund_id          BIGINT NOT NULL,
    name             TEXT NOT NULL,           -- 1–40 ký tự
    venue_categories TEXT[] NOT NULL DEFAULT '{}',
    suggestible      BOOLEAN NOT NULL DEFAULT false,
    sort_order       INT NOT NULL,
    archived         BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (fund_id, name)                    -- chặn tạo trùng mục mặc định khi hai request cùng khởi tạo
);

CREATE TABLE budget_amount (
    category_id BIGINT NOT NULL REFERENCES budget_category (id),
    month       DATE NOT NULL,                -- ngày 1 của tháng
    amount_vnd  BIGINT NOT NULL CHECK (amount_vnd >= 0),
    PRIMARY KEY (category_id, month)
);

CREATE TABLE expense (
    id                    BIGSERIAL PRIMARY KEY,
    fund_id               BIGINT NOT NULL,
    category_id           BIGINT NOT NULL REFERENCES budget_category (id),
    amount_vnd            BIGINT NOT NULL CHECK (amount_vnd > 0 AND amount_vnd <= 100000000),
    spent_on              DATE NOT NULL,      -- ngày theo giờ Việt Nam
    venue_id              BIGINT,             -- quán trong catalog, không bắt buộc
    note                  TEXT,               -- mã hóa bằng EncryptedStringConverter, tối đa 200 ký tự
    created_by_account_id BIGINT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX expense_fund_month_idx ON expense (fund_id, spent_on);
```

- **Không có khóa ngoại sang bảng của module khác** (`fund`, `venue`). Kiểm tra tồn tại bằng API Java của module đó. Khi quỹ bị xóa thì nghe event `FundDeleted` để dọn dữ liệu.
- **Mã hóa:** chỉ `note` được mã hóa. `amount_vnd` không mã hóa vì cần SUM trong SQL, lý do đã ghi ở `SPEC-privacy`.
- **Tháng chưa đặt ngân sách:** dùng số của tháng gần nhất trước đó và trả kèm cờ `carriedForward=true`. Người dùng bấm lưu thì mới ghi dòng cho tháng mới.
- **Chi vượt ngân sách:** được phép. Số còn lại âm thì hiện màu đỏ, không chặn.

## Quy tắc truy cập

| Tình huống | Đọc | Ghi, sửa, xóa |
|---|---|---|
| Quỹ ACTIVE, là thành viên | Mọi khoản trong quỹ, kể cả khoản người yêu ghi | Thêm mới được; sửa và xóa **chỉ khoản mình ghi** (khoản của người kia trả 403) |
| Quỹ CLOSED, là thành viên cũ | **Chỉ khoản mình ghi** (`requireArchiveAccess`) | Chỉ xóa được khoản mình ghi; không thêm, không sửa |
| Không phải thành viên | 404 | 404 |

- **Đồng ý:** thêm khoản chi cần người ghi đã đồng ý `SPENDING_LOG`, nếu chưa thì trả 403 `CONSENT_REQUIRED`. Đặt ngân sách không cần đồng ý này.

## API

Mọi đường dẫn đều bắt đầu bằng `/api/v1`.

| Method | Path | Body | Kết quả |
|---|---|---|---|
| GET | `/funds/{fundId}/categories` | — | danh sách mục con; tự tạo mục mặc định nếu chưa có |
| POST | `/funds/{fundId}/categories` | `name` | 201 |
| PATCH | `/funds/{fundId}/categories/{id}` | `name?, archived?, sortOrder?` | 200 |
| GET | `/funds/{fundId}/budgets/{yyyy-MM}` | — | mỗi mục: `budgetVnd, spentVnd, remainingVnd, carriedForward` + tổng |
| PUT | `/funds/{fundId}/budgets/{yyyy-MM}` | `[{categoryId, amountVnd}]` | 200 |
| POST | `/funds/{fundId}/expenses` | `categoryId, amountVnd, spentOn?, venueId?, note?` | 201; `spentOn` mặc định là hôm nay (giờ Việt Nam) |
| GET | `/funds/{fundId}/expenses?month=yyyy-MM` | — | danh sách khoản chi; quỹ CLOSED chỉ trả khoản của người gọi |
| PATCH / DELETE | `/funds/{fundId}/expenses/{id}` | — | chỉ người ghi; người khác trong quỹ nhận 403 |
| GET | `/funds/{fundId}/summary?month=yyyy-MM` | — | tổng theo mục, số buổi đi chơi, các quán đã đến |
| DELETE | `/expenses/mine` | — | xóa mọi khoản chi do người gọi ghi ở mọi quỹ (dùng khi rút đồng ý) |

**API Java cho recommendation** (package `com.plandi.budget`):
- `BudgetQuery.suggestibleRemaining(long fundId, YearMonth month)`: trả `List<CategoryRemaining(categoryId, name, remainingVnd, venueCategories)>`, chỉ gồm mục có gợi ý.
- `BudgetQuery.visitedVenueIds(long fundId, LocalDate since)`: dùng cho điểm "chỗ mới chưa đi". Kết quả chỉ lấy từ khoản chi **của đúng quỹ được hỏi**, nên chế độ Cặp đôi không bao giờ nhìn thấy chi tiêu cá nhân.

Recommendation phải gọi `FundAccess` trước khi gọi các hàm này.

## Event và quyền riêng tư

- **`ConsentWithdrawn(SPENDING_LOG)`:** không xóa gì tự động. Từ lúc đó người dùng không thêm khoản chi mới được, và màn hình hiện nút "Xóa toàn bộ chi tiêu đã ghi" (gọi `DELETE /expenses/mine`).
- **`FundDeleted`:** xóa toàn bộ `expense`, `budget_amount` và `budget_category` của quỹ đó.
- **`FundClosed`:** không cần làm gì. Quy tắc đọc với quỹ CLOSED đã được áp dụng qua `FundAccess`.
- **`PersonalDataHandler`** (`deleteOrder = 20`):
  - **Export:** khoản chi do mình ghi ở mọi quỹ (giải mã `note`), ngân sách và mục con của quỹ Cá nhân, và tổng chi theo tháng của quỹ Cặp đôi. Không xuất khoản người yêu ghi.
  - **Delete:** xóa mọi khoản chi có `created_by_account_id` là người này. Ngân sách và mục con của quỹ được dọn khi fund phát `FundDeleted`.
- **Log:** không ghi số tiền, ghi chú hay tên mục con. Chỉ ghi `expenseId`, `fundId` và mã lỗi.

## Test bắt buộc

1. Lần GET đầu tiên tạo đúng 5 mục mặc định theo loại quỹ. Hai request đồng thời không tạo mục trùng.
2. Số còn lại bằng ngân sách trừ chi trong tháng. Khoản ngày 31/10 không tính vào tháng 11. Tháng chưa đặt ngân sách lấy số tháng trước và có `carriedForward=true`.
3. Chưa đồng ý `SPENDING_LOG` mà thêm khoản chi thì nhận 403 `CONSENT_REQUIRED`.
4. **Truy cập chéo:** với mọi endpoint `/funds/{fundId}/**`: người ngoài quỹ nhận 404; người yêu gọi vào quỹ Cá nhân của người kia nhận 404.
5. **Quỹ Cặp đôi ACTIVE:** B đọc được khoản A ghi. B sửa hoặc xóa khoản của A thì nhận 403.
6. **Quỹ CLOSED:** mỗi người chỉ đọc được khoản mình ghi. Thêm khoản mới nhận 404. Xóa khoản của mình thì thành công.
7. Giá trị thô của `note` trong DB không chứa chuỗi gốc.
8. Log khi tạo khoản chi không chứa số tiền hay ghi chú. Kiểm tra bằng `OutputCaptureExtension`.
9. `suggestibleRemaining` và `visitedVenueIds` của quỹ Cặp đôi không bị ảnh hưởng bởi khoản chi trong quỹ Cá nhân của bất kỳ thành viên nào.
10. `DELETE /expenses/mine` xóa khoản của người gọi ở mọi quỹ và giữ nguyên khoản của người yêu.
11. Export chứa khoản của mình cùng tổng tháng của quỹ Cặp đôi, và không chứa khoản người yêu ghi.
12. Sau `FundDeleted`, không còn dòng nào của quỹ đó trong ba bảng.
13. `amountVnd` bằng 0, âm, hoặc lớn hơn 100 triệu thì nhận 400. `venueId` không tồn tại trong catalog cũng nhận 400.

## Màn hình (web)

1. **Ghi nhanh** (nút nổi ở mọi màn hình, mở dạng bottom sheet):
   - Bàn phím số, nhập theo nghìn: gõ `180` được 180.000đ.
   - Chip mục con, chọn sẵn mục dùng gần nhất.
   - Nút Lưu.
   - Ngày, quán và ghi chú nằm trong phần "Thêm chi tiết", mặc định đóng.
   - Ghi từ một thẻ kế hoạch thì quán, mục và số tiền điển hình được điền sẵn. Khi đó chỉ cần 2 lần chạm.
2. **Ngân sách tháng:** mỗi mục có thanh tiến độ và số còn lại. Chuyển được giữa Ngân sách cá nhân và Ngân sách chung.
3. **Đặt ngân sách:** nhập số cho từng mục. Có nút "Giống tháng trước".
4. **Tổng kết tháng:** tổng theo mục, số buổi đi chơi, các quán đã đến. Đây là bản đơn giản, Wrapped để sau.
5. **Lưu trữ ngân sách chung đã đóng:** danh sách chỉ đọc các khoản mình ghi, có nút xóa.

## Success Criteria

- Cả 13 test bắt buộc đều xanh.
- Ba người thử ghi một khoản chi: từ lúc chạm nút đến lúc lưu tối đa 3 lần chạm, thời gian trung vị dưới 10 giây. Đo và ghi lại kết quả.
- Trên hai điện thoại: A ghi một khoản vào ngân sách chung, B mở app thấy ngay số còn lại đã giảm.

## Out of Scope (Deferred)

- **Wrapped tổng kết tháng, xuất ảnh story:** chỉ làm nếu còn thời gian (theo `SPEC.md`).
- **Khoản chi định kỳ:** ví dụ tiền trọ tự ghi mỗi tháng.
- **Nhập chi tiêu bằng câu tự nhiên:** giai đoạn 2.
- **Ảnh hóa đơn:** chưa cần ở V1.
- **Ghi "ai trả", chia tiền:** chủ dự án quyết định không làm.

## Decisions (21/11/2026)

- **Mục có gợi ý:** ba mục (đi chơi, ăn uống, xem phim), không chỉ một mục như tài liệu nghiên cứu.
- **Tháng mới chưa đặt ngân sách:** tự dùng số của tháng trước.
