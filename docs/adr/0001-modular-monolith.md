# ADR-0001: Modular monolith thay cho microservices

- Trạng thái: Đã chấp nhận
- Ngày: 21/11/2026 (chốt trong tài liệu nghiên cứu, mục 8)

## Bối cảnh

- Một người làm, khoảng 12–15 giờ mỗi tuần, trong 12 tuần.
- Người dùng dự kiến vài chục cặp ở một thành phố. Tải nhỏ, một VPS 2GB là đủ.
- Nghiệp vụ có ranh giới rõ: tài khoản, quỹ, ngân sách, danh mục, sở thích, gợi ý, quyền riêng tư. Quy tắc riêng tư giữa hai người yêu đòi hỏi mỗi truy vấn đều lọc theo quỹ, nên ranh giới phải được kiểm tra chứ không chỉ quy ước miệng.
- Đây cũng là dự án portfolio backend: người phỏng vấn sẽ hỏi vì sao chọn kiến trúc này.

## Quyết định

Một Spring Boot app, chia thành 7 module theo package `com.plandi.<module>` cộng `shared`. Dùng Spring Modulith:

- Mỗi module khai `allowedDependencies` trong `package-info.java`, theo `spec/capability-map.md`.
- `ModularityTest` fail khi một module gọi module không được phép, hoặc đụng vào package `internal` của module khác.
- Module nói chuyện với nhau qua API public ở package gốc và qua event (ví dụ `AccountRegistered`, `FundDeleted`).

Một database PostgreSQL, mỗi module sở hữu bảng của mình. Không module nào đọc thẳng bảng của module khác.

## Phương án khác

### Microservices
- Ưu: mỗi phần deploy và mở rộng riêng; nghe "xịn" trên CV.
- Nhược: phải lo service discovery, gọi mạng giữa các service, giao dịch phân tán (ví dụ xóa tài khoản phải xóa ở mọi nơi trong một lần), nhiều container trên một VPS nhỏ.
- Bỏ: chi phí vận hành vượt xa lợi ích với một người làm và vài chục người dùng. Xóa tài khoản trong một transaction là yêu cầu pháp lý, monolith làm việc này dễ hơn nhiều.

### Monolith không chia module
- Ưu: nhanh nhất lúc đầu.
- Nhược: không có gì ngăn module gợi ý đọc thẳng chi tiêu quỹ Cá nhân, tức là lỗi riêng tư chỉ bị phát hiện khi đã xảy ra.
- Bỏ: ranh giới module là một phần của thiết kế quyền riêng tư.

## Hệ quả

- Deploy một jar, một container app. Giao dịch nhiều bảng dùng `@Transactional` bình thường.
- Ranh giới module được kiểm tra trong `./mvnw verify`, nên CI chặn vi phạm.
- Nếu sau này một phần cần tách ra (ví dụ job đồng bộ TMDB), ranh giới đã có sẵn để tách.
- Phải giữ kỷ luật: muốn thêm phụ thuộc giữa module thì sửa capability map trước (spec ghi "Ask first").
