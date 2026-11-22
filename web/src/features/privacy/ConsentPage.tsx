import { useEffect, useState } from 'react'
import { navigate } from '../../lib/router'
import { Button, ToggleRow } from '../../ui/controls'
import { ErrorNotice, Link, Screen } from '../../ui/layout'
import { fetchConsents, OPTIONAL_PURPOSES, saveConsent, type Consents, type Purpose } from './consents'

/** Ngay sau khi đăng ký, hoặc khi điều khoản đổi phiên bản (API trả TERMS_REQUIRED). */
export function ConsentPage() {
  const [loaded, setLoaded] = useState<Consents | null>(null)
  const [choices, setChoices] = useState<Partial<Record<Purpose, boolean>>>({})
  const [acceptTerms, setAcceptTerms] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetchConsents()
      .then((c) => {
        setLoaded(c)
        setChoices(c.consents)
      })
      .catch(() => setError('Không tải được trạng thái đồng ý.'))
  }, [])

  const termsMissing = loaded !== null && !loaded.consents.TERMS

  async function done() {
    if (!loaded) return
    if (termsMissing && !acceptTerms) return setError('Bạn cần đồng ý điều khoản mới để dùng tiếp.')
    setBusy(true)
    setError(null)
    try {
      if (termsMissing) await saveConsent('TERMS', true)
      for (const { purpose } of OPTIONAL_PURPOSES) {
        if (choices[purpose] !== loaded.consents[purpose]) await saveConsent(purpose, !!choices[purpose])
      }
      navigate('/')
    } catch {
      setError('Chưa lưu được. Thử lại sau.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Screen>
      <div className="intro">
        <h1 className="title">Bạn cho Plandi dùng gì?</h1>
        <p className="muted">Mặc định đều tắt. Bạn đổi lại được bất cứ lúc nào trong Hồ sơ.</p>
      </div>
      {termsMissing && (
        <label className="checkbox card card-brand">
          <input type="checkbox" checked={acceptTerms} onChange={(e) => setAcceptTerms(e.target.checked)} />
          <span>
            Điều khoản và Chính sách vừa được cập nhật (phiên bản {loaded.policyVersion}). Tôi đã đọc và đồng ý với{' '}
            <Link to="/dieu-khoan">bản mới</Link>.
          </span>
        </label>
      )}
      {OPTIONAL_PURPOSES.map(({ purpose, title, description }) => (
        <ToggleRow key={purpose} title={title} description={description} checked={!!choices[purpose]}
          disabled={!loaded} onChange={(on) => setChoices((c) => ({ ...c, [purpose]: on }))} />
      ))}
      <div className="spacer" />
      <ErrorNotice message={error} />
      <Button onClick={done} busy={busy} disabled={!loaded}>Xong</Button>
      <p className="center caption">
        <Link to="/dieu-khoan">Đọc Điều khoản và Chính sách</Link>
      </p>
    </Screen>
  )
}
