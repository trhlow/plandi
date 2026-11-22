import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import { navigate } from '../../lib/router'
import { useSession } from '../../lib/useSession'
import { Button, Field } from '../../ui/controls'
import { ErrorNotice, Intro, Link, Screen } from '../../ui/layout'

export function LoginPage() {
  const { refresh } = useSession()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.post('/auth/login', { email: email.trim(), password })
      await refresh()
      navigate('/')
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0
      setError(
        status === 401
          ? 'Email hoặc mật khẩu không đúng.'
          : status === 429
            ? 'Bạn đã nhập sai quá nhiều lần. Thử lại sau 15 phút.'
            : 'Chưa đăng nhập được. Kiểm tra mạng rồi thử lại.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <Screen>
      <div className="push-down" />
      <Intro title="Chào bạn quay lại" subtitle="Đăng nhập để xem kế hoạch cuối tuần." />
      <form className="stack" onSubmit={submit}>
        <Field label="Email" type="email" autoComplete="email" inputMode="email" required value={email}
          onChange={(e) => setEmail(e.target.value)} />
        <Field label="Mật khẩu" type="password" autoComplete="current-password" required value={password}
          onChange={(e) => setPassword(e.target.value)} />
        <p className="caption right">
          <Link to="/quen-mat-khau">Quên mật khẩu?</Link>
        </p>
        <Button type="submit" busy={busy}>Đăng nhập</Button>
        <ErrorNotice message={error} />
        <p className="center caption">
          <span className="muted">Chưa có tài khoản? </span>
          <Link to="/dang-ky">Tạo tài khoản</Link>
        </p>
      </form>
    </Screen>
  )
}
