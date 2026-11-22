import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import { emailError } from '../../lib/validation'
import { Button, Field } from '../../ui/controls'
import { Card, ErrorNotice, Screen } from '../../ui/layout'

type IssuedCode = { code: string; expiresAt: string }

export function AdminResetPage() {
  const [email, setEmail] = useState('')
  const [issued, setIssued] = useState<IssuedCode | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const invalid = emailError(email)
    if (invalid) return setError(invalid)
    setBusy(true)
    setError(null)
    setIssued(null)
    setCopied(false)
    try {
      setIssued(await api.post<IssuedCode>('/admin/password-resets', { email: email.trim() }))
    } catch (err) {
      setError(err instanceof ApiError && err.status === 404 ? 'Không có tài khoản nào dùng email này.' : 'Chưa tạo được mã.')
    } finally {
      setBusy(false)
    }
  }

  async function copy() {
    if (!issued) return
    await navigator.clipboard.writeText(issued.code).then(() => setCopied(true), () => setCopied(false))
  }

  const until = issued && new Date(issued.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })

  return (
    <Screen title="Admin · Đặt lại mật khẩu" back="/ho-so">
      <p className="muted">Người dùng nhắn qua Zalo và cho biết email. Tạo mã rồi gửi lại cho họ.</p>
      <form className="stack" onSubmit={submit} noValidate>
        <Field label="Email người dùng" type="email" inputMode="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <ErrorNotice message={error} />
        <Button type="submit" busy={busy}>Tạo mã</Button>
      </form>
      {issued && (
        <Card tone="brand">
          <p className="caption brand-heading">Mã đặt lại</p>
          <p className="code" aria-label={issued.code.split('').join(' ')}>
            {issued.code.slice(0, 4)} {issued.code.slice(4)}
          </p>
          <p className="caption muted">
            Dùng được đến {until} hôm nay. Tạo mã mới thì mã này hết hiệu lực. Mã chỉ hiện một lần.
          </p>
          <Button kind="secondary" onClick={copy}>{copied ? 'Đã sao chép' : 'Sao chép mã'}</Button>
        </Card>
      )}
    </Screen>
  )
}
