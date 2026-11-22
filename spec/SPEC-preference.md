# Spec: preference — V1

Status: approved (21/11/2026)

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: account, privacy. Không phụ thuộc fund hay catalog: các enum dùng chung nằm trong `shared`.

## Objective

Người dùng tự khai mình thích gì và tự chọn bộ lọc món ăn, để gợi ý hợp ngay từ ngày đầu. Họ xem, sửa và xóa được tất cả trên một màn hình.

Module này **chỉ lưu những gì người dùng tự chọn**. Những thứ suy ra từ lịch sử (quán đã đi) không được lưu thành hồ sơ. Recommendation tính chúng ngay lúc gợi ý, và chỉ khi người dùng đã đồng ý cá nhân hóa.

## Dữ liệu người dùng chọn

| Nhóm | Giá trị | Cần đồng ý |
|---|---|---|
| Thích kiểu chỗ nào | `VenueTag`: lãng mạn, yên tĩnh, view đẹp, ngoài trời, mở khuya… | `PERSONALIZATION` |
| Thích đi đâu | `VenueCategory`: cafe, trà sữa, ăn uống, xem phim, vui chơi | `PERSONALIZATION` |
| Bộ lọc món ăn | `FoodFilter`: `VEGETARIAN`, `NO_SPICY`, `NO_SEAFOOD` | `FOOD_FILTERS` |

App **không hỏi lý do** chọn bộ lọc (ví dụ vì sao ăn chay), và không có lựa chọn liên quan đến dị ứng hay sức khỏe.

Recommendation ánh xạ bộ lọc sang nhãn quán (`FoodTag`):

| FoodFilter | Điều kiện lên quán |
|---|---|
| `VEGETARIAN` | quán `FOOD` phải có `HAS_VEGETARIAN` |
| `NO_SPICY` | quán `FOOD` không có `MOSTLY_SPICY`, hoặc có `HAS_NON_SPICY` |
| `NO_SEAFOOD` | quán `FOOD` không có `MOSTLY_SEAFOOD` |

```sql
CREATE TABLE preference (
    account_id       BIGINT PRIMARY KEY,
    liked_tags       TEXT[] NOT NULL DEFAULT '{}',
    liked_categories TEXT[] NOT NULL DEFAULT '{}',
    food_filters     TEXT,                    -- mảng JSON, mã hóa bằng EncryptedStringConverter
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## API

| Method | Path | Body | Kết quả |
|---|---|---|---|
| GET | `/api/v1/preferences` | — | `likedTags, likedCategories, foodFilters` của chính người gọi |
| PUT | `/api/v1/preferences` | `likedTags, likedCategories` | 204; 403 `CONSENT_REQUIRED` nếu chưa đồng ý `PERSONALIZATION` |
| PUT | `/api/v1/preferences/food-filters` | `filters` | 204; 403 `CONSENT_REQUIRED` nếu chưa đồng ý `FOOD_FILTERS` |
| DELETE | `/api/v1/preferences` | — | 204, xóa hết |

Không endpoint nào nhận `accountId` làm tham số. Mỗi người chỉ đọc được sở thích của chính mình, kể cả người yêu cũng không đọc được.

**API Java cho recommendation** (package `com.plandi.preference`):
- `PreferenceQuery.forAccounts(List<Long> accountIds)`: trả về `PreferenceProfile`.
  - Đi một mình thì truyền một id. Đi cặp thì recommendation truyền id hai thành viên lấy từ `FundQuery.activeMemberIds`.
  - **Gộp sở thích theo quyết định đã chốt:**
    - `likedTags` và `likedCategories` là hợp của hai người, kèm số người thích mỗi giá trị (1 hoặc 2). Chỉ tính người đã đồng ý `PERSONALIZATION`.
    - `foodFilters` là hợp bộ lọc của những người đã đồng ý `FOOD_FILTERS`. Một người ăn chay thì kế hoạch cho hai người cũng phải có món chay.
    - Người chưa đồng ý mục nào thì không đóng góp gì cho mục đó.
  - Kết quả chỉ trả số người thích mỗi nhãn, không cho biết ai thích gì.

## Event và quyền riêng tư

- **`ConsentWithdrawn(PERSONALIZATION)`:** xóa ngay `liked_tags` và `liked_categories`.
- **`ConsentWithdrawn(FOOD_FILTERS)`:** xóa ngay `food_filters`.
- **`PersonalDataHandler`** (`deleteOrder = 20`): export toàn bộ dòng của người dùng (giải mã bộ lọc), delete thì xóa dòng đó.
- **Log:** không ghi nhãn hay bộ lọc của người dùng.

## Test bắt buộc

1. PUT sở thích khi chưa đồng ý `PERSONALIZATION`, và PUT bộ lọc khi chưa đồng ý `FOOD_FILTERS`, đều nhận 403 `CONSENT_REQUIRED`.
2. Rút `PERSONALIZATION` thì xóa sở thích nhưng giữ bộ lọc. Rút `FOOD_FILTERS` thì làm ngược lại.
3. Giá trị thô của `food_filters` trong DB không chứa tên bộ lọc.
4. **Gộp cho hai người:**
   - Nhãn được tính đúng số người thích.
   - Người chưa đồng ý không đóng góp gì.
   - Bộ lọc của A (đã đồng ý) vẫn áp dụng dù B chưa đồng ý.
5. `forAccounts` cho một người chưa đồng ý gì trả hồ sơ rỗng, không trả lỗi.
6. Gửi nhãn hoặc bộ lọc không có trong enum thì nhận 400.
7. Export có đủ dữ liệu đã giải mã. Sau khi delete thì không còn dòng nào của người đó.

## Màn hình (web)

1. **"Bạn thích gì?"** (ngay sau màn hình đồng ý, chỉ hiện nếu bật cá nhân hóa):
   - Chip loại chỗ và chip nhãn, chọn bao nhiêu cũng được.
   - Có nút "Bỏ qua".
2. **"Bộ lọc món ăn"** (chỉ hiện nếu bật bộ lọc món ăn):
   - Ba chip: Ăn chay, Không cay, Không hải sản.
   - Dòng chú thích: "Không cần lý do. Khi đi cùng người yêu, bộ lọc được áp dụng cho kế hoạch của cả hai."
3. **"Sở thích của bạn"** (trong Hồ sơ):
   - Xem và sửa cả hai nhóm, có nút "Xóa tất cả".
   - Một đoạn giải thích gợi ý dùng những gì: sở thích bạn chọn, bộ lọc món ăn, và những quán bạn đã ghi chi (nếu bật cá nhân hóa).

## Success Criteria

- Cả 7 test bắt buộc đều xanh.
- Người dùng mới bỏ qua cả hai màn hình mà vẫn nhận được gợi ý.

## Open Questions

Không có.
