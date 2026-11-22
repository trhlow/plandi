import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import type { Me } from '../../lib/useSession'
import { passwordError } from '../../lib/validation'
import { Button, Field } from '../../ui/controls'
import { ErrorNotice, Screen } from '../../ui/layout'

export function ChangePasswordPage({ me }: { me: Me }) {
  const [currentPassword, setCurrent] = useState('')
  const [newPassword, setNew] = useState('')
  const [touched, setTouched] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const newError = passwordError(newPassword, me.email)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setTouched(true)
    if (newError) return
    setBusy(true)
    setError(null)
    try {
      await api.put('/me/password', { currentPassword, newPassword })
      setDone(true)
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 400
          ? (err.problem?.detail ?? 'Mật khẩu hiện tại không đúng.')
          : 'Chưa đổi được. Thử lại sau.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <Screen title="Đổi mật khẩu" back="/ho-so">
      {done ? (
        <p className="notice-ok" role="status">Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.</p>
      ) : (
        <form className="stack" onSubmit={submit} noValidate>
          <Field label="Mật khẩu hiện tại" type="password" autoComplete="current-password" value={currentPassword}
            onChange={(e) => setCurrent(e.target.value)} />
          <Field label="Mật khẩu mới" type="password" autoComplete="new-password" value={newPassword}
            onChange={(e) => setNew(e.target.value)} helper="Ít nhất 8 ký tự, khác email."
            error={touched ? newError : null} />
          <ErrorNotice message={error} />
          <Button type="submit" busy={busy}>Đổi mật khẩu</Button>
        </form>
      )}
    </Screen>
  )
}
