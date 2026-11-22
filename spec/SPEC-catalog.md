# Spec: catalog — V1

Status: approved (21/11/2026); danh sách nhãn và mức giá giữ như đề xuất

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: account (vai trò ADMIN). Làm song song được với fund.

## Objective

Danh mục riêng của Plandi cho TP Trà Vinh: quán, rạp, phim và suất chiếu. Đây là nguồn dữ liệu chính cho gợi ý, và gợi ý phải chạy được kể cả khi Google và TMDB đều tắt.

Danh mục có ba nguồn:

| Nguồn | Lấy gì | Lưu gì |
|---|---|---|
| Admin (đi thực tế, xem TikTok, xem Facebook) | Tên ngắn, khu vực, loại, mức giá, nhãn, link TikTok, ngày kiểm tra | Lưu hết, đây là dữ liệu của Plandi |
| Google Places, gọi lúc cần | Tên và địa chỉ (chỉ hiện cho admin khi tìm quán); số sao, số lượt đánh giá, giờ mở, link Maps | **Chỉ `place_id`**. Không lưu, không cache nội dung khác |
| TMDB, job đồng bộ hằng tuần | Tên phim (vi-VN), poster, tóm tắt, thời lượng | Lưu, kèm ghi nguồn TMDB ở trang Giới thiệu |

## Quy trình nhập quán (khoảng 1 phút mỗi quán)

1. **Tìm và thêm hàng loạt:**
   - Admin gõ từ khóa, ví dụ "cafe Trà Vinh" hoặc "trà sữa phường 1".
   - Màn hình hiện danh sách tên và địa chỉ lấy trực tiếp từ Google, có đánh dấu quán đã có trong danh mục.
   - Admin tick nhiều quán. Mỗi quán gõ một tên ngắn, chọn khu vực và loại, rồi lưu.
   - Kết quả là các quán **nháp** (`active=false`), chỉ lưu `place_id` cùng phần admin tự nhập.
2. **Hoàn thiện từng quán nháp:**
   - Bấm chọn mức giá và tick nhãn.
   - Dán link TikTok nếu có.
   - Bấm "Đăng". Quán chưa có mức giá thì không đăng được.
3. **Quán không có trên Google:** admin dùng form thêm tay, để trống `place_id`.

Tên quán **không được tự điền sẵn từ Google**. Điền sẵn cũng là lưu nội dung Google, nên trái chính sách.

## Dữ liệu

```sql
CREATE TABLE venue (
    id              BIGSERIAL PRIMARY KEY,
    name            TEXT NOT NULL,          -- admin tự gõ
    google_place_id TEXT UNIQUE,            -- null được: quán nhỏ có thể không có trên Google
    area            TEXT NOT NULL,          -- enum Area (shared)
    category        TEXT NOT NULL,          -- enum VenueCategory
    price_tier      TEXT,                   -- enum PriceTier; null khi còn là quán nháp
    tags            TEXT[] NOT NULL DEFAULT '{}',       -- enum VenueTag
    food_tags       TEXT[] NOT NULL DEFAULT '{}',       -- enum FoodTag
    tiktok_url      TEXT,
    verified_on     DATE NOT NULL,          -- ngày admin kiểm tra gần nhất
    active          BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT venue_active_needs_price CHECK (NOT active OR price_tier IS NOT NULL)
);

CREATE TABLE movie (
    id           BIGSERIAL PRIMARY KEY,
    tmdb_id      BIGINT UNIQUE,             -- null nếu admin tự nhập khi tắt TMDB
    title        TEXT NOT NULL,
    poster_path  TEXT,
    overview     TEXT,
    runtime_min  INT,
    synced_at    TIMESTAMPTZ
);

CREATE TABLE showtime (
    id            BIGSERIAL PRIMARY KEY,
    movie_id      BIGINT NOT NULL REFERENCES movie (id),
    venue_id      BIGINT NOT NULL REFERENCES venue (id),   -- rạp, category = CINEMA
    starts_at     TIMESTAMPTZ NOT NULL,
    format        TEXT NOT NULL DEFAULT '2D',
    price_vnd     BIGINT NOT NULL                          -- giá vé thật, admin nhập
);
```

