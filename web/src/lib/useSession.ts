import { createContext, useContext } from 'react'

export type Me = {
  id: number
  email: string
  displayName: string
  area: string
  role: 'USER' | 'ADMIN'
}

export type Session = {
  /** undefined: đang hỏi server; null: chưa đăng nhập. */
  me: Me | null | undefined
  refresh: () => Promise<void>
  setMe: (me: Me | null) => void
}

export const SessionContext = createContext<Session | null>(null)

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession must be used inside SessionProvider')
  return session
}
