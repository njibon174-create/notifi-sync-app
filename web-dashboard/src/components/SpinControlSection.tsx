import { useEffect, useMemo, useState } from 'react'
import { supabase } from '../lib/supabase'
import type { Device, SpinStatus } from '../types'

type Props = {
  userId: string
  devices: Device[]
}

function formatDuration(ms: number) {
  if (ms <= 0) return 'Ready'
  const totalSeconds = Math.ceil(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${hours}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s left`
}

function getCountdown(lastSpinAt: string | null, isUnlocked: boolean, nowMs: number) {
  if (isUnlocked) return { label: 'Ready', remainingMs: 0 }
  if (!lastSpinAt) return { label: 'Ready', remainingMs: 0 }
  const remainingMs = new Date(lastSpinAt).getTime() + 24 * 60 * 60 * 1000 - nowMs
  return { label: formatDuration(remainingMs), remainingMs }
}

export function SpinControlSection({ userId, devices }: Props) {
  const [statuses, setStatuses] = useState<SpinStatus[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [nowTick, setNowTick] = useState(Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNowTick(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  async function fetchStatuses() {
    const { data, error } = await supabase
      .from('spin_status')
      .select('*')
      .order('updated_at', { ascending: false })

    if (error) {
      setError(error.message)
      setLoading(false)
      return
    }

    setStatuses((data ?? []) as SpinStatus[])
    setLoading(false)
  }

  useEffect(() => {
    fetchStatuses().catch((err: Error) => setError(err.message)).finally(() => setLoading(false))
  }, [userId, devices])

  const statusByDeviceId = useMemo(() => {
    return new Map(statuses.map((status) => [status.device_id, status]))
  }, [statuses])

  async function unlock(device: Device) {
    setError(null)
    const current = statusByDeviceId.get(device.id)
    const payload = {
      user_id: userId,
      device_id: device.id,
      last_spin_at: current?.last_spin_at ?? null,
      is_unlocked: true,
      updated_at: new Date().toISOString(),
    }
    const { error } = await supabase.from('spin_status').upsert(payload, { onConflict: 'device_id' })
    if (error) {
      setError(error.message)
      return
    }
    await fetchStatuses()
  }

  async function resetCooldown(device: Device) {
    setError(null)
    const current = statusByDeviceId.get(device.id)
    const payload = {
      user_id: userId,
      device_id: device.id,
      last_spin_at: current?.last_spin_at ?? null,
      is_unlocked: true,
      updated_at: new Date().toISOString(),
    }
    const { error } = await supabase.from('spin_status').upsert(payload, { onConflict: 'device_id' })
    if (error) {
      setError(error.message)
      return
    }
    await fetchStatuses()
  }

  return (
    <section className="panel-section">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Device spin control</p>
          <h2>24-hour cooldown and unlocks</h2>
        </div>
        <p className="muted">Unlock Spin lets the Android app spin immediately on its next check.</p>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <div className="empty-state compact">Loading spin status…</div>
      ) : (
        <div className="device-list">
          {devices.length === 0 ? (
            <div className="empty-state compact">
              <strong>No devices registered</strong>
              <span>Spin controls appear after a device registers.</span>
            </div>
          ) : (
            devices.map((device) => {
              const status = statusByDeviceId.get(device.id)
              const countdown = getCountdown(status?.last_spin_at ?? null, status?.is_unlocked ?? false, nowTick)
              return (
                <article className="management-row" key={device.id}>
                  <div className="management-details">
                    <strong>{device.device_name}</strong>
                    <span>Last spin: {status?.last_spin_at ? new Date(status.last_spin_at).toLocaleString() : 'Never'}</span>
                    <span>Cooldown: {countdown.label}</span>
                    <span>Unlocked: {status?.is_unlocked ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="row-actions">
                    <button className="secondary-button small-button" type="button" onClick={() => unlock(device)}>
                      Unlock Spin
                    </button>
                    <button className="secondary-button small-button" type="button" onClick={() => resetCooldown(device)}>
                      Reset Cooldown
                    </button>
                  </div>
                </article>
              )
            })
          )}
        </div>
      )}
    </section>
  )
}
