import { useCallback, useEffect, useMemo, useState } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'

type SegmentType = 'coin' | 'gift'

type Segment = {
  key: string
  label: string
  type: SegmentType
  coinValue: number
}

type Props = {
  user: User
}

const MIN_SEGMENTS = 6
const MAX_SEGMENTS = 16

const DEFAULT_SEGMENTS: Segment[] = [
  { key: 's1', label: '0', type: 'coin', coinValue: 0 },
  { key: 's2', label: '2', type: 'coin', coinValue: 2 },
  { key: 's3', label: '4', type: 'coin', coinValue: 4 },
  { key: 's4', label: '📱 Phone', type: 'gift', coinValue: 0 },
  { key: 's5', label: '5', type: 'coin', coinValue: 5 },
  { key: 's6', label: '🎧 Audio', type: 'gift', coinValue: 0 },
  { key: 's7', label: '7', type: 'coin', coinValue: 7 },
  { key: 's8', label: '🎁 Mystery', type: 'gift', coinValue: 0 },
  { key: 's9', label: '9', type: 'coin', coinValue: 9 },
]

const PALETTE = [
  '#6C63FF', '#FF6584', '#43B89C', '#FFB347', '#5C85D6',
  '#FF7043', '#26C6DA', '#AB47BC', '#78909C',
]

function uid(): string {
  return `seg_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`
}

