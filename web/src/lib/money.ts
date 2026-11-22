// Tiền là số nguyên VND (backend lưu BIGINT), không có phần lẻ.
const vnd = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })

export function formatVnd(amountVnd: number): string {
  return `${vnd.format(amountVnd)}đ`
}

// Bàn phím ghi chi nhập theo nghìn: gõ "180" nghĩa là 180.000đ.
export function parseThousands(input: string): number | null {
  const digits = input.replace(/[.\s]/g, '')
  if (!/^\d+$/.test(digits)) return null
  return Number(digits) * 1000
}
