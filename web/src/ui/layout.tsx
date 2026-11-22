import type { ReactNode } from 'react'
import { navigate } from '../lib/router'

/** Khung một màn hình điện thoại; trên máy tính căn giữa ở độ rộng 440px. */
export function Screen({ title, back, children }: { title?: string; back?: string; children: ReactNode }) {
  return (
    <div className="screen">
      {title && (
        <header className="app-bar">
          {back && (
            <button type="button" className="icon-button" aria-label="Quay lại" onClick={() => navigate(back)}>
              <svg aria-hidden="true" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="m15 18-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          )}
          <h1 className="app-bar-title">{title}</h1>
        </header>
      )}
      <main className="body">{children}</main>
    </div>
  )
}

export function Intro({ title, subtitle, logo = true }: { title: string; subtitle?: string; logo?: boolean }) {
  return (
    <div className="intro">
      {logo && <img src="/logo.svg" alt="" width={64} height={64} />}
      <h1 className="title">{title}</h1>
      {subtitle && <p className="muted">{subtitle}</p>}
    </div>
  )
}

export function Card({ tone = 'plain', children }: { tone?: 'plain' | 'brand' | 'danger'; children: ReactNode }) {
  return <section className={`card card-${tone}`}>{children}</section>
}

export function ErrorNotice({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p className="notice-error" role="alert">
      {message}
    </p>
  )
}

export function Link({ to, children }: { to: string; children: ReactNode }) {
  return (
    <a className="link" href={`#${to}`}>
      {children}
    </a>
  )
}
