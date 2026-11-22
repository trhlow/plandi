# Spec: recommendation — V1

Status: approved (22/11/2026)

Kế thừa [SPEC.md](SPEC.md). Phụ thuộc: fund, budget, catalog, preference (và privacy để kiểm tra đồng ý). Module cuối trong [capability-map.md](capability-map.md).

## Objective

Người dùng mở app, chọn "đi một mình" hoặc "đi cùng người yêu" và chọn thời điểm, rồi nhận 2–3 kế hoạch. Mỗi kế hoạch gồm 2–3 hoạt động có tổng tiền vừa ngân sách còn lại, kèm lý do dễ hiểu.

Thuật toán là **một công thức chấm điểm giải thích được**, không dùng ML hay LLM. Gợi ý vẫn chạy khi Google và TMDB tắt.

## Đầu vào

| Trường | Bắt buộc | Mặc định |
|---|---|---|
| `fundId` | Có | — Quỹ Cá nhân thì đi một mình (`partySize = 1`); quỹ Cặp đôi thì đi cùng người yêu (`partySize = 2`, kể cả khi người yêu chưa vào quỹ) |
| `plannedAt` | Không | 18:00 thứ Bảy gần nhất (giờ Việt Nam). Web có chip "Tối nay / Thứ Bảy / Chủ nhật" |
| `areas` | Không | Khu vực trong hồ sơ của người gọi; có lựa chọn "Toàn thành phố" |
| `rainy` | Không | `false` |
| `excludeVenueIds` | Không | rỗng; dùng cho nút "Gợi ý khác" |

## Pipeline

```
0. Quyền        FundAccess.requireMember(caller, fundId); quỹ CLOSED → 404
1. Ngân sách    budget.suggestibleRemaining(fundId, tháng của plannedAt) → còn lại theo từng mục (âm thì tính 0)
2. Sở thích     preference.forAccounts(members)           ┐ chỉ dùng phần người dùng đã đồng ý
   Lịch sử      budget.visitedVenueIds(fundId, 90 ngày)    ┘ chỉ khi MỌI thành viên đã đồng ý PERSONALIZATION
3. Ứng viên     catalog.candidates(lọc theo mức giá, khu vực, rainy, nhãn món ăn)
                → chấm sơ bộ bằng dữ liệu riêng → giữ tối đa 10 quán, mỗi nhóm loại quán ít nhất 2 nếu có
   Suất chiếu   catalog.showtimes(plannedAt … plannedAt + 3 giờ) nếu mục Xem phim còn tiền
4. Google       PlacesGateway.signals(≤ 10 placeId, plannedAt), gọi song song
                → bỏ quán có openAt = false; quán có openAt = null giữ lại kèm cảnh báo
5. Chấm điểm    điểm đầy đủ cho từng quán (công thức bên dưới)
6. Ghép         2–3 hoạt động theo mẫu, mỗi mục con không vượt số còn lại → chọn 3 kế hoạch tốt nhất, không lặp quán
7. Lý do        câu giải thích cho từng hoạt động và từng kế hoạch
```

### Bước 5: Chấm điểm

**Chất lượng `Q` (trung bình Bayes):**

```latex
Q = \frac{v}{v+m} R + \frac{m}{v+m} C
```

- `R` là số sao, `v` là số lượt đánh giá. Hai tham số lấy từ config: `C = 4.3` và `m = 30` (giả định của tài liệu nghiên cứu).
- Quán không có số sao (Google tắt, hết lượt, hoặc không có `place_id`) thì `Q = C`, tức trung tính.
- Chuẩn hóa về khoảng 0–1: `q = clamp((Q − 3.5) / 1.5, 0, 1)`.

**Các thành phần còn lại, mỗi thành phần trong khoảng 0–1:**

| Thành phần | Cách tính |
|---|---|
| `d`: hợp hẹn hò (chỉ chế độ Cặp đôi) | `min(1, số nhãn trong {ROMANTIC, QUIET, NICE_VIEW} mà quán có / 2)` |
| `p`: hợp sở thích | `0.5` nếu loại quán nằm trong `likedCategories`, cộng `0.5 × min(1, Σ likeCount(tag) / (2 × số thành viên))`. Không có dữ liệu sở thích thì `0` |
| `n`: chỗ mới | `1` nếu chưa đến trong 90 ngày, `0` nếu đã đến. Không được dùng lịch sử thì `0.5` cho mọi quán |

**Điểm quán** = `wQ·q + wD·d + wP·p + wN·n`. Trọng số lấy từ config:

