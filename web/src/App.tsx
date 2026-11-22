import { useEffect, type ReactNode } from 'react'
import { AdminResetPage } from './features/account/AdminResetPage'
import { ChangePasswordPage } from './features/account/ChangePasswordPage'
import { ForgotPasswordPage } from './features/account/ForgotPasswordPage'
import { HomePage } from './features/account/HomePage'
import { LoginPage } from './features/account/LoginPage'
import { ProfilePage } from './features/account/ProfilePage'
import { RegisterPage } from './features/account/RegisterPage'
import { ConsentPage } from './features/privacy/ConsentPage'
import { PrivacyPage } from './features/privacy/PrivacyPage'
import { TermsPage } from './features/privacy/TermsPage'
import { navigate, useRoute } from './lib/router'
import { SessionProvider } from './lib/session'
import { useSession, type Me } from './lib/useSession'

const GUEST_ONLY = ['/dang-nhap', '/dang-ky', '/quen-mat-khau']

function Redirect({ to }: { to: string }) {
  useEffect(() => navigate(to), [to])
  return null
}

function Routes() {
  const { me } = useSession()
  const route = useRoute()

  if (me === undefined) return <p className="loading" role="status">Đang tải…</p>
  if (route === '/dieu-khoan') return <TermsPage />
  if (!me) {
    if (route === '/dang-ky') return <RegisterPage />
    if (route === '/quen-mat-khau') return <ForgotPasswordPage />
    return route === '/dang-nhap' ? <LoginPage /> : <Redirect to="/dang-nhap" />
  }
  if (GUEST_ONLY.includes(route)) return <Redirect to="/" />

  const pages: Record<string, (me: Me) => ReactNode> = {
    '/': (m) => <HomePage me={m} />,
    '/dong-y': () => <ConsentPage />,
    '/ho-so': (m) => <ProfilePage me={m} />,
    '/doi-mat-khau': (m) => <ChangePasswordPage me={m} />,
    '/quyen-rieng-tu': () => <PrivacyPage />,
    '/admin/dat-lai-mat-khau': (m) => (m.role === 'ADMIN' ? <AdminResetPage /> : <Redirect to="/" />),
  }
  const page = pages[route]
  return page ? page(me) : <Redirect to="/" />
}

export default function App() {
  return (
    <SessionProvider>
      <Routes />
    </SessionProvider>
  )
}
