import { Screen } from '../../ui/layout'

// Bản nháp do Claude soạn theo spec (spec/SPEC-privacy.md, ADR-0006), không phải tư vấn pháp lý.
// Sửa nội dung ở đây thì tăng POLICY_VERSION trong backend (ConsentService) để người dùng đồng ý lại.
export function TermsPage() {
  return (
    <Screen title="Điều khoản và Chính sách" back="/">
      <article className="prose">
        <p className="caption muted">Phiên bản 2026-11</p>

        <h2>Plandi là gì</h2>
        <p>
          Plandi giúp bạn, một mình hoặc cùng người yêu, đặt ngân sách đi chơi theo tháng và gợi ý kế hoạch cuối tuần ở
          TP Trà Vinh. Đây là dự án cá nhân, chỉ mời người tự nguyện dùng thử. Plandi không giữ, không chuyển tiền và
          không bán vé.
        </p>

        <h2>Dữ liệu Plandi lưu</h2>
        <ul>
          <li>Tài khoản: email, mật khẩu (chỉ lưu dạng đã băm), tên hiển thị, khu vực bạn chọn.</li>
          <li>Những gì bạn cho phép ở màn hình đồng ý: khoản chi tiêu, sở thích, bộ lọc món ăn.</li>
          <li>Lịch sử đồng ý: bạn đã bật hay tắt từng mục lúc nào, làm bằng chứng theo Nghị định 356/2025.</li>
        </ul>

        <h2>Plandi không thu</h2>
        <ul>
          <li>
            Vị trí GPS. Nút "Dùng vị trí của tôi" chỉ dùng vị trí ngay trên điện thoại để chọn khu gần nhất; vị trí
            không được gửi lên server.
          </li>
          <li>Dữ liệu sức khỏe. Bộ lọc món ăn là nhãn bạn tự chọn, app không hỏi lý do.</li>
          <li>Tin nhắn của bạn, hay dữ liệu từ ngân hàng, ví điện tử, mạng xã hội.</li>
        </ul>

        <h2>Cặp đôi</h2>
        <p>
          Chi tiêu trong ngân sách cá nhân không bao giờ hiện cho người yêu. Gợi ý ở chế độ cặp đôi chỉ dùng dữ liệu
          của ngân sách chung.
        </p>

        <h2>Bảo vệ dữ liệu</h2>
        <ul>
          <li>Ghi chú và bộ lọc món ăn được mã hóa trong cơ sở dữ liệu.</li>
          <li>Nhật ký hệ thống không ghi email, số tiền hay ghi chú, và tự xóa sau 30 ngày.</li>
          <li>Bản sao lưu được mã hóa và giữ tối đa 7 ngày.</li>
        </ul>

        <h2>Quyền của bạn</h2>
        <ul>
          <li>Bật hoặc tắt từng mục đích bất cứ lúc nào trong Hồ sơ → Quyền riêng tư.</li>
          <li>Tải toàn bộ dữ liệu của bạn dưới dạng file JSON.</li>
          <li>
            Xóa tài khoản: mọi dữ liệu bị xóa ngay. Bản sao lưu cũ còn chứa dữ liệu tối đa 7 ngày rồi tự xóa.
          </li>
        </ul>

        <h2>Nguồn dữ liệu bên ngoài</h2>
        <p>
          Thông tin quán (số sao, giờ mở cửa) lấy từ Google Maps lúc hiển thị và không được Plandi lưu lại. Thông tin
          phim lấy từ TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.
        </p>

        <h2>Độ tuổi</h2>
        <p>Plandi dành cho người từ 18 tuổi trở lên.</p>

        <h2>Thay đổi điều khoản</h2>
        <p>Khi điều khoản thay đổi, bạn sẽ được hỏi đồng ý lại trước khi dùng tiếp.</p>
      </article>
    </Screen>
  )
}
