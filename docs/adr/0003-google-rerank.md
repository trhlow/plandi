# ADR-0003: Gọi Google Places theo kiểu "lọc ứng viên rồi xếp hạng lại"

- Trạng thái: Đã chấp nhận
- Ngày: 21/11/2026 (chốt trong tài liệu nghiên cứu, mục 5.3)

## Bối cảnh

- Không có nguồn sẵn nào có giá quán ở Trà Vinh, nên danh mục quán (giá, nhãn, khu vực) do Plandi tự xây.
- Số sao Google giúp xếp hạng, nhưng chính sách Google chỉ cho lưu `place_id`. Tọa độ được cache tối đa 30 ngày. Tên, số sao, giờ mở cửa, ảnh, đánh giá phải lấy lúc hiển thị và không được lưu.
- Số sao, số lượt đánh giá, mức giá và giờ mở cửa thuộc gói Enterprise của Place Details: miễn phí 1.000 lượt mỗi tháng, sau đó $20 cho 1.000 lượt.
- Gọi Google cho cả 100 quán mỗi lần gợi ý: 20 cặp × 4 tuần × 100 quán = 8.000 lượt mỗi tháng, vượt hạn mức miễn phí.

## Quyết định

1. Lọc danh mục riêng (giá, nhãn, khu vực, bộ lọc món ăn) xuống khoảng 10 quán ứng viên.
2. Chỉ gọi Place Details cho 10 quán đó, khi người dùng mở trang gợi ý. Gọi song song, timeout 2 giây.
3. Bỏ quán đang đóng cửa vào giờ dự định, rồi xếp hạng lại bằng trung bình Bayes `v/(v+m)·R + m/(v+m)·C` cộng điểm nhãn, sở thích và chỗ mới.
4. Hiển thị kèm chữ hoặc logo "Google Maps".

Mọi lời gọi Google đi qua adapter `PlacesGateway`, có đếm lượt theo SKU bằng Redis và một bean rỗng khi tắt.

## Phương án khác

### Gọi Google cho toàn bộ danh mục
- Bỏ: khoảng 8.000 lượt mỗi tháng với 20 cặp, vượt hạn mức miễn phí ngay.

### Lưu số sao vào database và cập nhật định kỳ
- Bỏ: vi phạm chính sách Google, chỉ được lưu `place_id`.

### Chỉ hiển thị số sao, xếp hạng hoàn toàn bằng dữ liệu riêng
- Ưu: an toàn nhất về điều khoản.
- Nhược: bỏ mất tín hiệu chất lượng tốt nhất đang có lúc chưa có đánh giá nội bộ.
- Giữ làm phương án dự phòng: nếu đọc kỹ Service Specific Terms mà cách gọi trên bị coi là "tải trước" thì chuyển sang phương án này.

## Hệ quả

- 20 cặp × 4 tuần × 10 quán = 800 lượt mỗi tháng, nằm trong hạn mức miễn phí. 50 cặp khoảng 2.000 lượt, tốn khoảng $20 mỗi tháng.
- Gợi ý vẫn chạy khi Google tắt hoặc hết lượt: bỏ bước lọc giờ mở cửa, quán không có số sao nhận điểm trung tính `Q = C`, và người dùng thấy cảnh báo.
- Cần đặt trần ngân sách trên Google Cloud và giới hạn API key.
- Có test ArchUnit chặn lưu dữ liệu Google ngoài `place_id`.
