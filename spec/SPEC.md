# Spec: Plandi (phần chung) — V1

Status: approved (21/11/2026)

Phần chung cho mọi module. Spec từng module (`spec/SPEC-<id>.md`) chỉ ghi phần riêng và kế thừa toàn bộ file này. Danh sách module và thứ tự xây ở [capability-map.md](capability-map.md). Nguồn: [tài liệu nghiên cứu](https://claude.ai/artifact/GpXGUfRAYgNu3gKYXwHpNn) rev 37.

## Objective

Plandi giúp người dùng ở TP Trà Vinh, đi một mình hoặc theo cặp, đặt ngân sách đi chơi theo tháng. App gợi ý 2–3 kế hoạch cuối tuần vừa với số tiền còn lại.

Vòng lõi: chọn chế độ (Cá nhân / Cặp đôi + mời người yêu) → đặt ngân sách → nhận gợi ý → đi → ghi chi → xem tổng tháng.

Người dùng: sinh viên và người đi làm trẻ ở TP Trà Vinh, ngân sách đi chơi từ vài trăm nghìn đến vài triệu mỗi tháng.

Hai mục tiêu:
1. Có người dùng thật: 15–20 cặp hoặc người dùng cá nhân.
2. Là dự án portfolio backend khi ứng tuyển vị trí intern/fresher Java.

Làm một mình, 12–15 giờ/tuần, trong 12 tuần.

## Tech Stack

| Lớp | Lựa chọn |
|---|---|
| Backend | Java 21, Spring Boot 4.x (bản ổn định mới nhất lúc scaffold), Maven wrapper |
| Ranh giới module | Spring Modulith (kiểm tra bằng test, không cần tự viết rule ArchUnit) |
| Dữ liệu | PostgreSQL 17, Flyway, Spring Data JPA |
| Cache, đếm lượt | Redis 7 |
| Việc định kỳ | Spring Scheduler (đồng bộ TMDB hằng tuần) |
| Bảo mật | Spring Security, phiên bằng cookie httpOnly; web và API cùng domain |
| Frontend | React + TypeScript, Vite, vite-plugin-pwa, npm |
| Test | JUnit 5, AssertJ, Testcontainers (Postgres, Redis); Vitest cho web |
| Triển khai | Docker Compose, GitHub Actions, VPS có HTTPS (Caddy tự lấy chứng chỉ) |
| Giám sát | Spring Actuator (health, metrics); số lượt gọi Google đếm trong Redis; JVM `-Xmx512m` để vừa VPS 2GB |

## Commands

```
# Hạ tầng local
docker compose up -d postgres redis

# Backend (chạy trong backend/)
./mvnw -q -DskipTests package && java -jar target/plandi-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
#   (không dùng spring-boot:run: trên Windows nó làm hỏng classpath khi đường dẫn có dấu tiếng Việt)
./mvnw test                  # chỉ unit test, không cần Docker
./mvnw verify                # unit + integration (Testcontainers, cần Docker) + kiểm tra module
./mvnw spotless:apply        # format

# Web (chạy trong web/)
npm ci
npm run dev
npm run lint
npm test
npm run build

# Production (trên VPS)
docker compose -f compose.prod.yml up -d
```

## Project Structure

```
backend/
  src/main/java/com/plandi/
    <module>/              → API public của module: service, DTO (record), event
    <module>/internal/     → entity, repository, controller, adapter; module khác không được import
    shared/                → chỉ những thứ thật sự dùng chung (Money helper, ProblemDetail, security config)
  src/main/resources/db/migration/
    V<n>__<module>_<mô_tả>.sql
  src/test/java/com/plandi/<module>/   → test đặt theo cấu trúc package giống code
web/
  src/features/<module>/   → màn hình và API client của từng module
  src/lib/                 → fetch wrapper, format tiền
docs/adr/                  → NNNN-<tiêu-đề>.md
spec/                      → capability map, SPEC.md, SPEC-<module>.md
compose.yaml               → postgres, redis cho local
compose.prod.yml           → app, postgres, redis, caddy
.github/workflows/ci.yml
```

## Code Style

```java
// budget/BudgetService.java: API public của module budget
@Service
public class BudgetService {
    private final FundAccess fundAccess;      // API public của module fund
    private final ExpenseRepository expenses;

    public BudgetService(FundAccess fundAccess, ExpenseRepository expenses) {
        this.fundAccess = fundAccess;
        this.expenses = expenses;
    }

    public long remainingVnd(long accountId, long fundId, long categoryId, YearMonth month) {
        fundAccess.requireMember(accountId, fundId);   // ném FundNotFoundException → 404, không lộ là quỹ có tồn tại
        long spentVnd = expenses.sumAmountVnd(fundId, categoryId, month.atDay(1), month.atEndOfMonth());
        return budgetOf(fundId, categoryId, month) - spentVnd;
    }
}
```

Quy ước:
- **Đặt tên:** code, tên bảng và tên cột viết tiếng Anh. UI, tài liệu, ADR và commit message viết tiếng Việt.
- **Chữ "quỹ":** giao diện không bao giờ dùng chữ "quỹ", vì người dùng dễ hiểu là app giữ tiền. Dùng "Ngân sách cá nhân" và "Ngân sách chung". Code và spec vẫn gọi là `fund`.
- **Tiền:** dùng `long`, tên biến có hậu tố `Vnd`. Cột DB là `BIGINT`, tên cột có hậu tố `_vnd`. Không dùng `double` hay `BigDecimal`.
- **Truy vấn theo quỹ:** truy vấn nào đụng dữ liệu của quỹ cũng nhận `fundId`. `fundId` phải đã qua `FundAccess.requireMember`. Không bao giờ tin `fundId` hay `accountId` lấy thẳng từ request.
- **DTO:** dùng `record`. Không dùng Lombok. Không trả entity ra ngoài API.
- **REST:** đường dẫn `/api/v1/<tài-nguyên>`. Lỗi trả về theo `ProblemDetail` (RFC 9457) có sẵn trong Spring.
- **Log:** không ghi số tiền, ghi chú, sở thích hay email. Chỉ ghi id và mã lỗi.
- **Migration:** không sửa migration đã chạy, muốn đổi thì thêm migration mới.
- **Cột trỏ tới tài khoản:** tên cột luôn kết thúc bằng `account_id` (`account_id`, `created_by_account_id`…). Test xóa tài khoản của privacy dựa vào quy ước này để tìm mọi dòng còn sót.

## Testing Strategy

| Mức | Dùng cho | Công cụ |
|---|---|---|
| Unit | Tính điểm Bayes, chấm điểm, ghép kế hoạch, tính số dư: các hàm thuần, chạy trên dữ liệu cố định | JUnit 5, AssertJ, không khởi động Spring |
| Integration | Repository, API, migration, phân quyền | `@SpringBootTest` + Testcontainers Postgres/Redis |
| Truy cập chéo (bắt buộc) | Mọi endpoint đụng dữ liệu quỹ: người ngoài quỹ nhận 404; người yêu không đọc được quỹ Cá nhân của nhau | Integration |
| Ranh giới module | Không module nào import `internal` của module khác, không có vòng phụ thuộc | `ApplicationModules.of(PlandiApplication.class).verify()` |
| Adapter ngoài | Test bằng fake cài cùng interface với adapter. Gọi API thật chỉ làm bằng tay, không chạy trong CI | JUnit 5 |
| Web | Logic format, validate form | Vitest |

Không đặt ngưỡng % coverage. Thay vào đó, mỗi module có danh sách test bắt buộc trong spec của module. CI chạy `./mvnw verify`, `npm run lint`, `npm test` và `npm run build` trên mỗi PR vào `main`.

## Boundaries

- **Always**
  - Chạy `./mvnw verify` trước khi commit code backend.
  - Mỗi endpoint mới có test truy cập chéo.
  - Mọi thay đổi schema đi qua Flyway.
  - Hiện "Google Maps" ở mọi chỗ có dữ liệu Places. Ghi nguồn TMDB ở trang Giới thiệu.
  - Lưu secret trong `.env` (đã gitignore). Commit `.env.example`.
  - Ghi ADR cho các quyết định lớn.
- **Ask first**
  - Thêm dependency.
  - Thêm trường dữ liệu cá nhân mới.
  - Thêm nguồn hoặc API ngoài.
  - Đổi trọng số gợi ý.
  - Đổi CI hoặc cấu hình deploy.
  - Đổi ranh giới module trong capability map.
- **Never**
  - Lưu nội dung Google ngoài `place_id` (tọa độ chỉ cache tối đa 30 ngày).
  - Thu GPS, lý do sức khỏe hay tin nhắn.
  - Scrape TikTok, Facebook hay web rạp.
  - Gửi dữ liệu người dùng cho LLM.
  - Dùng dữ liệu người dùng thật khi phát triển.
  - Giữ hoặc chuyển tiền.
  - Sửa migration đã chạy.
  - Xóa hoặc skip test đang fail khi chưa được đồng ý.
  - Commit API key.

## Success Criteria

V1 xong khi tất cả điều dưới đây đúng trên bản deploy thật:

1. **Chế độ Cá nhân:** người dùng mới đăng ký, đặt ngân sách cho mục con và nhận 2–3 kế hoạch. Tổng tiền mỗi kế hoạch không vượt số dư của mục đó.
2. **Chế độ Cặp đôi:** người dùng tạo link mời. Người yêu mở link trên trình duyệt điện thoại, đăng ký và vào quỹ mà không cần cài app. Link hết hạn thì bị từ chối.
3. **Ghi chi:** mở form → nhập số tiền → lưu, tối đa 3 lần chạm. Màn hình số dư cập nhật ngay.
4. **Tách quỹ:** có test chứng minh người yêu không đọc được chi tiêu của quỹ Cá nhân, và gợi ý ở chế độ Cặp đôi không dùng chi tiêu của quỹ Cá nhân.
5. **Gợi ý khi tắt Google:** tắt adapter Google, gợi ý vẫn trả về kế hoạch từ danh mục riêng.
6. **Hạn mức Google:** mỗi lần gợi ý gọi Google tối đa 10 lần. Redis đếm số lượt theo tháng, quá 900 lượt Enterprise thì tự tắt bước lấy số sao.
7. **Quyền riêng tư:** có màn hình đồng ý cho 3 mục đích. Xuất toàn bộ dữ liệu ra JSON. Xóa tài khoản thì DB không còn dòng nào gắn với tài khoản đó, trừ dữ liệu của quỹ Cặp đôi, phần này xử lý theo `SPEC-fund`.
8. **Dữ liệu:** danh mục có ít nhất 50 quán. Lịch chiếu CGV Vincom Trà Vinh được nhập cho tuần hiện tại.
9. **Vận hành:** CI xanh. Có HTTPS. Chi phí dưới 300.000đ/tháng. Mỗi tuần có ít nhất một bản deploy.
10. **ADR:** có ADR cho 5 quyết định nêu ở mục 8 của tài liệu nghiên cứu.

## Out of Scope (Deferred)

- **Wrapped tổng kết tháng, xuất ảnh story:** chỉ làm nếu còn thời gian sau khi vòng lõi chạy.
- **Nhắc gợi ý cuối tuần:** cần web push, làm sau khi có người dùng.
- **"Muốn quay lại không?" và giá trung bình từ người dùng:** cần có người dùng trước (giai đoạn 2).
- **Mẫu Bạn cùng phòng, VietQR + SePay:** giai đoạn 2.
- **Sticker nhắc nợ, thử thách nhóm, nhập chi tiêu bằng câu tự nhiên, Spring AI:** giai đoạn 2.
- **App native, widget, thành phố thứ hai:** giai đoạn 3.
- **Test E2E trên trình duyệt:** làm một mình, chi phí bảo trì cao so với lợi ích ở V1.
- **Ghi "ai trả", tính ai nợ ai trong ngân sách chung:** chủ dự án quyết định không làm. App không xử lý chuyện tiền giữa hai người.
- **Prometheus + Grafana:** tốn vài trăm MB RAM trên VPS 2GB. V1 dùng Actuator, thêm dashboard khi có nhu cầu thật.

## Decisions (21/11/2026)

- **Lọc "đang mở cửa":** lấy `regularOpeningHours` từ Google ở bước 2, trong cùng lượt gọi lấy số sao, không tốn thêm lượt và không lưu lại. Khi Google tắt hoặc hết lượt thì bỏ qua bộ lọc này, và gợi ý kèm dòng "Kiểm tra giờ mở trước khi đi". Quyết định này thay cho quyết định "admin tự nhập giờ mở" trước đó, để giảm việc nhập tay.
- **Nhập danh mục:** admin thêm quán hàng loạt từ kết quả tìm trên Google, chỉ lưu `place_id`. Admin tự gõ tên ngắn, chọn mức giá có sẵn thay vì gõ số, và tick nhãn.
- **Lọc "hợp thời tiết":** người dùng tự bật "trời mưa", khi đó chỉ lấy quán có nhãn "trong nhà". Không gọi API thời tiết.
- **Link mời:** hết hạn sau 7 ngày, chỉ dùng một lần.
- **Build tool:** Maven.
- **Hạ tầng:** VPS đặt tại Việt Nam, để dữ liệu người dùng nằm trong nước. Không dùng dịch vụ quản lý sẵn ở nước ngoài (Supabase, Firebase…) để lưu dữ liệu người dùng. Giữ spec tự làm đăng nhập.

## Open Questions

1. **Cổng 1 (phỏng vấn 5–8 cặp):** chưa làm. Nếu kết quả trượt thì dự án chuyển sang mẫu Bạn cùng phòng. Phần module không đổi, chỉ đổi mẫu quỹ.
