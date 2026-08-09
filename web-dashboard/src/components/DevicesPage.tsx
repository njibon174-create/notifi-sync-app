import { useEffect, useState } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import type { Device } from '../types'
import { getRelativeStatus } from '../lib/format'

type Props = {
  user: User
}

export function DevicesPage({ user }: Props) {
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingDeviceIds, setDeletingDeviceIds] = useState<Record<string, boolean>>({})

  async function fetchDevices() {
    const { data, error } = await supabase
      .from('devices')
      .select('*')
      .order('last_active', { ascending: false })

    if (error) {
      setError(error.message)
    } else {
      setDevices((data ?? []) as Device[])
    }
    setLoading(false)
  }

  useEffect(() => {
    fetchDevices()
    const interval = window.setInterval(fetchDevices, 60_000)
    return () => window.clearInterval(interval)
  }, [])

  async function removeDevice(device: Device) {
    const confirmText = `Removing ${device.device_name} will also delete all notifications that came from it. This cannot be undone.`
    if (!window.confirm(confirmText)) return

    setError(null)
    setDeletingDeviceIds((current) => ({ ...current, [device.id]: true }))

    const snapshot = devices
    setDevices((current) => current.filter((item) => item.id !== device.id))

    const { error } = await supabase.from('devices').delete().eq('id', device.id)

    if (error) {
      setError(error.message)
      setDevices(snapshot)
    }
    setDeletingDeviceIds((current) => ({ ...current, [device.id]: false }))
  }

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Devices</p>
          <h2>Registered collector devices</h2>
          <p className="muted">{devices.length} device{devices.length !== 1 ? 's' : ''} · auto-refreshes every 60s</p>
        </div>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <div className="empty-state">Loading devices…</div>
      ) : devices.length === 0 ? (
        <div className="empty-state">
          <strong>No devices registered</strong>
          <span>Once an Android collector registers, it will appear here.</span>
        </div>
      ) : (
        <div className="device-list">
          {devices.map((device) => {
            const status = getRelativeStatus(device.last_active)
            return (
              <article className="device-row" key={device.id}>
                <div className="device-details">
                  <div className="device-name-row">
                    <strong>{device.device_name}</strong>
                    <span className={`online-dot ${status.isOnline ? 'online' : 'offline'}`} />
                  </div>
                  <span>{device.device_model || 'No model set'}</span>
                  <span>
                    {status.isOnline
                      ? '🟢 Online now'
                      : `⚫ Last seen ${status.label}`}
                  </span>
                  <span>Registered {new Date(device.created_at).toLocaleDateString()}</span>
                </div>
                <button
                  className="secondary-button danger-button small-button"
                  type="button"
                  disabled={Boolean(deletingDeviceIds[device.id])}
                  onClick={() => removeDevice(device)}
                >
                  {deletingDeviceIds[device.id] ? 'Removing…' : 'Remove'}
                </button>
              </article>
            )
          })}
        </div>
      )}
    </div>
  )
}
