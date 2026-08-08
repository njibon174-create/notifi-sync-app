import type { MouseEvent, KeyboardEvent } from 'react'
import type { NotificationRow } from '../types'
import { formatTimestamp, getDeviceLabel, getDisplayTitle } from '../lib/format'

type Props = {
  notification: NotificationRow
  onOpen: (notification: NotificationRow) => void
  onDelete: (notification: NotificationRow) => void
  deleting?: boolean
}

export function NotificationCard({ notification, onOpen, onDelete, deleting = false }: Props) {
  const isSms = notification.type === 'sms'

  function handleOpen() {
    onOpen(notification)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      handleOpen()
    }
  }

  function handleDeleteClick(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()
    onDelete(notification)
  }

  return (
    <article
      className={`notification-card ${notification.is_read ? 'read' : 'unread'}`}
      onClick={handleOpen}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={`Open ${notification.type} notification from ${notification.sender}`}
    >
      <span className={`type-pill ${isSms ? 'sms' : 'app'}`}>{isSms ? 'SMS' : 'APP'}</span>
      <span className="notification-main">
        <span className="notification-heading">
          <span className="notification-heading-left">
            {!notification.is_read && <span className="unread-dot" aria-label="Unread" />}
            <span className="notification-title">{getDisplayTitle(notification)}</span>
          </span>
          <span className="notification-heading-right">
            <span className="notification-time">{formatTimestamp(notification.created_at)}</span>
            <button
              type="button"
              className="icon-button danger-icon"
              onClick={handleDeleteClick}
              aria-label={`Delete notification from ${notification.sender}`}
              title="Delete"
              disabled={deleting}
            >
              🗑
            </button>
          </span>
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
    </article>
  )
}
