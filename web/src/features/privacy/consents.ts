import { api } from '../../lib/api'

export type Purpose = 'TERMS' | 'SPENDING_LOG' | 'PERSONALIZATION' | 'FOOD_FILTERS'
export type Consents = { policyVersion: string; consents: Record<Purpose, boolean> }

export const fetchConsents = () => api.get<Consents>('/consents')
export const saveConsent = (purpose: Purpose, granted: boolean) => api.put(`/consents/${purpose}`, { granted })

// Mỗi mục đích một câu nói rõ app dùng dữ liệu để làm gì (NĐ 356/2025, spec/SPEC-privacy.md).
export const OPTIONAL_PURPOSES: { purpose: Purpose; title: string; description: string }[] = [
  {
    purpose: 'SPENDING_LOG',
    title: 'Lưu chi tiêu',
    description: 'Ghi lại các khoản đã tiêu để biết còn bao nhiêu và gợi ý vừa túi tiền.',
  },
  {
    purpose: 'PERSONALIZATION',
    title: 'Cá nhân hóa gợi ý',
    description: 'Dùng sở thích và những chỗ bạn đã đi để gợi ý hợp ý hơn.',
  },
  {
    purpose: 'FOOD_FILTERS',
    title: 'Bộ lọc món ăn',
    description: 'Nhớ món bạn không ăn được, như ăn chay hay tránh hải sản. App không hỏi lý do.',
  },
]