| Chế độ | wQ | wD | wP | wN |
|---|---|---|---|---|
| Cặp đôi (theo tài liệu nghiên cứu) | 0.40 | 0.30 | 0.20 | 0.10 |
| Một mình | 0.50 | 0 | 0.35 | 0.15 |

Bước 3 dùng cùng công thức nhưng thay `q` bằng giá trị trung tính, vì lúc đó chưa có số sao.

### Bước 6: Ghép kế hoạch

- **Mẫu kế hoạch:**
  - `FOOD + (CAFE | DRINK)`
  - `CINEMA + FOOD`
  - `CINEMA + DRINK + FOOD`
  - `CAFE + ACTIVITY`
  - `ACTIVITY + FOOD`
- **Tiền của mỗi hoạt động:**
  - Quán: `priceTier.typicalVnd × partySize`.
  - Suất chiếu: `price_vnd × partySize`.
- **Ràng buộc ngân sách:** cộng tiền các hoạt động theo mục con tương ứng (qua `venueCategories` của budget). Tổng của mỗi mục phải nhỏ hơn hoặc bằng số còn lại của mục đó.
- **Cách chọn:**
  - Với mỗi mẫu, lấy 3 quán tốt nhất cho mỗi vị trí rồi thử mọi tổ hợp.
  - Điểm kế hoạch là trung bình điểm các hoạt động.
  - Chọn tham lam 3 kế hoạch điểm cao nhất, quán đã nằm trong kế hoạch trước thì không dùng lại.
- **Kết quả ổn định:** cùng đầu vào và cùng dữ liệu thì cùng kết quả. Hai quán bằng điểm thì xếp theo `venueId`.

### Bước 7: Lý do

- **Mỗi hoạt động:** ghép từ dữ liệu có sẵn, ví dụ "4.6★ từ 400 đánh giá · khoảng 45k/người · yên tĩnh · chỗ mới".
- **Mỗi kế hoạch:** ví dụ "Tổng khoảng 510k · còn lại 290k cho tháng này".
- **Chế độ Cặp đôi:** lý do chỉ nói "hợp sở thích của hai bạn: lãng mạn", không bao giờ nói ai thích gì.
- **Không có số sao:** bỏ phần sao.
- **Có quán `openAt = null`:** thêm "Kiểm tra giờ mở trước khi đi".

## Bảo vệ hạn mức Google

Hạn mức Enterprise khoảng 900 lượt/tháng, mỗi lần gợi ý tốn tối đa 10 lượt. Như vậy cả app chỉ có khoảng 90 lần gợi ý có số sao mỗi tháng.

- **Giới hạn theo người:** mỗi tài khoản được **3 lần gợi ý có gọi Google mỗi ngày**, đếm trong Redis. Từ lần thứ 4, và khi bấm "Gợi ý khác" vượt mức, gợi ý vẫn trả kết quả nhưng chỉ dùng dữ liệu riêng: không có sao, không lọc giờ mở, kèm cảnh báo.
- **Trần toàn app:** trần 900 lượt/tháng trong adapter vẫn là chốt chặn cuối.
- **Không cache kết quả Google** giữa các request. Lý do: chính sách Google, đã ghi trong `SPEC-catalog`.

## API

| Method | Path | Body | Kết quả |
|---|---|---|---|
| POST | `/api/v1/recommendations` | các trường đầu vào ở trên | `plans[]`, `warnings[]`, `attribution?` |

Mỗi phần tử trong `plans[]`:
- `items[]`, mỗi item gồm:
  - `type`: `VENUE` hoặc `SHOWTIME`;
  - `venueId`, `name`, `category`, `costVnd`, `reason`;
  - `google?`: số sao và số lượt;
  - `showtime?`: phim, giờ chiếu.
- `totalVnd`, `remainingAfterVnd`, `reason`.

`warnings[]` gồm:
- `HOURS_UNVERIFIED`: có quán chưa kiểm tra được giờ mở.
- `GOOGLE_LIMITED`: đang dùng dữ liệu riêng do vượt giới hạn hoặc Google tắt.
- `NO_BUDGET`: mọi mục có gợi ý đều hết tiền hoặc chưa đặt ngân sách.
- `NOT_ENOUGH_CANDIDATES`: ít hơn 2 kế hoạch thỏa ràng buộc.

Mỗi cảnh báo kèm một câu gợi ý hành động, ví dụ "Đặt ngân sách mục Ăn uống để có gợi ý ăn tối".

Recommendation **không lưu gì** trong V1: không lưu lịch sử gợi ý, không lưu hồ sơ. Vì vậy module này không cần cài `PersonalDataHandler`. Log chỉ ghi `fundId`, số ứng viên, số lượt gọi Google và thời gian xử lý.

## Test bắt buộc

