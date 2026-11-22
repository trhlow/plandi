import type { Me } from '../../lib/useSession'
import { Card, Intro, Link, Screen } from '../../ui/layout'

// Trang chủ tạm cho đến Task 29 (gợi ý kế hoạch cuối tuần).
export function HomePage({ me }: { me: Me }) {
  return (
    <Screen>
      <Intro title={`Chào ${me.displayName}`} subtitle="Cuối tuần này đi đâu, ăn gì?" />
      <Card>
        <h2>Gợi ý cuối tuần sắp có</h2>
        <p className="muted">Plandi đang chuẩn bị danh sách quán ở Trà Vinh. Trong lúc chờ, bạn chỉnh hồ sơ và quyền riêng tư nhé.</p>
      </Card>
      <Link to="/ho-so">Mở hồ sơ</Link>
    </Screen>
  )
}