Các enum cố định trong code, admin chọn từ danh sách. `VenueCategory`, `VenueTag` và `FoodTag` đặt trong `shared` (giống `Area`), vì preference cũng dùng mà không được phụ thuộc vào catalog:

- **`PriceTier`:** giá mỗi người. Mỗi mức có `typicalVnd` để recommendation cộng tổng kế hoạch. Đi cặp thì nhân 2.

  | Mức | Khoảng | `typicalVnd` |
  |---|---|---|
  | `UNDER_30K` | dưới 30k | 25.000 |
  | `FROM_30K` | 30–60k | 45.000 |
  | `FROM_60K` | 60–100k | 80.000 |
  | `FROM_100K` | 100–200k | 150.000 |
  | `OVER_200K` | trên 200k | 250.000 |

- **`VenueCategory`:** `CAFE`, `DRINK` (trà sữa, nước), `FOOD`, `CINEMA`, `ACTIVITY`.
- **`VenueTag`:** `CHEAP`, `ROMANTIC`, `QUIET`, `INDOOR`, `OUTDOOR`, `NICE_VIEW`, `LATE_NIGHT`, `GROUP_FRIENDLY`.
- **`FoodTag`:** mô tả quán, không mô tả người dùng: `HAS_VEGETARIAN`, `MOSTLY_SPICY`, `MOSTLY_SEAFOOD`, `HAS_NON_SPICY`.

Bộ lọc món ăn của người dùng (module preference) được ánh xạ sang các nhãn này trong module recommendation.

Catalog **không lưu id tài khoản nào**, kể cả id của admin đã nhập quán. Vì vậy module này không có dữ liệu cá nhân và không cần cài `PersonalDataHandler`.

## Adapter ngoài

### Google Places (`PlacesGateway`)

```java
public interface PlacesGateway {
    // Enterprise, một lần gọi Place Details cho mỗi placeId: rating, userRatingCount, regularOpeningHours
    Map<String, PlaceSignal> signals(Collection<String> placeIds, LocalDateTime plannedAt);
    // Enterprise: như trên + googleMapsUri, cho thẻ quán
    Optional<PlaceCard> card(String placeId);
    // Pro, chỉ admin: Text Search trả displayName, formattedAddress, id
    List<PlaceSearchHit> search(String query);
}

record PlaceSignal(Double rating, Integer userRatingCount, Boolean openAt) {}  // null = không biết
```

- **Tính `openAt`:** adapter tính từ `regularOpeningHours` và `plannedAt` (giờ Việt Nam), kể cả quán đóng sau nửa đêm. Lịch giờ mở không ra khỏi adapter và không được lưu.
- **Đếm lượt:** mỗi loại SKU một bộ đếm Redis (`google:enterprise:<yyyy-MM>`, `google:pro:<yyyy-MM>`), khóa tự hết hạn sau 40 ngày.
  - Enterprise vượt `plandi.google.enterprise-monthly-limit` (900), hoặc Pro vượt `plandi.google.pro-monthly-limit` (4.500), thì trả kết quả rỗng và không gọi Google.
  - Gọi lỗi hoặc quá 2 giây cũng trả rỗng.
  - Các trường hợp này không bao giờ trả lỗi cho người dùng.
- **Tắt hẳn:** `plandi.google.enabled=false` dùng bean rỗng, không gọi HTTP lần nào.
- **Không lưu kết quả:** kết quả Google chỉ sống trong một request, không ghi vào DB hay Redis.
- **Ghi nguồn:** mọi response có dữ liệu Google đều kèm `"attribution": "Google Maps"`, và web hiện chữ hoặc logo Google Maps cạnh dữ liệu đó, kể cả trên màn hình tìm quán của admin.
- **API key:** lấy từ `GOOGLE_MAPS_API_KEY`. Trên Google Cloud giới hạn key chỉ gọi Places API, chỉ từ IP của VPS, và đặt trần ngân sách.

### TMDB (`MovieSource`)

