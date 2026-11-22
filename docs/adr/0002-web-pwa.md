# ADR-0002: Web responsive/PWA thay cho app native

- Trạng thái: Đã chấp nhận
- Ngày: 21/11/2026 (chốt trong tài liệu nghiên cứu, mục 8)

## Bối cảnh

- Người dùng ở cả Android và iOS. Hai người yêu thường dùng hai hệ điều hành khác nhau.
- Người yêu được mời qua link gửi trong Zalo. Rủi ro lớn của dự án là người yêu không tham gia, nên bước vào app phải ngắn nhất có thể: mở link là dùng được, không bắt cài app.
- Một người làm, trọng tâm là backend. Không có thời gian viết và duy trì hai app native.

## Quyết định

Làm một web app React responsive, cài được thành PWA (Vite + `vite-plugin-pwa`). Cùng một bản code chạy trên:

- Android: Chrome, "Cài đặt ứng dụng".
- iOS: Safari, Chia sẻ → "Thêm vào MH chính".

App chạy qua HTTPS tại tên miền con của `evts.id.vn`, ví dụ `plandi.evts.id.vn`.

## Phương án khác

### App native hoặc đa nền tảng (React Native, Flutter) lên CH Play và App Store
- Ưu: thông báo đẩy ổn định, có widget, cảm giác "app thật".
- Nhược: phải qua xét duyệt cửa hàng, phải trả phí tài khoản nhà phát triển (Google một lần, Apple hằng năm), người yêu phải cài app mới mở được link mời.
- Bỏ khỏi V1: tốn thời gian của phần backend, và làm bước mời người yêu dài hơn. Để giai đoạn sau nếu có người dùng thật.

### Chỉ web, không PWA
- Bỏ: PWA chỉ thêm manifest và service worker, đổi lại có icon trên màn hình chính và mở toàn màn hình.

## Hệ quả

- Bắt buộc có HTTPS và một tên miền cố định, vì PWA và cookie phiên đăng nhập gắn với tên miền.
- Không cần xét duyệt cửa hàng; deploy là người dùng có bản mới.
- Trên iOS, thông báo đẩy chỉ chạy khi người dùng đã thêm app vào màn hình chính. Tính năng "nhắc gợi ý cuối tuần" phải chịu giới hạn này.
- Backend phục vụ luôn `web/dist`, nên web và API cùng tên miền, không phải cấu hình CORS.
