import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../../lib/api'
import { navigate } from '../../lib/router'
import { useSession } from '../../lib/useSession'
import { Button, Field, ToggleRow } from '../../ui/controls'
import { Card, ErrorNotice, Screen } from '../../ui/layout'
import { fetchConsents, OPTIONAL_PURPOSES, saveConsent, type Purpose } from './consents'

function DeleteAccountSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { setMe } = useSession()
  const dialog = useRef<HTMLDialogElement>(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // <dialog> showModal: trình duyệt tự giữ focus trong bảng và đóng bằng Esc.
  useEffect(() => {
    if (open) dialog.current?.showModal()
    else dialog.current?.close()
  }, [open])

  async function confirm(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.delete('/me', { password })
      setMe(null)
      navigate('/dang-nhap')
    } catch (err) {
      setError(err instanceof ApiError && err.status === 403 ? 'Mật khẩu không đúng.' : 'Chưa xóa được. Thử lại sau.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <dialog ref={dialog} className="sheet" aria-labelledby="delete-title" onClose={onClose}>
      <form onSubmit={confirm}>
        <div className="grabber" />
        <h2 id="delete-title" className="title">Xóa tài khoản?</h2>
        <p className="muted">
          Mọi dữ liệu của bạn bị xóa ngay và không khôi phục được. Bản sao lưu cũ tự xóa sau tối đa 7 ngày.
        </p>
        <Field label="Nhập mật khẩu để xác nhận" type="password" autoComplete="current-password" required
          value={password} onChange={(e) => setPassword(e.target.value)} />
        <ErrorNotice message={error} />
        <Button type="submit" kind="danger" busy={busy}>Xóa vĩnh viễn</Button>
        <Button type="button" kind="secondary" onClick={onClose}>Hủy</Button>
      </form>
    </dialog>
  )
}

export function PrivacyPage() {
  const [consents, setConsents] = useState<Partial<Record<Purpose, boolean>> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [exportMessage, setExportMessage] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    fetchConsents().then((c) => setConsents(c.consents)).catch(() => setError('Không tải được trạng thái đồng ý.'))
  }, [])

  async function toggle(purpose: Purpose, granted: boolean) {
    const previous = consents
    setConsents((c) => ({ ...c, [purpose]: granted }))
    try {
      await saveConsent(purpose, granted)
    } catch {
      setConsents(previous)
      setError('Chưa lưu được thay đổi. Thử lại sau.')
    }
  }

  async function download() {
    setExporting(true)
    setExportMessage(null)
    try {
      const data = await api.get<unknown>('/me/export')
      const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }))
      const a = Object.assign(document.createElement('a'), { href: url, download: 'plandi-du-lieu-cua-toi.json' })
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setExportMessage(
        err instanceof ApiError && err.status === 429
          ? 'Hôm nay bạn đã tải 5 lần. Thử lại ngày mai.'
          : 'Chưa tải được. Thử lại sau.',
      )
    } finally {
      setExporting(false)
    }
  }

  return (
    <Screen title="Quyền riêng tư" back="/ho-so">
      <h2 className="section-title">Bạn cho Plandi dùng</h2>
      {OPTIONAL_PURPOSES.map(({ purpose, title, description }) => (
        <ToggleRow key={purpose} title={title} checked={!!consents?.[purpose]} disabled={!consents}
          description={purpose === 'FOOD_FILTERS' ? 'Nhớ món bạn không ăn được. Tắt đi thì bộ lọc đã lưu bị xóa ngay.' : description}
          onChange={(on) => toggle(purpose, on)} />
      ))}
      <ErrorNotice message={error} />
      <h2 className="section-title">Dữ liệu của bạn</h2>
      <Card>
        <h2>Tải dữ liệu của tôi</h2>
        <p className="caption muted">File JSON gồm mọi thứ Plandi lưu về bạn. Tối đa 5 lần mỗi ngày.</p>
        {exportMessage && <p className="hint hint-error" role="alert">{exportMessage}</p>}
        <Button kind="secondary" onClick={download} busy={exporting}>Tải về</Button>
      </Card>
      <Card tone="danger">
        <h2 className="danger-heading">Xóa tài khoản</h2>
        <p className="caption">Xóa vĩnh viễn tài khoản và mọi dữ liệu. Không khôi phục được.</p>
        <Button kind="danger" onClick={() => setDeleting(true)}>Xóa tài khoản</Button>
      </Card>
      <DeleteAccountSheet open={deleting} onClose={() => setDeleting(false)} />
    </Screen>
  )
}
