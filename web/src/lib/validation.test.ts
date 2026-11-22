import { describe, expect, it } from 'vitest'
import { emailError, passwordError } from './validation'

describe('passwordError', () => {
  it('chấp nhận mật khẩu từ 8 ký tự, khác email', () => {
    expect(passwordError('matkhau-123', 'an@example.com')).toBeNull()
  })

  it('từ chối mật khẩu ngắn hơn 8 ký tự', () => {
    expect(passwordError('ngan', 'an@example.com')).toMatch(/8 ký tự/)
  })

  it('từ chối mật khẩu trùng email, không phân biệt hoa thường và khoảng trắng', () => {
    expect(passwordError(' AN@example.com ', 'an@example.com')).toMatch(/khác email/)
  })

  it('đếm giới hạn 72 theo byte UTF-8 chứ không theo ký tự (giống backend, bcrypt)', () => {
    expect(passwordError('a'.repeat(72), 'an@example.com')).toBeNull()
    // 25 chữ "ệ" = 75 byte UTF-8 dù chỉ 25 ký tự
    expect(passwordError('ệ'.repeat(25), 'an@example.com')).toMatch(/quá dài/)
  })
})

describe('emailError', () => {
  it('chấp nhận email bình thường, bỏ khoảng trắng hai đầu', () => {
    expect(emailError('  an@example.com ')).toBeNull()
  })

  it('từ chối email rỗng hoặc sai dạng', () => {
    expect(emailError('')).not.toBeNull()
    expect(emailError('an.example.com')).not.toBeNull()
    expect(emailError('an@')).not.toBeNull()
  })
})