1. Công thức Bayes khớp bảng trong tài liệu nghiên cứu: quán B được 4.58, A được 4.38, C được 4.20 (với `C = 4.3`, `m = 30`).
2. **Không bao giờ vượt ngân sách:** dùng 1.000 bộ ngân sách và danh mục ngẫu nhiên với seed cố định. Mọi kế hoạch trả về có tổng theo từng mục con không vượt số còn lại. Test chỉ dùng JUnit, không thêm dependency.
3. **Cặp đôi không thấy dữ liệu cá nhân:** A có khoản chi ở quán X và ngân sách lớn trong quỹ Cá nhân. Gợi ý ở chế độ Cặp đôi phải ra kết quả giống hệt khi A không có các dữ liệu đó.
4. **Google tắt:** vẫn có kế hoạch, không có sao, có `GOOGLE_LIMITED` và `HOURS_UNVERIFIED`, không có `attribution`.
5. **Giờ mở:** quán có `openAt = false` không xuất hiện. Quán có `openAt = null` vẫn xuất hiện, kèm cảnh báo.
6. Mỗi request gửi tối đa 10 `placeId` cho `signals` (kiểm tra bằng fake gateway).
7. Lần gợi ý thứ 4 trong ngày của cùng tài khoản không gọi `signals` lần nào, và vẫn trả kế hoạch.
8. **Thiếu đồng ý `PERSONALIZATION`:** nếu một thành viên chưa đồng ý thì kết quả không phụ thuộc vào lịch sử quán đã đến, tức là thay đổi lịch sử không làm đổi thứ hạng.
9. Bộ lọc `VEGETARIAN` loại mọi quán `FOOD` không có `HAS_VEGETARIAN`.
10. Ngân sách các mục có gợi ý đều bằng 0 thì `plans` rỗng và có cảnh báo `NO_BUDGET`.
11. **Ổn định:** gọi hai lần với cùng đầu vào thì ra cùng kết quả. Quán trong `excludeVenueIds` không xuất hiện.
12. Không quán nào xuất hiện trong nhiều hơn một kế hoạch của cùng một response.
13. **Quyền:** gợi ý cho quỹ không phải của mình nhận 404. Gợi ý cho quỹ CLOSED cũng nhận 404.

## Màn hình (web)

1. **Trang chủ:**
   - Chọn đi một mình hay đi cùng người yêu.
   - Chọn thời điểm bằng chip (Tối nay, Thứ Bảy, Chủ nhật).
   - Công tắc "Trời mưa".
   - Ngay bên dưới là 2–3 thẻ kế hoạch.
2. **Thẻ kế hoạch:** các hoạt động theo thứ tự, mỗi hoạt động có tiền và lý do. Cuối thẻ có tổng tiền và số còn lại. Có chữ "Google Maps" khi thẻ hiện số sao.
3. **Nút trên thẻ:**
   - "Xem quán": mở thẻ quán.
   - "Ghi lại sau khi đi": mở form ghi nhanh với dữ liệu điền sẵn.
   - "Gợi ý khác": gọi lại với `excludeVenueIds`.
4. **Không có kế hoạch:** hiện câu giải thích theo cảnh báo, kèm nút dẫn tới chỗ cần sửa (đặt ngân sách, chọn toàn thành phố).

## Success Criteria

- Cả 13 test bắt buộc đều xanh.
- Thời gian trả về gợi ý ở p95 dưới 2 giây khi có gọi Google, dưới 300 ms khi không gọi. Đo trên staging với 50 quán.
- Hai người dùng thật thấy lý do gợi ý dễ hiểu. Hỏi trực tiếp và ghi lại câu trả lời.

## Out of Scope (Deferred)

- **LLM viết lời gợi ý:** nếu làm sau này, LLM chỉ được chọn từ kết quả có thật.
- **"Muốn quay lại không?" và đánh giá nội bộ thay dần số sao Google:** giai đoạn 2.
- **Xếp giờ cho từng hoạt động trong kế hoạch, tính khoảng cách và đường đi:** chưa làm ở V1.
- **Lưu lịch sử gợi ý để đo tỉ lệ được chọn:** chỉ làm khi cần số liệu cho CV, và phải xin đồng ý thêm.

## Decisions (22/11/2026)

- **Trọng số chế độ Một mình:** 0.50 / 0 / 0.35 / 0.15, dùng tạm và chỉnh khi có dữ liệu.
- **Ngân sách:** kiểm tra theo từng mục con, không theo tổng.
- **Giới hạn Google:** mỗi tài khoản 3 lần gợi ý có số sao mỗi ngày. Trần 900 lượt/tháng của adapter là chốt chặn cuối.
