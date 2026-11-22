// Kiểm tra sớm trên máy cho người dùng đỡ chờ; server vẫn kiểm tra lại (spec/SPEC-account.md).

export function emailError(email: string): string | null {
  const value = email.trim()
  if (!value) return 'Nhập email của bạn.'
  if (value.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Email chưa đúng dạng.'
  return null
}

export function passwordError(password: string, email: string): string | null {
  if (password.length < 8) return 'Mật khẩu cần ít nhất 8 ký tự.'
  // bcrypt chỉ nhận 72 byte; chữ có dấu tốn 2–3 byte mỗi ký tự.
  if (new TextEncoder().encode(password).length > 72) return 'Mật khẩu quá dài, hãy rút ngắn lại.'
  if (password.trim().toLowerCase() === email.trim().toLowerCase()) return 'Mật khẩu phải khác email.'
  return null
}
