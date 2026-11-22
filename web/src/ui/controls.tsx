import { useId, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  kind?: 'primary' | 'secondary' | 'danger'
  busy?: boolean
}

export function Button({ kind = 'primary', busy, disabled, children, ...rest }: ButtonProps) {
  return (
    <button className={`btn btn-${kind}`} disabled={disabled || busy} aria-busy={busy || undefined} {...rest}>
      {busy ? 'Đang xử lý…' : children}
    </button>
  )
}

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  helper?: string
  error?: string | null
}

export function Field({ label, helper, error, ...input }: FieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const hint = error || helper
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        className="input"
        aria-invalid={error ? true : undefined}
        aria-describedby={hint ? hintId : undefined}
        {...input}
      />
      {hint && (
        <p id={hintId} className={error ? 'hint hint-error' : 'hint'}>
          {hint}
        </p>
      )}
    </div>
  )
}

type SelectFieldProps = {
  label: string
  value: string
  onChange: (value: string) => void
  options: { value: string; label: string }[]
  disabled?: boolean
}

export function SelectField({ label, value, onChange, options, disabled }: SelectFieldProps) {
  const id = useId()
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="select">
        <select id={id} className="input" value={value} disabled={disabled} onChange={(e) => onChange(e.target.value)}>
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
    </div>
  )
}

type ToggleRowProps = {
  title: string
  description: ReactNode
  checked: boolean
  onChange: (checked: boolean) => void
  disabled?: boolean
}

export function ToggleRow({ title, description, checked, onChange, disabled }: ToggleRowProps) {
  const id = useId()
  return (
    <div className="toggle-row">
      <div>
        <p className="toggle-title" id={`${id}-title`}>
          {title}
        </p>
        <p className="caption muted" id={`${id}-desc`}>
          {description}
        </p>
      </div>
      <button
        type="button"
        role="switch"
        className="switch"
        aria-checked={checked}
        aria-labelledby={`${id}-title`}
        aria-describedby={`${id}-desc`}
        disabled={disabled}
        onClick={() => onChange(!checked)}
      />
    </div>
  )
}
