import { api } from './api'

export type Area = { code: string; name: string; lat: number; lng: number }

export const fetchAreas = () => api.get<Area[]>('/areas')

// Thành phố nhỏ (vài km) nên khoảng cách phẳng là đủ; cos(vĩ độ) bù cho kinh độ co lại.
export function nearestArea(lat: number, lng: number, areas: Area[]): Area | null {
  let best: Area | null = null
  let bestDistance = Infinity
  const k = Math.cos((lat * Math.PI) / 180)
  for (const area of areas) {
    const d = (area.lat - lat) ** 2 + ((area.lng - lng) * k) ** 2
    if (d < bestDistance) {
      best = area
      bestDistance = d
    }
  }
  return best
}

/** Vị trí chỉ dùng ngay trên máy để chọn khu gần nhất, không bao giờ gửi lên server (ADR-0006). */
export function locateNearestArea(areas: Area[]): Promise<Area> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) return reject(new Error('Trình duyệt không hỗ trợ định vị.'))
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const area = nearestArea(pos.coords.latitude, pos.coords.longitude, areas)
        if (area) resolve(area)
        else reject(new Error('Chưa tải được danh sách khu vực.'))
      },
      () => reject(new Error('Không lấy được vị trí. Bạn chọn khu vực bằng tay nhé.')),
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 },
    )
  })
}
