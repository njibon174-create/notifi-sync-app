import type { NotificationRow } from '../types'
import { formatTimestamp, getDeviceLabel, getDisplayTitle } from '../lib/format'

type Props = {
  notification: NotificationRow
  onOpen: (notification: NotificationRow) => void
}

export function NotificationCard({ notification, onOpen }: Props) {
  const isSms = notification.type === 'sms'

  return (
    <button
      className={`notification-card ${notification.is_read ? 'read' : 'unread'}`}
      onClick={() => onOpen(notification)}
      type="button"
      aria-label={`Open ${notification.type} notification from ${notification.sender}`}
    >
      <span className={`type-pill ${isSms ? 'sms' : 'app'}`}>{isSms ? 'SMS' : 'APP'}</span>
      <span className="notification-main">
        <span className="notification-heading">
          {!notification.is_read && <span className="unread-dot" aria-label="Unread" />}
          <span className="notification-title">{getDisplayTitle(notification)}</span>
          <span className="notification-time">{formatTimestamp(notification.created_at)}</span>
        </span>

        {notification.title && notification.type === 'sms' ? (
          <span className="notification-subtitle">{notification.title}</span>
        ) : null}

        <span className="notification-body">{notification.body}</span>
        <span className="notification-meta">
          {getDeviceLabel(notification)} · arrived {formatTimestamp(notification.original_timestamp)}
          {notification.app_package_name ? ` · ${notification.app_package_name}` : ''}
        </span>
      </span>
    </button>
  )
}
