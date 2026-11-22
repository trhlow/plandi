import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import { fetchAreas, locateNearestArea, type Area } from '../../lib/areas'
import { navigate } from '../../lib/router'
import { useSession, type Me } from '../../lib/useSession'
import { emailError, passwordError } from '../../lib/validation'
import { Button, Field, SelectField } from '../../ui/controls'
import { ErrorNotice, Intro, Link, Screen } from '../../ui/layout'

export function RegisterPage() {
  const { setMe } = useSession()
  const [areas, setAreas] = useState<Area[]>([])
  const [form, setForm] = useState({ email: '', password: '', displayName: '', area: '', adultConfirmed: false })
  const [touched, setTouched] = useState(false)
  const [locating, setLocating] = useState(false)
  const [locateMessage, setLocateMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetchAreas()
      .then((list) => {
        setAreas(list)
        setForm((f) => ({ ...f, area: f.area || list[0]?.code || '' }))
      })
      .catch(() => setError('Không tải được danh sách khu vực. Kiểm tra mạng rồi thử lại.'))
  }, [])

  const errors = {
    email: emailError(form.email),
    password: passwordError(form.password, form.email),
    displayName: form.displayName.trim() ? null : 'Nhập tên để người yêu nhận ra bạn.',
  }
  const set = (key: keyof typeof form) => (value: string | boolean) => setForm((f) => ({ ...f, [key]: value }))

  async function useMyLocation() {
    setLocating(true)
    setLocateMessage(null)
    try {
      const area = await locateNearestArea(areas)
      set('area')(area.code)
      setLocateMessage(`Đã chọn khu gần bạn nhất: ${area.name}.`)
    } catch (e) {
      setLocateMessage((e as Error).message)
    } finally {
      setLocating(false)
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setTouched(true)
    if (errors.email || errors.password || errors.displayName || !form.adultConfirmed) return
    setBusy(true)
    setError(null)
    try {
      setMe(await api.post<Me>('/auth/register', { ...form, email: form.email.trim() }))
      navigate('/dong-y')
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? 'Email này đã được đăng ký. Bạn đăng nhập nhé.'
          : 'Chưa tạo được tài khoản. Kiểm tra lại thông tin rồi thử lại.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <Screen>
      <Intro title="Tạo tài khoản" subtitle="Lên kế hoạch đi chơi cuối tuần vừa túi tiền ở Trà Vinh." />
      <form className="stack" onSubmit={submit} noValidate>
        <Field label="Email" type="email" autoComplete="email" inputMode="email" value={form.email}
          onChange={(e) => set('email')(e.target.value)} error={touched ? errors.email : null} />
        <Field label="Mật khẩu" type="password" autoComplete="new-password" value={form.password}
          onChange={(e) => set('password')(e.target.value)}
          helper="Ít nhất 8 ký tự, khác email." error={touched ? errors.password : null} />
        <Field label="Tên hiển thị" autoComplete="nickname" maxLength={50} value={form.displayName}
          onChange={(e) => set('displayName')(e.target.value)} error={touched ? errors.displayName : null} />
        <SelectField label="Khu vực bạn hay đi chơi" value={form.area} onChange={set('area')}
          options={areas.map((a) => ({ value: a.code, label: a.name }))} disabled={areas.length === 0} />
        <div>
          <button type="button" className="text-button-brand text-button" onClick={useMyLocation}
            disabled={locating || areas.length === 0}>
            {locating ? 'Đang tìm vị trí…' : '📍 Dùng vị trí của tôi'}
          </button>
          <p className="caption muted" aria-live="polite">
            {locateMessage ?? 'Vị trí chỉ dùng trên điện thoại để chọn khu gần nhất, không gửi lên server.'}
          </p>
        </div>
        <label className="checkbox">
          <input type="checkbox" checked={form.adultConfirmed} onChange={(e) => set('adultConfirmed')(e.target.checked)} />
          <span>
            Tôi đủ 18 tuổi và đồng ý với <Link to="/dieu-khoan">Điều khoản và Chính sách</Link> của Plandi.
          </span>
        </label>
        {touched && !form.adultConfirmed && <p className="hint hint-error">Bạn cần xác nhận để tạo tài khoản.</p>}
        <ErrorNotice message={error} />
        <Button type="submit" busy={busy}>Tạo tài khoản</Button>
        <p className="center caption">
          <span className="muted">Đã có tài khoản? </span>
          <Link to="/dang-nhap">Đăng nhập</Link>
        </p>
      </form>
    </Screen>
  )
}
