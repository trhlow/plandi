# Plandi V1 — Task list

Kế hoạch tổng ở [plan.md](plan.md). Mỗi task code đi theo `incremental-implementation` + `test-driven-development`. Test bắt buộc đánh số theo spec của từng module, ví dụ "account #1–4" là test 1–4 trong `SPEC-account.md`.

Lệnh kiểm tra chung:
- Backend: `cd backend && ./mvnw verify`
- Web: `cd web && npm run lint && npm test && npm run build`

---

## Phase 0: Việc của chủ dự án (tuần 1, không code)

- [ ] **O1.** Phỏng vấn 5–8 cặp đôi và 4–5 người độc thân, ghi nguyên văn (Cổng 1, mục 2 tài liệu nghiên cứu).
- [ ] **O2.** Tạo dự án Google Cloud. Gọi thử Place Details cho 5 quán ở Trà Vinh. Thử Text Search xem trả tối đa bao nhiêu kết quả. Giới hạn API key theo IP và đặt trần ngân sách.
- [ ] **O3.** Mở app CGV, đếm số phim và số suất trong một tuần ở CGV Vincom Trà Vinh, ghi lại giá vé.
- [ ] **O4.** Xác nhận bản nháp 5 khu vực trong `SPEC-account.md` (Claude đã tra Wikipedia và Google Maps ngày 22/11/2026).
- [ ] **O5.** Tra quy định về dữ liệu trẻ em trong Luật 91/2025 (ngưỡng tuổi).
- [ ] **O6.** ~~Đăng ký tên miền~~: dùng `evts.id.vn` sẵn có (hết hạn 26/07/2028, trang cũ đã dừng), app ở tên miền con `plandi.evts.id.vn`. Còn lại: chuyển DNS sang Cloudflare ở Task 5.
- [ ] **O7.** Đăng ký TMDB API token.

---

## Phase 1: Nền móng

### Task 1: Scaffold backend ✅
**Description:** Tạo `backend/` gồm Maven wrapper, Java 21, Spring Boot 4.x, Spring Modulith, Flyway, JPA, Redis, Security, Actuator. Tạo package rỗng cho 7 module và `shared`. Viết lớp test nền dùng Testcontainers Postgres và Redis. Tạo `compose.yaml` cho môi trường local.

**Acceptance criteria:**
- [x] `./mvnw verify` xanh. Test `ApplicationModules.of(PlandiApplication.class).verify()` chạy và pass.
- [x] `docker compose up -d postgres redis` rồi chạy jar với profile `local` thì `/actuator/health` trả 200 `UP`. Dùng jar thay cho `spring-boot:run`, xem ghi chú bên dưới.
- [x] Spotless chạy được, `.env.example` có đủ biến môi trường.

**Done 22/11/2026:**
- Dùng Spring Boot 4.2.0 và Spring Modulith 2.2.0 (start.spring.io chọn 2.2.0-M1, mình đổi sang bản chính thức).
- `allowedDependencies` theo capability map, có thử cố ý vi phạm để chắc test fail.
- Wrapper ghim Maven 3.9.16, vì 3.10.1 crash khi đường dẫn có dấu tiếng Việt. `spring-boot:run` cũng hỏng vì lý do tương tự, nên chạy bằng jar.

**Verification:** `./mvnw verify`; gọi thủ công `curl localhost:8080/actuator/health`.
**Dependencies:** Không.
**Files:** `backend/pom.xml`, `backend/src/main/java/com/plandi/PlandiApplication.java`, `backend/src/test/java/com/plandi/ModularityTest.java`, `backend/src/test/java/com/plandi/IntegrationTest.java`, `compose.yaml`, `.env.example`
**Scope:** M. Kiểm tra trước tiên: Boot 4, Modulith và Testcontainers có tương thích nhau không. Không hợp thì ghi lại và hỏi chủ dự án.

### Task 2: Năm ADR
**Description:** Ghi 5 ADR theo mục 8 tài liệu nghiên cứu: monolith hay microservices; web/PWA hay native; cách gọi Google (xếp hạng lại); không dùng LLM; một repo.

**Acceptance criteria:**
- [x] `docs/adr/0001…0005-*.md` đủ các phần: bối cảnh, quyết định, phương án khác, hệ quả.
- [ ] Chủ dự án đọc và tự giải thích lại được từng ADR.