- **Job đồng bộ:** chạy thứ Hai lúc 03:00 (Asia/Ho_Chi_Minh). Lấy danh sách `now_playing` và `upcoming` với `region=VN`, `language=vi-VN`, rồi upsert theo `tmdb_id`.
- **Tắt TMDB:** khi `plandi.tmdb.enabled=false`, job ghi log rồi bỏ qua. Admin tự nhập phim chỉ với tên.
- **API key:** lấy từ `TMDB_API_TOKEN`.
- **Giới hạn sử dụng:** dữ liệu TMDB chỉ dùng để hiển thị, không dùng để huấn luyện mô hình (theo điều khoản TMDB).

## API

**Người dùng đã đăng nhập:**

| Method | Path | Kết quả |
|---|---|---|
| GET | `/api/v1/venues/{id}` | dữ liệu riêng + `google` (số sao, số lượt, giờ mở hôm nay, link Maps; null nếu không lấy được) + `attribution` + `tiktokUrl` |
| GET | `/api/v1/movies?from=&to=` | phim có suất chiếu trong khoảng thời gian, kèm suất và giá |

**Admin (`/api/v1/admin/**`, chỉ ADMIN):**

| Method | Path | Kết quả |
|---|---|---|
| GET | `/admin/places/search?q=` | kết quả Google (`placeId, name, address, alreadyInCatalog`), chỉ để hiển thị |
| POST | `/admin/venues/bulk` | `[{placeId, name, area, category}]` → tạo quán nháp, bỏ qua `placeId` đã có |
| GET / POST / PUT | `/admin/venues`, `/admin/venues/{id}` | danh sách (lọc nháp, đã đăng, quá hạn kiểm tra), thêm tay, sửa |
| POST | `/admin/venues/{id}/publish` | đăng quán; 400 nếu thiếu mức giá |
| POST | `/admin/venues/{id}/unpublish` | ẩn quán |
| POST | `/admin/venues/{id}/verify` | đặt `verified_on` thành hôm nay |
| GET / POST | `/admin/movies`, `/admin/movies/sync` | xem phim; nhập phim tay; chạy đồng bộ TMDB ngay |
| GET / POST / DELETE | `/admin/showtimes` | quản lý suất chiếu |
| POST | `/admin/showtimes/copy-week` | chép suất chiếu tuần trước sang tuần này để sửa nhanh |

**API Java cho recommendation** (package `com.plandi.catalog`):
- `CatalogQuery.candidates(CandidateFilter f)`: chỉ lọc trên dữ liệu riêng, chỉ lấy quán đã đăng, trả `List<VenueSummary>` (có `priceTier.typicalVnd`). `CandidateFilter` gồm:
  - `areas`, `categories`;
  - `maxPricePerPersonVnd` và `partySize` (1 hoặc 2);
  - `rainy` (bật thì chỉ lấy quán có nhãn `INDOOR`);
  - `requiredFoodTags`, `excludedFoodTags`.
- `PlacesGateway.signals(placeIds, plannedAt)`: lấy số sao và tình trạng mở cửa cho khoảng 10 ứng viên.
- `MovieQuery.showtimes(from, to, maxTicketPriceVnd)`.

## Test bắt buộc

1. **Lọc ứng viên** trên dữ liệu cố định, kiểm tra từng tiêu chí:
   - `typicalVnd` nhân với `partySize`;
   - khu vực;
   - `rainy` chỉ trả quán `INDOOR`;
   - nhãn món ăn bắt buộc và nhãn loại trừ;
   - quán nháp không xuất hiện.
2. Quán không có `google_place_id` vẫn xuất hiện trong danh sách ứng viên và vẫn hiện được chi tiết.
3. **`openAt`** tính từ JSON `regularOpeningHours` cố định:
   - quán mở 07:00–22:00 với giờ đi 21:59 và 22:00;
   - quán mở 18:00–02:00 với giờ đi 00:30 Chủ nhật;
   - quán đóng cửa cả ngày;
   - không có dữ liệu giờ mở thì trả `null`.
