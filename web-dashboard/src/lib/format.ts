import type { NotificationRow } from '../types'

export function formatTimestamp(value: string): string {
  const date = new Date(value)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffSeconds = Math.max(0, Math.floor(diffMs / 1000))

  if (diffSeconds < 60) return diffSeconds <= 5 ? 'just now' : `${diffSeconds}s ago`
  const diffMinutes = Math.floor(diffSeconds / 60)
  if (diffMinutes < 60) return `${diffMinutes}m ago`
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays}d ago`

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function getDeviceLabel(notification: NotificationRow): string {
  const joined = Array.isArray(notification.devices) ? notification.devices[0] : notification.devices
  if (!joined) return 'Unknown device'
  return joined.device_model ? `${joined.device_name} · ${joined.device_model}` : joined.device_name
}

export function getDisplayTitle(notification: NotificationRow): string {
  if (notification.type === 'sms') return notification.sender || 'SMS'
  return notification.title || notification.sender || notification.app_package_name || 'App notification'
}

export function matchesSearch(notification: NotificationRow, search: string): boolean {
  const term = search.trim().toLowerCase()
  if (!term) return true
  return [
    notification.sender,
    notification.title,
    notification.body,
    notification.app_package_name,
    getDeviceLabel(notification),
  ]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(term))
}