**Verification:** Chủ dự án đọc và tự giải thích lại được từng ADR.
**Dependencies:** Không.
**Files:** `docs/adr/*.md`
**Scope:** S.

### Task 3: Scaffold web ✅
**Description:** Tạo `web/` gồm Vite, React, TypeScript và vite-plugin-pwa. Viết fetch wrapper tự gửi CSRF token đọc từ cookie, cùng hàm format tiền VND. Cấu hình ESLint và Vitest. Dev server proxy `/api` sang backend.

**Acceptance criteria:**
- [x] `npm run lint && npm test && npm run build` xanh.
- [x] Có test Vitest cho hàm format tiền: `180000` hiển thị thành `180.000đ`; nhập `180` thì được `180000`.
- [ ] Mở trên điện thoại thì cài được thành PWA (có manifest và icon). Đã kiểm tra manifest và icon trên máy dev; cài thử trên điện thoại cần HTTPS, làm ở Task 5.

**Done 22/11/2026:**
- Vite 8, React 19, TypeScript, Vitest 5, vite-plugin-pwa 2.0.
- Lint dùng oxlint (mặc định của create-vite hiện tại) thay cho ESLint: cùng lệnh `npm run lint`, không thêm dependency.
- Icon PNG vẽ từ `public/logo.svg` bằng script Python một lần, không thêm công cụ.
- `npm audit` báo 4 lỗi mức moderate trong `glob`/`minimatch` mà `workbox-build` dùng lúc build. Chỉ chạy khi build, không vào bản chạy trên trình duyệt. Xem lại khi vite-plugin-pwa ra bản mới.

**Verification:** Lệnh web chung; kiểm tra thủ công bằng Lighthouse PWA.
**Dependencies:** Không.
**Files:** `web/package.json`, `web/vite.config.ts`, `web/src/lib/api.ts`, `web/src/lib/money.ts`, `web/src/lib/money.test.ts`
**Scope:** M.

### Task 4: CI GitHub Actions ✅
**Description:** Workflow chạy trên mỗi PR vào `main`: job backend chạy `./mvnw verify`, job web chạy lint, test và build.

