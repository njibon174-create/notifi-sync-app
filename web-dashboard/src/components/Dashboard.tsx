import { useCallback, useEffect, useMemo, useState } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import { formatTimestamp, matchesSearch } from '../lib/format'
import type { Device, NotificationRow, NotificationType } from '../types'
import { Filters } from './Filters'
import { LeaderboardSection } from './LeaderboardSection'
import { NotificationCard } from './NotificationCard'
import { RewardOffersSection } from './RewardOffersSection'
import { SpinControlSection } from './SpinControlSection'

const PAGE_SIZE = 50
const notificationSelect = '*, devices:devices(device_name, device_model)'

type Props = {
  user: User
}

export function Dashboard({ user }: Props) {
  const [devices, setDevices] = useState<Device[]>([])
  const [notifications, setNotifications] = useState<NotificationRow[]>([])
  const [deviceId, setDeviceId] = useState('all')
  const [type, setType] = useState<'all' | NotificationType>('all')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [bulkDeleting, setBulkDeleting] = useState(false)
  const [deletingDeviceIds, setDeletingDeviceIds] = useState<Record<string, boolean>>({})
  const [deletingNotificationIds, setDeletingNotificationIds] = useState<Record<string, boolean>>({})

  const fetchDevices = useCallback(async () => {
    const { data, error } = await supabase
      .from('devices')
      .select('*')
      .order('last_active', { ascending: false })

    if (error) {
      setError(error.message)
      return
    }

    setDevices((data ?? []) as Device[])
  }, [])

  const fetchNotifications = useCallback(
    async (nextPage = 0, append = false) => {
      if (append) setLoadingMore(true)
      else setLoading(true)
      setError(null)

      let query = supabase
        .from('notifications')
        .select(notificationSelect)
        .order('created_at', { ascending: false })
        .range(nextPage * PAGE_SIZE, nextPage * PAGE_SIZE + PAGE_SIZE - 1)

      if (deviceId !== 'all') query = query.eq('device_id', deviceId)
      if (type !== 'all') query = query.eq('type', type)

      const { data, error } = await query

      if (error) {
        setError(error.message)
      } else {
        const rows = (data ?? []) as NotificationRow[]
        setNotifications((current) => (append ? [...current, ...rows] : rows))
        setPage(nextPage)
        setHasMore(rows.length === PAGE_SIZE)
      }

      setLoading(false)
      setLoadingMore(false)
    },
    [deviceId, type],
  )

  useEffect(() => {
    fetchDevices().catch((err: Error) => setError(err.message))
  }, [fetchDevices])

  useEffect(() => {
    fetchNotifications(0, false)
  }, [fetchNotifications])

  useEffect(() => {
    const channel = supabase
      .channel(`notifications-user-${user.id}`)
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'notifications',
          filter: `user_id=eq.${user.id}`,
        },
        async (payload) => {
          const inserted = payload.new as NotificationRow
          const { data, error } = await supabase
            .from('notifications')
            .select(notificationSelect)
            .eq('id', inserted.id)
            .single()

          if (error || !data) return

          setNotifications((current) => {
            const row = data as NotificationRow
            if (current.some((item) => item.id === row.id)) return current
            return [row, ...current]
          })
        },
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [user.id])

  const filteredNotifications = useMemo(() => {
    return notifications.filter((notification) => matchesSearch(notification, search))
  }, [notifications, search])

  const unreadCount = notifications.filter((notification) => !notification.is_read).length

  function setNotificationDeleting(id: string, value: boolean) {
    setDeletingNotificationIds((current) => ({ ...current, [id]: value }))
  }

  function setDeviceDeleting(id: string, value: boolean) {
    setDeletingDeviceIds((current) => ({ ...current, [id]: value }))
  }

  async function markAsRead(notification: NotificationRow) {
    if (notification.is_read) return

    setNotifications((current) =>
      current.map((item) => (item.id === notification.id ? { ...item, is_read: true } : item)),
    )

    const { error } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .eq('id', notification.id)

    if (error) {
      setError(error.message)
      setNotifications((current) =>
        current.map((item) => (item.id === notification.id ? { ...item, is_read: false } : item)),
      )
    }
  }

  async function deleteNotification(notification: NotificationRow) {
    if (!window.confirm('Delete this notification permanently? This cannot be undone.')) return

    setError(null)
    setNotificationDeleting(notification.id, true)

    const snapshot = notifications
    setNotifications((current) => current.filter((item) => item.id !== notification.id))

    const { error } = await supabase.from('notifications').delete().eq('id', notification.id)

    setNotificationDeleting(notification.id, false)

    if (error) {
      setError(error.message)
      setNotifications(snapshot)
    }
  }

  async function deleteAllNotifications() {
    if (
      !window.confirm(
        'Delete ALL notifications for your account? This is permanent and cannot be undone.',
      )
    ) {
      return
    }

    setBulkDeleting(true)
    setError(null)

    const snapshot = notifications
    setNotifications([])
    setHasMore(false)

    const { error } = await supabase.from('notifications').delete().eq('user_id', user.id)

    setBulkDeleting(false)

    if (error) {
      setError(error.message)
      setNotifications(snapshot)
      setHasMore(snapshot.length === PAGE_SIZE)
    }
  }

  async function removeDevice(device: Device) {
    const confirmText = `Removing ${device.device_name} will also delete all notifications that came from it. This cannot be undone.`
    if (!window.confirm(confirmText)) return

    setError(null)
    setDeviceDeleting(device.id, true)

    const deviceSnapshot = devices
    const notificationSnapshot = notifications

    setDevices((current) => current.filter((item) => item.id !== device.id))
    setNotifications((current) => current.filter((item) => item.device_id !== device.id))
    if (deviceId === device.id) setDeviceId('all')

    const { error } = await supabase.from('devices').delete().eq('id', device.id)

    setDeviceDeleting(device.id, false)

    if (error) {
      setError(error.message)
      setDevices(deviceSnapshot)
      setNotifications(notificationSnapshot)
    }
  }

  async function logout() {
    await supabase.auth.signOut()
  }

  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Notification Sync</p>
          <h1>All synced notifications</h1>
          <p className="muted">Signed in as {user.email}</p>
        </div>
        <div className="header-actions">
          <button className="secondary-button danger-button" onClick={deleteAllNotifications} type="button" disabled={bulkDeleting}>
            {bulkDeleting ? 'Deleting…' : 'Clear all notifications'}
          </button>
          <button className="secondary-button" onClick={logout} type="button">
            Logout
          </button>
        </div>
      </header>

      <section className="stats-grid">
        <div className="stat-card">
          <span>Total loaded</span>
          <strong>{notifications.length}</strong>
        </div>
        <div className="stat-card">
          <span>Unread</span>
          <strong>{unreadCount}</strong>
        </div>
        <div className="stat-card">
          <span>Devices</span>
          <strong>{devices.length}</strong>
        </div>
      </section>

      <Filters
        devices={devices}
        deviceId={deviceId}
        type={type}
        search={search}
        onDeviceChange={setDeviceId}
        onTypeChange={setType}
        onSearchChange={setSearch}
      />

      <RewardOffersSection userId={user.id} />
      <LeaderboardSection userId={user.id} />
      <SpinControlSection userId={user.id} devices={devices} />

      <section className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Manage devices</p>
            <h2>Registered devices</h2>
          </div>
          <p className="muted">Remove a phone to delete its synced notifications too.</p>
        </div>

        {devices.length === 0 ? (
          <div className="empty-state compact">
            <strong>No devices registered</strong>
            <span>Once an Android collector registers, it will appear here.</span>
          </div>
        ) : (
          <div className="device-list">
            {devices.map((device) => (
              <article className="device-row" key={device.id}>
                <div className="device-details">
                  <strong>{device.device_name}</strong>
                  <span>{device.device_model || 'No model set'}</span>
                  <span>Last active: {formatTimestamp(device.last_active)}</span>
                </div>
                <button
                  className="secondary-button danger-button small-button"
                  type="button"
                  disabled={deletingDeviceIds[device.id]}
                  onClick={() => removeDevice(device)}
                >
                  {deletingDeviceIds[device.id] ? 'Removing…' : 'Remove'}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      {error && <div className="error-box">{error}</div>}

      <section className="notification-list" aria-live="polite">
        {loading ? (
          <div className="empty-state">Loading notifications…</div>
        ) : filteredNotifications.length === 0 ? (
          <div className="empty-state">
            <strong>No notifications found</strong>
            <span>Try changing filters, or wait for a collector device to upload new items.</span>
          </div>
        ) : (
          filteredNotifications.map((notification) => (
            <NotificationCard
              key={notification.id}
              notification={notification}
              onOpen={markAsRead}
              onDelete={deleteNotification}
              deleting={Boolean(deletingNotificationIds[notification.id])}
            />
          ))
        )}
      </section>

      {!loading && hasMore && (
        <button
          type="button"
          className="load-more-button"
          disabled={loadingMore}
          onClick={() => fetchNotifications(page + 1, true)}
        >
          {loadingMore ? 'Loading…' : 'Load more'}
        </button>
      )}
    </main>
  )
}
