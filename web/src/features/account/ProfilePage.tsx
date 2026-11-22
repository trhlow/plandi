import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { api } from '../../lib/api'
import { fetchAreas, type Area } from '../../lib/areas'
import { navigate } from '../../lib/router'
import { useSession, type Me } from '../../lib/useSession'
import { Button, Field, SelectField } from '../../ui/controls'
import { Card, ErrorNotice, Screen } from '../../ui/layout'

function Row({ to, children }: { to: string; children: ReactNode }) {
  return (
    <a className="list-row" href={`#${to}`}>
      <span>{children}</span>
      <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="m9 18 6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </a>
  )
}

export function ProfilePage({ me }: { me: Me }) {
  const { setMe } = useSession()
  const [areas, setAreas] = useState<Area[]>([])
  const [displayName, setDisplayName] = useState(me.displayName)
  const [area, setArea] = useState(me.area)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetchAreas().then(setAreas).catch(() => setError('Không tải được danh sách khu vực.'))
  }, [])

  async function save(e: FormEvent) {
    e.preventDefault()
    if (!displayName.trim()) return setError('Tên hiển thị không được để trống.')
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      setMe(await api.patch<Me>('/me', { displayName: displayName.trim(), area }))
      setMessage('Đã lưu.')
    } catch {
      setError('Chưa lưu được. Thử lại sau.')
    } finally {
      setBusy(false)
    }
  }

  async function logout() {
    await api.post('/auth/logout').catch(() => {})
    setMe(null)
    navigate('/dang-nhap')
  }

  return (
    <Screen title="Hồ sơ" back="/">
      <div className="who">
        <span className="avatar" aria-hidden="true">{me.displayName.charAt(0).toUpperCase()}</span>
        <div>
          <p className="toggle-title">{me.displayName}</p>
          <p className="caption muted">{me.email}</p>
        </div>
      </div>
      <Card>
        <form className="stack" onSubmit={save}>
          <Field label="Tên hiển thị" maxLength={50} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <SelectField label="Khu vực bạn hay đi chơi" value={area} onChange={setArea}
            options={areas.map((a) => ({ value: a.code, label: a.name }))} disabled={areas.length === 0} />
          {message && <p className="notice-ok" role="status">{message}</p>}
          <ErrorNotice message={error} />
          <Button type="submit" kind="secondary" busy={busy}>Lưu thay đổi</Button>
        </form>
      </Card>
      <nav className="list" aria-label="Cài đặt">
        <Row to="/doi-mat-khau">Đổi mật khẩu</Row>
        <Row to="/quyen-rieng-tu">Quyền riêng tư</Row>
        <Row to="/dieu-khoan">Điều khoản và Chính sách</Row>
        {me.role === 'ADMIN' && <Row to="/admin/dat-lai-mat-khau">Admin · Đặt lại mật khẩu</Row>}
      </nav>
      <button type="button" className="text-button text-button-danger" onClick={logout}>Đăng xuất</button>
    </Screen>
  )
}
