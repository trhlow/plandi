import { describe, expect, it } from 'vitest'
import { formatVnd, parseThousands } from './money'

describe('formatVnd', () => {
  it('nhóm hàng nghìn bằng dấu chấm và thêm đ', () => {
    expect(formatVnd(180000)).toBe('180.000đ')
  })

  it('hiện số 0 và số âm', () => {
    expect(formatVnd(0)).toBe('0đ')
    expect(formatVnd(-50000)).toBe('-50.000đ')
  })
})

describe('parseThousands', () => {
  it('nhập theo nghìn: 180 thành 180000', () => {
    expect(parseThousands('180')).toBe(180000)
  })

  it('bỏ dấu chấm và khoảng trắng người dùng gõ', () => {
    expect(parseThousands(' 1.250 ')).toBe(1250000)
  })

  it('trả null khi rỗng hoặc không phải số nguyên dương', () => {
    expect(parseThousands('')).toBeNull()
    expect(parseThousands('abc')).toBeNull()
    expect(parseThousands('-5')).toBeNull()
    expect(parseThousands('1,5')).toBeNull()
  })
})
