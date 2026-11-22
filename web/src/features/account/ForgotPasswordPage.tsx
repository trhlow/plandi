import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import { emailError, passwordError } from '../../lib/validation'
import { Button, Field } from '../../ui/controls'
import { Card, ErrorNotice, Link, Screen } from '../../ui/layout'

// Link Zalo của admin đặt qua biến môi trường lúc build; chưa có thì ẩn nút.
const ADMIN_ZALO = import.meta.env.VITE_ADMIN_ZALO_URL as string | undefined

export function ForgotPasswordPage() {
  const [form, setForm] = useState({ email: '', code: '', newPassword: '' })
  const [touched, setTouched] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const set = (key: keyof typeof form) => (value: string) => setForm((f) => ({ ...f, [key]: value }))
  const errors = {
    email: emailError(form.email),
    code: form.code.trim().length === 8 ? null : 'Mã gồm 8 ký tự.',
    newPassword: passwordError(form.newPassword, form.email),
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setTouched(true)
    if (errors.email || errors.code || errors.newPassword) return
    setBusy(true)
    setError(null)
    try {
      await api.post('/auth/reset-password', { ...form, email: form.email.trim() })
      setDone(true)
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0
      setError(
        status === 429
          ? 'Bạn đã nhập sai quá nhiều lần. Thử lại sau 15 phút.'
          : status === 400
            ? (err as ApiError).problem?.detail ?? 'Mã không đúng hoặc đã hết hạn.'
            : 'Chưa đặt lại được. Kiểm tra mạng rồi thử lại.',
      )
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <Screen title="Quên mật khẩu" back="/dang-nhap">
        <p className="notice-ok" role="status">Đã đặt lại mật khẩu. Các thiết bị đang đăng nhập đã bị đăng xuất.</p>
        <Link to="/dang-nhap">Đăng nhập bằng mật khẩu mới</Link>
      </Screen>
    )
  }

  return (
    <Screen title="Quên mật khẩu" back="/dang-nhap">
      <Card tone="brand">
        <h2 className="brand-heading">Đặt lại qua admin</h2>
        <p>1. Nhắn admin Plandi qua Zalo, kèm email bạn đã đăng ký.</p>
        <p>2. Admin gửi lại mã 8 ký tự, dùng được trong 1 giờ.</p>
        <p>3. Nhập mã và mật khẩu mới bên dưới.</p>
      </Card>
      {ADMIN_ZALO && (
        <a className="btn btn-secondary btn-link"
          href={ADMIN_ZALO} target="_blank" rel="noreferrer">
          Nhắn admin qua Zalo
        </a>
      )}
      <form className="stack" onSubmit={submit} noValidate>
        <Field label="Email" type="email" autoComplete="email" inputMode="email" value={form.email}
          onChange={(e) => set('email')(e.target.value)} error={touched ? errors.email : null} />
        <Field label="Mã đặt lại" autoComplete="one-time-code" autoCapitalize="characters" maxLength={8}
          value={form.code} onChange={(e) => set('code')(e.target.value)}
          helper="Không phân biệt chữ hoa, chữ thường." error={touched ? errors.code : null} />
        <Field label="Mật khẩu mới" type="password" autoComplete="new-password" value={form.newPassword}
          onChange={(e) => set('newPassword')(e.target.value)}
          helper="Ít nhất 8 ký tự, khác email." error={touched ? errors.newPassword : null} />
        <ErrorNotice message={error} />
        <Button type="submit" busy={busy}>Đặt lại mật khẩu</Button>
      </form>
    </Screen>
  )
}