4. **Adapter Google**, test bằng `MockRestServiceServer` của spring-test, không thêm dependency:
   - mỗi lần gọi tăng đúng bộ đếm của SKU đó;
   - đạt giới hạn thì không có request HTTP nào được gửi;
   - quá 2 giây thì trả rỗng;
   - `enabled=false` thì không gửi request nào;
   - field mask gửi đi đúng các trường cần, không có trường thừa làm tăng SKU.
5. ArchUnit (có sẵn qua Spring Modulith): không lớp `@Entity` nào phụ thuộc vào package `catalog.internal.google`. Test này chặn việc lỡ lưu dữ liệu Google vào DB.
6. `bulk` bỏ qua `placeId` đã có trong danh mục và tạo quán ở trạng thái nháp. `publish` khi thiếu mức giá nhận 400, và constraint ở DB cũng chặn trường hợp này.
7. Response có dữ liệu Google thì luôn có `attribution`. Khi `google` là null thì không có `attribution`.
8. Job TMDB chạy 2 lần không tạo phim trùng. TMDB tắt thì job không gửi request nào.
9. `/movies?from=&to=` lấy đúng suất chiếu theo giờ Việt Nam: suất 23:30 thứ Bảy không bị tính sang Chủ nhật.
10. `copy-week` chép đủ suất chiếu sang tuần mới, giữ nguyên giờ và giá.

## Màn hình (web)

1. **Admin, tìm và thêm quán:**
   - Ô tìm kiếm và danh sách kết quả Google có checkbox, kèm chữ "Google Maps".
   - Mỗi dòng được tick có ô tên ngắn, khu vực và loại.
   - Nút "Thêm vào danh mục".
2. **Admin, danh sách quán:**
   - Có ba tab: Nháp, Đã đăng, Cần kiểm tra lại (quá 60 ngày).
   - Form hoàn thiện quán có mức giá dạng nút bấm, nhãn dạng tick và ô link TikTok.
3. **Admin, phim và suất chiếu:** lưới theo tuần, có nút "Chép tuần trước".
4. **Thẻ quán** (người dùng):
   - Tên, mức giá, nhãn.
   - Số sao và giờ mở hôm nay từ Google kèm chữ "Google Maps", hoặc ẩn nếu không lấy được.
   - Nút "Mở Google Maps".
   - Video TikTok nhúng qua oEmbed, chỉ tải khi người dùng bấm xem.

## Success Criteria

- Cả 10 test bắt buộc đều xanh.
- Danh mục có ít nhất 50 quán đã đăng và có lịch chiếu CGV Vincom Trà Vinh cho tuần hiện tại.
- Nhập 50 quán, gồm cả bước tìm và bước hoàn thiện, mất dưới 1,5 giờ. Đo và ghi lại kết quả thật.
- Tắt cả Google lẫn TMDB, mở thẻ quán và trang phim vẫn hiện đủ dữ liệu riêng, không có lỗi.

## Out of Scope (Deferred)

- **Ảnh từ Google Places:** tốn thêm SKU và quota. V1 dùng video TikTok nhúng làm phần hình ảnh.
- **Nút "Báo sai" cho người dùng:** cần lưu `account_id` và màn hình duyệt báo cáo. V1 dùng `verified_on` và việc admin kiểm tra lại định kỳ.
- **Nhập quán từ CSV:** cách thêm hàng loạt từ Google đã giải quyết phần lớn việc nhập tay.
- **Bản đồ và tọa độ:** V1 không có bản đồ nên không cần cache lat/lng.
- **Giá trung bình từ người dùng ghi chi:** giai đoạn 2. Lúc đó con số này dần thay cho `typicalVnd`.

## Open Questions

1. **Danh sách nhãn và mức giá:** `VenueTag`, `FoodTag` và năm mức `PriceTier` ở trên là đề xuất. Bạn biết quán Trà Vinh nên kiểm tra xem có thiếu hay thừa không. Đổi về sau thì phải viết migration, nên nên chốt sớm.
2. **Giới hạn kết quả Text Search:** theo mình nhớ, Places API (New) trả tối đa 60 kết quả cho mỗi lần tìm. Có thể phải tìm theo nhiều từ khóa và nhiều khu vực mới đủ. Kiểm tra khi gọi thử Google ở tuần 1.
