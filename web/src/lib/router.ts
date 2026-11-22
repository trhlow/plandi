import { useSyncExternalStore } from 'react'

// Điều hướng bằng hash (#/dang-nhap): không cần thư viện, và server không phải trả index.html cho mọi đường dẫn.
const subscribe = (onChange: () => void) => {
  addEventListener('hashchange', onChange)
  return () => removeEventListener('hashchange', onChange)
}

export function useRoute(): string {
  return useSyncExternalStore(subscribe, () => location.hash.slice(1) || '/')
}

export function navigate(path: string) {
  location.hash = '#' + path
}
