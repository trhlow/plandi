# ADR-0006: Vị trí chỉ dùng trên điện thoại để chọn khu vực gần nhất

- Trạng thái: Đã chấp nhận
- Ngày: 22/11/2026
- Sửa một phần quyết định "không dùng GPS" trong tài liệu nghiên cứu (mục 6.1 và 6.2) và `CLAUDE.md`.

## Bối cảnh

- Gợi ý lọc quán theo khu vực (enum `Area`, vài khu vực quen gọi ở TP Trà Vinh). Hiện người dùng phải tự chọn khu vực.
- Chủ dự án muốn app xin quyền vị trí để gợi ý quán gần người dùng mà không bắt họ chọn tay.
- Vị trí xác định qua dịch vụ định vị là dữ liệu nhạy cảm (Nghị định 356/2025, Điều 4 khoản 1 điểm h). Lưu hay xử lý nó trên server kéo theo nghĩa vụ xin đồng ý riêng, phân quyền, bảo mật, xuất và xóa.
- TP Trà Vinh nhỏ. Độ chính xác cấp khu vực là đủ để gợi ý.

## Quyết định

1. **Mặc định:** khi xin gợi ý, người dùng chọn khu vực hoặc mốc quen thuộc từ danh sách (ví dụ "gần Vincom"). Nếu không chọn thì dùng khu vực trong hồ sơ.
2. **Tùy chọn "Dùng vị trí của tôi":** web gọi Geolocation API của trình duyệt. Điện thoại tự tính khu vực có tâm gần nhất, rồi chỉ gửi **mã khu vực** lên server như khi chọn tay.
3. Tọa độ của người dùng **không bao giờ** được gửi lên server, không lưu trong localStorage, không ghi log.
4. Mỗi khu vực trong enum `Area` có tọa độ tâm do Plandi tự đặt (không lấy từ Google). `GET /api/v1/areas` trả kèm tọa độ này để web tính khoảng cách.

## Phương án khác

### Gửi tọa độ lên server để tính khoảng cách chính xác
- Ưu: xếp quán theo khoảng cách thật.
- Nhược: server xử lý dữ liệu nhạy cảm; phải thêm mục đồng ý riêng, mã hóa, xuất và xóa; lợi ích nhỏ vì thành phố nhỏ.
- Bỏ.

### Người không cho vị trí thì dùng vị trí của người dùng khác (ví dụ người yêu)
- Bỏ: dùng dữ liệu nhạy cảm sai mục đích người kia đã đồng ý, và để lộ vị trí người yêu, trái quy tắc riêng tư giữa hai người.

### Gõ tự do tên địa điểm rồi nhờ Google tìm
- Bỏ khỏi V1: mỗi lần gõ là một lượt gọi Google có tính phí, kết quả bị giới hạn lưu trữ. Chọn từ danh sách mốc nhanh hơn và miễn phí.

## Hệ quả

- Server không nhận vị trí, nên không cần thêm mục đồng ý, cột mã hóa hay phần xuất và xóa cho vị trí.
- Trình duyệt vẫn hiện hộp hỏi quyền. Trang Chính sách phải nói rõ vị trí chỉ dùng trên điện thoại để chọn khu vực và không được gửi đi.
- Geolocation API chỉ chạy qua HTTPS, đã có theo ADR-0002.
- Cần chủ dự án đưa danh sách khu vực hoặc mốc kèm vị trí đại khái (O4) trước khi viết enum `Area`.
- Chưa tra kỹ việc xử lý vị trí ngay trên thiết bị có bị coi là "xử lý dữ liệu cá nhân" của Plandi theo Luật 91/2025 hay không. Đối chiếu lại ở bước O5, trước khi ra mắt.