**Acceptance criteria:**
- [x] Một PR thử có cả hai job xanh (PR #1, 22/11/2026: backend 47 giây, web 16 giây).
- [x] Testcontainers chạy được trên runner `ubuntu-latest`.

**Verification:** Mở một PR thử.
**Dependencies:** Task 1, 3.
**Files:** `.github/workflows/ci.yml`
**Scope:** S.

### Task 5: Staging tại nhà
**Description:** Viết `compose.prod.yml` (app, postgres, redis) và Dockerfile của backend phục vụ luôn `web/dist`. Chạy trên chính máy dev của chủ dự án (Windows, Docker Desktop), mở ra Internet qua Cloudflare Tunnel (`cloudflared` chạy như service). Máy phải bật thì link mới sống. Viết script deploy thủ công. Chỉ dùng dữ liệu giả.

**Acceptance criteria:**
- [x] `https://plandi-staging.evts.id.vn/actuator/health` trả 200 qua Cloudflare Tunnel (22/11/2026).
- [x] Postgres và Redis không mở port ra ngoài.
- [ ] `deploy.sh` chạy được: `git pull` → build → `docker compose up -d`.

**Verification:** Mở link staging từ điện thoại dùng 4G.
**Dependencies:** Task 1, 3, O6.
**Files:** `backend/Dockerfile`, `compose.prod.yml`, `deploy.sh`, `docs/staging.md`
**Scope:** M.

### Checkpoint 1 (cuối tuần 1)
- [ ] CI xanh, staging trả 200, 5 ADR đã có.
- [ ] Cập nhật mục Status và Commands trong `CLAUDE.md`.
- [ ] Chủ dự án duyệt trước khi sang Phase 2. Nếu Cổng 1 trượt thì dừng lại xem lại spec.

---

## Phase 2: account + privacy

### Task 6: Đăng ký, đăng nhập, đăng xuất, /me
**Description:** Tạo bảng `account` và enum `Area` (dùng danh sách tạm nếu O4 chưa xong). Cấu hình Spring Security với session lưu trong Redis và CSRF đặt trong cookie. Làm các endpoint `register`, `login`, `logout`, `/me`, cùng `CurrentAccount` và event `AccountRegistered`.

**Acceptance criteria:**
- [ ] Test account #1, #2, #3, #5, #6, #8 xanh.
- [ ] Ngoài `/auth/**`, `/areas` và `/actuator/health`, mọi endpoint trả 401 khi chưa đăng nhập.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 1.
**Files:** `V1__account.sql`, `account/AccountService.java`, `account/CurrentAccount.java`, `account/internal/AuthController.java`, `shared/SecurityConfig.java`, các test
**Scope:** M. Dùng skill `security-and-hardening`.

### Task 7: Khóa tạm khi sai, đổi mật khẩu, hồ sơ, khu vực
**Description:** Khóa tạm theo email + IP dùng Redis. Endpoint `PUT /me/password` hủy các phiên khác. `PATCH /me` sửa hồ sơ, `GET /areas` trả danh sách khu vực. Thêm guard ADMIN cho `/admin/**`. Log không chứa email.

**Acceptance criteria:**
- [ ] Test account #4, #7, #9, #10 xanh.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 6.
**Files:** `account/internal/LoginThrottle.java`, `account/internal/ProfileController.java`, các test
**Scope:** S.

### Task 8: Đặt lại mật khẩu bằng mã admin
**Description:** Tạo bảng `password_reset`, endpoint admin tạo mã và endpoint `reset-password`. Mã cũ chưa dùng hết hiệu lực khi tạo mã mới. Đặt lại xong thì hủy mọi phiên.

**Acceptance criteria:**
- [ ] Test account #11, #12 xanh. DB chỉ lưu hash của mã.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 7.
**Files:** `V2__password_reset.sql`, `account/internal/PasswordResetService.java`, các test
**Scope:** S.

### Task 9: Đồng ý theo mục đích
**Description:** Tạo bảng `consent_event`. Ghi `TERMS` khi nghe `AccountRegistered`. Làm `ConsentService`, event `ConsentWithdrawn`, API `/consents`. Thêm filter trả 403 `TERMS_REQUIRED` khi `policyVersion` tăng.

**Acceptance criteria:**
- [ ] Test privacy #1, #2, #3, #11 xanh.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 6.
**Files:** `V3__consent.sql`, `privacy/ConsentService.java`, `privacy/internal/ConsentController.java`, `privacy/internal/TermsFilter.java`, các test
**Scope:** M.

### Task 10: Mã hóa cột nhạy cảm
**Description:** Viết `EncryptedStringConverter` dùng AES-256-GCM với khóa lấy từ `PLANDI_DATA_KEY`. Profile `prod` thiếu khóa thì app không khởi động.

**Acceptance criteria:**
- [ ] Test privacy #9, #10 xanh.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 1.
**Files:** `privacy/EncryptedStringConverter.java`, `privacy/internal/DataKeyConfig.java`, test
**Scope:** S.

### Task 11: Xuất dữ liệu và xóa tài khoản
**Description:** Tạo interface `PersonalDataHandler`. Account và privacy tự cài handler của mình. Làm endpoint `GET /me/export` (tối đa 5 lần/ngày) và `DELETE /me` (xác nhận bằng mật khẩu, xóa trong một transaction, hủy phiên). Viết test quét mọi bảng có cột tên kết thúc bằng `account_id`.

**Acceptance criteria:**
- [ ] Test privacy #4–#8 xanh.
- [ ] Test quét (#6) tự áp dụng cho bảng mới mà không phải sửa test.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 8, 9.
**Files:** `privacy/PersonalDataHandler.java`, `privacy/internal/ExportService.java`, `privacy/internal/AccountDeletionService.java`, `account/internal/AccountDataHandler.java`, test
**Scope:** M. Dùng skill `security-and-hardening`.

### Task 12: Web: tài khoản và quyền riêng tư
**Description:** Làm các màn hình đăng ký, đăng nhập (có "Quên mật khẩu?"), hồ sơ, đồng ý (3 công tắc), quyền riêng tư (xuất dữ liệu, xóa tài khoản) và admin đặt lại mật khẩu. Claude soạn bản nháp trang Điều khoản và Chính sách, có ghi nguồn TMDB và Google Maps.

**Acceptance criteria:**
- [ ] Trên điện thoại: đăng ký → đồng ý → đóng trình duyệt → mở lại sau 1 ngày vẫn còn đăng nhập.
- [ ] Xóa tài khoản trên staging rồi đăng nhập lại thì nhận 401.

**Verification:** Lệnh web chung và thử thủ công trên staging.
**Dependencies:** Task 3, 11.
**Files:** `web/src/features/account/*`, `web/src/features/privacy/*`, `web/src/pages/terms.tsx`
**Scope:** M. Dùng skill `frontend-ui-engineering`.

### Checkpoint 2 (cuối tuần 3)
- [ ] Mọi test của account và privacy xanh. Test kiểm tra module xanh.
- [ ] Staging: đăng ký, đồng ý, xuất dữ liệu, xóa tài khoản đều chạy.
- [ ] Chủ dự án duyệt.

---

## Phase 3: fund

### Task 13: Quỹ Cá nhân + FundAccess
**Description:** Tạo bảng `fund` và `fund_member` kèm partial unique index. Tạo quỹ PERSONAL khi nghe `AccountRegistered`. Làm `FundAccess`, `FundQuery`, các endpoint `GET /funds` và `GET /funds/{id}`, và `PersonalDataHandler` của fund.

**Acceptance criteria:**
- [ ] Test fund #1, #9, #11 xanh, và test #6 cho các endpoint đã có.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 11.
**Files:** `V4__fund.sql`, `fund/FundAccess.java`, `fund/FundQuery.java`, `fund/internal/*`, test
**Scope:** M.

### Task 14: Quỹ Cặp đôi + link mời
**Description:** Tạo bảng `fund_invite`, lưu token dưới dạng hash, thời gian dùng bean `Clock`. Làm các endpoint tạo quỹ Cặp đôi, tạo và thu hồi link, xem lời mời, chấp nhận lời mời, cùng bảng mã lỗi. Mỗi tài khoản tạo tối đa 10 link/ngày.

**Acceptance criteria:**
- [ ] Test fund #2–#5, #10, #12 xanh, và test #6 mở rộng cho các endpoint mới.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 13.
**Files:** `V5__fund_invite.sql`, `fund/internal/InviteService.java`, `fund/internal/InviteController.java`, test
**Scope:** M. Dùng skill `security-and-hardening`.

### Task 15: Rời quỹ
**Description:** Luồng rời quỹ trong một transaction. Phát event `FundClosed` và `FundDeleted`. Làm `requireArchiveAccess`. Handler xóa dữ liệu chạy luồng rời quỹ trước khi xóa.

**Acceptance criteria:**
- [ ] Test fund #7, #8 xanh. Test quét của privacy (#6) vẫn xanh khi người bị xóa đang ở quỹ Cặp đôi.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 14.
**Files:** `fund/internal/LeaveService.java`, `fund/FundClosed.java`, `fund/FundDeleted.java`, test
**Scope:** S.

### Task 16: Web: ngân sách chung
**Description:** Làm các màn hình chọn chế độ, chia sẻ link (Web Share API và nút sao chép), nhận lời mời (chưa đăng nhập thì chuyển đi đăng ký hoặc đăng nhập rồi quay lại), ngân sách chung, hộp xác nhận rời quỹ và banner "đã đóng". Giao diện không dùng chữ "quỹ".

**Acceptance criteria:**
- [ ] Trên hai điện thoại: B chưa có tài khoản, mở link từ Zalo và vào được quỹ trong tối đa 5 màn hình.
- [ ] A rời quỹ thì B thấy banner.

**Verification:** Thử thủ công trên staging bằng hai điện thoại.
**Dependencies:** Task 12, 15.
**Files:** `web/src/features/fund/*`
**Scope:** M.

### Checkpoint 3 (cuối tuần 4)
- [ ] Mọi test của fund xanh. Kiểm tra lại test truy cập chéo phủ hết endpoint.
- [ ] Chủ dự án duyệt.

---

## Phase 4: catalog

### Task 17: Danh mục quán + lọc ứng viên
**Description:** Đưa các enum `VenueCategory`, `VenueTag`, `FoodTag` vào `shared`, tạo enum `PriceTier`. Tạo bảng `venue` kèm constraint. Làm API admin thêm, sửa, đăng, ẩn và xác nhận đã kiểm tra quán. Làm `CatalogQuery.candidates`.

**Acceptance criteria:**
- [ ] Test catalog #1, #2 xanh, và phần DB của test #6.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 7 (cần guard ADMIN).
**Files:** `V6__venue.sql`, `shared/*Tag.java`, `catalog/CatalogQuery.java`, `catalog/internal/VenueAdminController.java`, test
**Scope:** M.

### Task 18: Adapter Google Places
**Description:** Làm `PlacesGateway` với ba hàm `signals`, `card` và `search`, có field mask, bộ đếm Redis theo SKU, giới hạn lượt, timeout 2 giây và bean rỗng khi tắt. Viết test ArchUnit chặn lưu dữ liệu Google.

**Acceptance criteria:**
- [ ] Test catalog #3, #4, #5 xanh.
- [ ] Gọi thử bằng key thật cho 1 quán. Chạy thủ công, không đưa vào CI.

**Verification:** `./mvnw verify` và một lần gọi tay.
**Dependencies:** Task 17, O2.
**Files:** `catalog/PlacesGateway.java`, `catalog/internal/google/*`, test
**Scope:** M.

### Task 19: Admin thêm quán hàng loạt
**Description:** Làm endpoint `/admin/places/search` và `/admin/venues/bulk`. Web admin gồm màn hình tìm và tick nhiều quán (có chữ "Google Maps"), các tab Nháp, Đã đăng, Cần kiểm tra lại, và form hoàn thiện quán (mức giá, nhãn, link TikTok).

**Acceptance criteria:**
- [ ] Test catalog #6 xanh.
- [ ] Thêm 5 quán thật bằng giao diện, đo thời gian mỗi quán.

**Verification:** `./mvnw verify` và thử thủ công.
**Dependencies:** Task 18.
**Files:** `catalog/internal/BulkImportService.java`, `web/src/features/admin/venues/*`
**Scope:** M.

### Task 20: Thẻ quán cho người dùng
**Description:** Làm endpoint `GET /venues/{id}` kết hợp dữ liệu riêng với `card()` của Google, kèm attribution. Web hiện thẻ quán và video TikTok qua oEmbed, chỉ tải khi người dùng bấm.

**Acceptance criteria:**
- [ ] Test catalog #7 xanh.
- [ ] Tắt Google thì thẻ quán vẫn hiện đủ dữ liệu riêng.

**Verification:** `./mvnw verify` và thử thủ công.
**Dependencies:** Task 18.
**Files:** `catalog/internal/VenueController.java`, `web/src/features/catalog/VenueCard.tsx`
**Scope:** S.

### Task 21: TMDB, phim, suất chiếu
**Description:** Tạo bảng `movie` và `showtime`. Làm job đồng bộ hằng tuần, API admin phim và suất chiếu, `copy-week`, `GET /movies` và `MovieQuery`. Web admin có lưới suất chiếu theo tuần.

**Acceptance criteria:**
- [ ] Test catalog #8, #9, #10 xanh.
- [ ] Nhập được lịch chiếu một tuần thật trong dưới 15 phút.

**Verification:** `./mvnw verify` và thử thủ công.
**Dependencies:** Task 17, O7.
**Files:** `V7__movie.sql`, `catalog/internal/tmdb/*`, `catalog/internal/ShowtimeAdminController.java`, `web/src/features/admin/showtimes/*`
**Scope:** M.

- [ ] **O8.** Chủ dự án nhập 50 quán và lịch chiếu tuần hiện tại, ghi lại tổng thời gian. Mục tiêu dưới 1,5 giờ.

### Checkpoint 4 (cuối tuần 6)
- [ ] Mọi test của catalog xanh. Danh mục có ít nhất 50 quán đã đăng.
- [ ] Chủ dự án duyệt.

---

## Phase 5: budget + preference

### Task 22: Mục con và ngân sách tháng
**Description:** Tạo bảng `budget_category` và `budget_amount`. Tạo mục mặc định một cách lười theo mẫu của loại quỹ. Làm API `budgets/{month}` có chép số tháng trước. Nghe event `FundDeleted`. Làm `BudgetQuery.suggestibleRemaining`.

**Acceptance criteria:**
- [ ] Test budget #1, #2, #12 xanh, và test #4 cho các endpoint đã có.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 15.
**Files:** `V8__budget.sql`, `budget/BudgetQuery.java`, `budget/internal/*`, test
**Scope:** M.

### Task 23: Khoản chi
**Description:** Tạo bảng `expense`, ghi chú mã hóa. Kiểm tra đồng ý `SPENDING_LOG`. Áp quy tắc truy cập cho quỹ ACTIVE và CLOSED. Làm `DELETE /expenses/mine`, `visitedVenueIds`, `PersonalDataHandler` và `summary`.

**Acceptance criteria:**
- [ ] Test budget #3–#11, #13 xanh.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 22, 10, 17.
**Files:** `V9__expense.sql`, `budget/internal/ExpenseService.java`, `budget/internal/ExpenseController.java`, `budget/internal/BudgetDataHandler.java`, test
**Scope:** M. Dùng skill `security-and-hardening`.

### Task 24: Web: ngân sách và ghi chi
**Description:** Làm bottom sheet ghi nhanh (bàn phím nhập theo nghìn, chip mục, nút Lưu), màn hình ngân sách tháng, đặt ngân sách ("Giống tháng trước"), tổng kết tháng và lưu trữ quỹ đã đóng.

**Acceptance criteria:**
- [ ] Ba người thử ghi một khoản chi: tối đa 3 lần chạm, thời gian trung vị dưới 10 giây. Ghi kết quả vào `docs/measurements.md`.

**Verification:** Lệnh web chung và đo thủ công.
**Dependencies:** Task 23, 16.
**Files:** `web/src/features/budget/*`, `docs/measurements.md`
**Scope:** M.

### Task 25: preference
**Description:** Tạo bảng `preference`, bộ lọc món ăn mã hóa. Làm API, `PreferenceQuery.forAccounts` có gộp cho hai người, nghe `ConsentWithdrawn` và cài handler. Web làm màn hình "Bạn thích gì?", "Bộ lọc món ăn" và "Sở thích của bạn".

**Acceptance criteria:**
- [ ] Test preference #1–#7 xanh.

**Verification:** `./mvnw verify` và lệnh web chung.
**Dependencies:** Task 10, 17, 12.
**Files:** `V10__preference.sql`, `preference/*`, `web/src/features/preference/*`
**Scope:** M.

### Checkpoint 5 (cuối tuần 8)
- [ ] Mọi test của budget và preference xanh. Test quét xóa tài khoản vẫn xanh với các bảng mới.
- [ ] Chủ dự án duyệt.

---

## Phase 6: recommendation

### Task 26: Chấm điểm
**Description:** Viết các hàm thuần, không dùng Spring: Bayes, `q`, `d`, `p`, `n` và trọng số theo chế độ lấy từ config.

**Acceptance criteria:**
- [ ] Test recommendation #1 xanh, cùng unit test cho từng thành phần kể cả các trường hợp biên (không có sao, không có sở thích).

**Verification:** `./mvnw test -Dtest='Scoring*'`.
**Dependencies:** Task 17.
**Files:** `recommendation/internal/Scoring.java`, `ScoringTest.java`
**Scope:** S. Chủ dự án tự viết hoặc đọc kỹ task này để giải thích được khi phỏng vấn.

### Task 27: Ghép kế hoạch
**Description:** Viết hàm thuần cho 5 mẫu kế hoạch: lấy 3 quán tốt nhất mỗi vị trí, áp ràng buộc ngân sách theo từng mục, chọn tham lam không lặp quán, xếp ổn định theo `venueId`.

**Acceptance criteria:**
- [ ] Test recommendation #2 (1.000 bộ ngẫu nhiên), #11, #12 xanh.

**Verification:** `./mvnw test -Dtest='PlanCombiner*'`.
**Dependencies:** Task 26.
**Files:** `recommendation/internal/PlanCombiner.java`, `PlanCombinerTest.java`
**Scope:** S. Chủ dự án tự viết hoặc đọc kỹ.

### Task 28: Pipeline gợi ý
**Description:** Viết `RecommendationService` nối các bước 0–7: gọi Google song song bằng virtual threads, giới hạn 3 lần có Google mỗi ngày bằng Redis, sinh cảnh báo và lý do. Làm endpoint `POST /recommendations`.

**Acceptance criteria:**
- [ ] Test recommendation #3–#10, #13 xanh.

**Verification:** `./mvnw verify`.
**Dependencies:** Task 27, 23, 25, 21.
**Files:** `recommendation/internal/RecommendationService.java`, `recommendation/internal/RecommendationController.java`, `ReasonWriter.java`, test
**Scope:** M.

### Task 29: Web: trang chủ và thẻ kế hoạch
**Description:** Trang chủ có chọn đi một mình hoặc đi cùng người yêu, chip thời điểm và công tắc "Trời mưa". Làm thẻ kế hoạch, các nút "Xem quán", "Ghi lại sau khi đi" và "Gợi ý khác", cùng màn hình khi không có kế hoạch nào.

**Acceptance criteria:**
- [ ] Vòng lõi chạy được trên staging: đặt ngân sách → nhận gợi ý → ghi lại từ thẻ kế hoạch → số còn lại giảm.
- [ ] Đo p95 trên staging (dưới 2 giây có Google, dưới 300 ms không có), ghi vào `docs/measurements.md`.

**Verification:** Thử thủ công và đo.
**Dependencies:** Task 28, 24, 20.
**Files:** `web/src/features/recommendation/*`, `web/src/pages/home.tsx`
**Scope:** M.

### Checkpoint 6 (cuối tuần 10)
- [ ] Mọi test xanh. Vòng lõi chạy từ đầu đến cuối với hai tài khoản Cặp đôi.
- [ ] Hai người thử nói lý do gợi ý dễ hiểu (ghi lại nguyên văn).
- [ ] Chủ dự án duyệt.

---

## Phase 7: Ra mắt

### Task 30: VPS Việt Nam + deploy tự động + backup
**Description:** Thuê VPS Việt Nam 2GB. Thêm Caddy vào `compose.prod.yml` để có HTTPS tự động. GitHub Actions deploy qua SSH khi merge vào `main`. Backup Postgres bằng `pg_dump` hằng ngày, mã hóa, giữ 7 ngày. Log tự xóa sau 30 ngày. JVM chạy với `-Xmx512m`.

**Acceptance criteria:**
- [ ] HTTPS hoạt động trên tên miền thật. Merge vào `main` thì tự deploy.
- [ ] Khôi phục được một bản backup trên máy khác.
- [ ] Chi phí dưới 300.000đ/tháng.

**Verification:** Thử khôi phục backup và kiểm tra hóa đơn.
**Dependencies:** Checkpoint 6.
**Files:** `compose.prod.yml`, `Caddyfile`, `.github/workflows/deploy.yml`, `ops/backup.sh`, `docs/ops.md`
**Scope:** M. Dùng skill `ci-cd-and-automation` và `shipping-and-launch`.

### Task 31: Rà bảo mật và observability
**Description:** Chạy skill `security-and-hardening` trên toàn bộ code: kiểm tra OWASP, dependency audit, header bảo mật. Thêm metric Actuator cho số lượt Google, thời gian gợi ý và lỗi. Kiểm tra log trên VPS không có số tiền hay email.

**Acceptance criteria:**
- [ ] Mọi vấn đề nghiêm trọng hoặc cao đã sửa hoặc có lý do ghi lại.
- [ ] `/actuator/metrics` có `plandi.google.calls` và `plandi.recommendation.duration`.

**Verification:** Báo cáo rà soát trong `docs/security-review.md`.
**Dependencies:** Task 30.
**Files:** `docs/security-review.md`, các file cần sửa
**Scope:** M.

- [ ] **O9.** Mời 5 cặp đầu tiên, nói rõ đây là dự án cá nhân và chỉ mời người tự nguyện.

### Checkpoint 7: V1 xong
- [ ] Đạt đủ 10 tiêu chí Success Criteria trong `SPEC.md`.
- [ ] README có link demo, sơ đồ module, cách chạy và link tới các ADR.
