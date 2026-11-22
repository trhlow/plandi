import { describe, expect, it } from 'vitest'
import { nearestArea, type Area } from './areas'

const areas: Area[] = [
  { code: 'TRUNG_TAM', name: 'Trung tâm', lat: 9.9356, lng: 106.3397 },
  { code: 'AO_BA_OM', name: 'Ao Bà Om', lat: 9.9176, lng: 106.3041 },
  { code: 'LONG_DUC', name: 'Long Đức', lat: 9.9817, lng: 106.345 },
]

describe('nearestArea', () => {
  it('chọn khu có tâm gần vị trí nhất', () => {
    // Cổng chùa Âng, sát Ao Bà Om
    expect(nearestArea(9.918, 106.306, areas)?.code).toBe('AO_BA_OM')
    // Gần chợ Trà Vinh
    expect(nearestArea(9.9353, 106.3424, areas)?.code).toBe('TRUNG_TAM')
  })

  it('trả null khi chưa có danh sách khu', () => {
    expect(nearestArea(9.93, 106.34, [])).toBeNull()
  })
})
