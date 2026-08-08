import { useCallback, useEffect, useMemo, useState } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import { matchesSearch } from '../lib/format'
import type { Device, NotificationRow, NotificationType } from '../types'
import { Filters } from './Filters'
import { NotificationCard } from './NotificationCard'

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
    fetchDevices().catch((error: Error) => setError(error.message))
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
        <button className="secondary-button" onClick={logout} type="button">
          Logout
        </button>
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
