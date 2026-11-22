# ADR-0004: Không dùng LLM trong MVP

- Trạng thái: Đã chấp nhận
- Ngày: 21/11/2026 (chốt trong tài liệu nghiên cứu, mục 5 và 8)

## Bối cảnh

- Gợi ý cần đúng ba thứ: vừa ngân sách, đang mở cửa, hợp sở thích. Cả ba tính được bằng lọc và công thức.
- Dữ liệu người dùng (chi tiêu, sở thích, ai hẹn hò với ai) được xử lý như dữ liệu nhạy cảm theo Nghị định 356/2025. Gửi dữ liệu này cho một LLM ở nước ngoài kéo theo câu hỏi chuyển dữ liệu ra nước ngoài.
- Người dùng phải hiểu vì sao được gợi ý một quán, và chủ dự án phải giải thích được thuật toán khi phỏng vấn.
- LLM có thể bịa ra quán hoặc giá không có thật.

## Quyết định

Gợi ý là một công thức chấm điểm giải thích được: trung bình Bayes cho chất lượng, cộng điểm hợp hẹn hò, hợp sở thích và chỗ mới, với trọng số lấy từ config. Lý do hiển thị cạnh mỗi gợi ý được ghép từ mẫu câu, ví dụ "4.6★ từ 400 đánh giá · khoảng 180k · yên tĩnh".

Không có dependency LLM nào trong V1. Spring AI để giai đoạn 2.

## Phương án khác

### Dùng LLM chọn quán hoặc viết kế hoạch
- Ưu: lời gợi ý tự nhiên hơn; "có AI" hấp dẫn trên CV.
- Nhược: gửi dữ liệu nhạy cảm ra nước ngoài; có thể bịa quán hoặc giá; tốn tiền mỗi lần gọi; khó test vì kết quả không cố định.
- Bỏ khỏi V1.

### Máy học (học trọng số từ hành vi)
- Bỏ: chưa có dữ liệu. Vài chục cặp không đủ để học gì có nghĩa.

## Hệ quả

- Test thuật toán bằng dữ liệu cố định, kết quả lặp lại được.
- Không phải lo chuyển dữ liệu ra nước ngoài.
- Nếu sau này dùng LLM để viết lời gợi ý, nó chỉ được chọn từ danh mục có thật, không được tự nghĩ ra quán hay giá. Việc đó cần một ADR mới.