export function WheelSettingsPage({ user }: Props) {
  const [segments, setSegments] = useState<Segment[]>(DEFAULT_SEGMENTS)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [configId, setConfigId] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<string | null>(null)

  const fetchConfig = useCallback(async () => {
    setLoading(true)
    setError(null)
    const { data, error } = await supabase
      .from('wheel_config')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle()

    if (error) {
      setError(error.message)
    } else if (data?.segments && Array.isArray(data.segments) && data.segments.length >= MIN_SEGMENTS) {
      setConfigId(data.id)
      setUpdatedAt(data.updated_at)
      setSegments(
        data.segments.map((s: any) => ({
          key: uid(),
          label: String(s.label ?? ''),
          type: (s.type === 'gift' ? 'gift' : 'coin') as SegmentType,
          coinValue: Number(s.coin_value ?? s.coinValue ?? 0),
        })),
      )
    }
    setLoading(false)
  }, [user.id])

  useEffect(() => {
    fetchConfig()
  }, [fetchConfig])

  function updateSegment(key: string, patch: Partial<Segment>) {
    setSegments((current) =>
      current.map((s) => (s.key === key ? { ...s, ...patch } : s)),
    )
    setSaved(false)
  }

  function addSegment() {
    if (segments.length >= MAX_SEGMENTS) {
      setError(`Maximum ${MAX_SEGMENTS} segments allowed`)
      return
    }
    setSegments((current) => [
      ...current,
      { key: uid(), label: 'New', type: 'coin', coinValue: 1 },
    ])
    setSaved(false)
  }

  function removeSegment(key: string) {
    if (segments.length <= MIN_SEGMENTS) {
      setError(`Minimum ${MIN_SEGMENTS} segments required — cannot remove more`)
      return
    }
    setSegments((current) => current.filter((s) => s.key !== key))
    setSaved(false)
  }

  function moveSegment(key: string, direction: 'up' | 'down') {
    setSegments((current) => {
      const idx = current.findIndex((s) => s.key === key)
      if (idx === -1) return current
      const target = direction === 'up' ? idx - 1 : idx + 1
      if (target < 0 || target >= current.length) return current
      const next = [...current]
      ;[next[idx], next[target]] = [next[target], next[idx]]
      return next
    })
    setSaved(false)
  }

  async function saveConfig() {
    setError(null)
    setSaving(true)
    setSaved(false)

    // Validate
    for (const s of segments) {
      if (!s.label.trim()) {
        setError('All segments need a label')
        setSaving(false)
        return
      }
      if (s.type === 'coin' && (s.coinValue < 0 || !Number.isFinite(s.coinValue))) {
        setError('Coin values must be 0 or higher')
        setSaving(false)
        return
      }
    }
    if (segments.length < MIN_SEGMENTS) {
      setError(`Minimum ${MIN_SEGMENTS} segments required`)
      setSaving(false)
      return
    }

    const payload = segments.map((s) => ({
      label: s.label.trim(),
      type: s.type,
      coin_value: s.type === 'coin' ? s.coinValue : 0,
    }))

    const { data, error } = await supabase
      .from('wheel_config')
      .upsert(
        { user_id: user.id, segments: payload },
        { onConflict: 'user_id' },
      )
      .select('id, updated_at')
      .single()

    setSaving(false)

    if (error) {
      setError(error.message)
    } else if (data) {
      setConfigId(data.id)
      setUpdatedAt(data.updated_at)
      setSaved(true)
    }
  }

  const canDelete = segments.length > MIN_SEGMENTS
  const atMax = segments.length >= MAX_SEGMENTS

  const wheelPreviewColors = useMemo(() => {
    return segments.map((_, i) => PALETTE[i % PALETTE.length])
  }, [segments])

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Wheel Settings</p>
          <h2>Spinning wheel segments</h2>
          <p className="muted">
            Configure the segments shown on the Android game screen. Save to apply changes everywhere.
          </p>
          {updatedAt && (
            <p className="muted small-muted">
              Last saved: {new Date(updatedAt).toLocaleString()}
            </p>
          )}
        </div>
        <div className="header-actions">
          <button
            className="secondary-button"
            type="button"
            onClick={() => {
              if (window.confirm('Reset to default 9 segments? Unsaved changes will be lost.')) {
                setSegments(DEFAULT_SEGMENTS)
                setSaved(false)
              }
            }}
          >
            Reset to defaults
          </button>
          <button
            className="primary-button"
            type="button"
            onClick={saveConfig}
            disabled={saving || loading}
          >
            {saving ? 'Saving…' : saved ? '✓ Saved' : 'Save changes'}
          </button>
        </div>
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="panel-section wheel-settings-grid">
        <div className="wheel-preview-card">
          <p className="eyebrow">Preview</p>
          <div className="wheel-preview">
            <svg viewBox="-110 -110 220 220" className="wheel-preview-svg">
              {segments.map((s, i) => {
                const angle = 360 / segments.length
                const startAngle = i * angle - 90
                const endAngle = startAngle + angle
                const startRad = (startAngle * Math.PI) / 180
                const endRad = (endAngle * Math.PI) / 180
                const x1 = Math.cos(startRad) * 100
                const y1 = Math.sin(startRad) * 100
                const x2 = Math.cos(endRad) * 100
                const y2 = Math.sin(endRad) * 100
                const largeArc = angle > 180 ? 1 : 0
                const path = `M 0 0 L ${x1} ${y1} A 100 100 0 ${largeArc} 1 ${x2} ${y2} Z`
                const midAngle = startAngle + angle / 2
                const midRad = (midAngle * Math.PI) / 180
                const tx = Math.cos(midRad) * 55
                const ty = Math.sin(midRad) * 55
                return (
                  <g key={s.key}>
                    <path d={path} fill={wheelPreviewColors[i]} stroke="#0F0F14" strokeWidth={1} />
                    <text
                      x={tx}
                      y={ty}
                      fill="#FFFFFF"
                      fontSize="11"
                      fontWeight="700"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      transform={`rotate(${midAngle + 90}, ${tx}, ${ty})`}
                    >
                      {s.label.length > 8 ? s.label.slice(0, 8) : s.label}
                    </text>
                  </g>
                )
              })}
              <circle r="18" fill="#1A1A24" stroke="#6C63FF" strokeWidth="2" />
              <text y="4" fill="#F5C542" fontSize="14" textAnchor="middle" fontWeight="700">🪙</text>
              <polygon points="0,-100 -8,-115 8,-115" fill="#E53935" stroke="#FFFFFF" strokeWidth="1" />
            </svg>
          </div>
          <p className="muted small-muted">
            {segments.length} segments · {segments.filter((s) => s.type === 'coin').length} coin ·
            {' '}{segments.filter((s) => s.type === 'gift').length} gift
          </p>
        </div>

        <div className="segment-editor">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Segments</p>
              <h2>Edit each segment</h2>
              <p className="muted">
                Min {MIN_SEGMENTS} · Max {MAX_SEGMENTS}. Use arrows to reorder.
              </p>
            </div>
            <button
              className="secondary-button small-button"
              type="button"
              onClick={addSegment}
              disabled={atMax}
            >
              + Add segment
            </button>
          </div>

          {loading ? (
            <div className="empty-state">Loading wheel config…</div>
          ) : (
            <div className="segment-list">
              {segments.map((segment, index) => (
                <div className="segment-row" key={segment.key}>
                  <div
                    className="segment-color-swatch"
                    style={{ backgroundColor: wheelPreviewColors[index] }}
                    title={`Slot ${index + 1}`}
                  >
                    {index + 1}
                  </div>
                  <input
                    type="text"
                    className="text-input segment-label-input"
                    value={segment.label}
                    onChange={(e) => updateSegment(segment.key, { label: e.target.value })}
                    placeholder="Label"
                    maxLength={32}
                  />
                  <select
                    className="text-input segment-type-select"
                    value={segment.type}
                    onChange={(e) =>
                      updateSegment(segment.key, { type: e.target.value as SegmentType })
                    }
                  >
                    <option value="coin">🪙 Coin</option>
                    <option value="gift">🎁 Gift</option>
                  </select>
                  <input
                    type="number"
                    className="text-input segment-value-input"
                    value={segment.coinValue}
                    min={0}
                    max={999}
                    disabled={segment.type !== 'coin'}
                    onChange={(e) =>
                      updateSegment(segment.key, {
                        coinValue: Number(e.target.value) || 0,
                      })
                    }
                    placeholder="Coins"
                  />
                  <div className="segment-row-actions">
                    <button
                      className="icon-button"
                      type="button"
                      onClick={() => moveSegment(segment.key, 'up')}
                      disabled={index === 0}
                      aria-label="Move up"
                    >
                      ▲
                    </button>
                    <button
                      className="icon-button"
                      type="button"
                      onClick={() => moveSegment(segment.key, 'down')}
                      disabled={index === segments.length - 1}
                      aria-label="Move down"
                    >
                      ▼
                    </button>
                    <button
                      className="icon-button danger-icon"
                      type="button"
                      onClick={() => removeSegment(segment.key)}
                      disabled={!canDelete}
                      aria-label="Remove"
                      title={canDelete ? 'Remove segment' : `Min ${MIN_SEGMENTS} segments required`}
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}