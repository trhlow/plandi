import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { api, ApiError } from './api'
import { SessionContext, type Me } from './useSession'

export function SessionProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null | undefined>(undefined)

  const refresh = useCallback(async () => {
    try {
      setMe(await api.get<Me>('/me'))
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) setMe(null)
      else throw e
    }
  }, [])

  // Lần đầu mở app: hỏi server mình là ai. Lỗi mạng thì coi như chưa đăng nhập.
  useEffect(() => {
    api.get<Me>('/me').then(setMe, () => setMe(null))
  }, [])

  return <SessionContext.Provider value={{ me, refresh, setMe }}>{children}</SessionContext.Provider>
}
