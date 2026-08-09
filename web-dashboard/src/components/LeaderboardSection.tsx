import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { supabase } from '../lib/supabase'
import type { LeaderboardEntry } from '../types'

const DEFAULT_LEADERBOARD = [
  { display_name: 'Alice', coins: 1240 },
  { display_name: 'Bob', coins: 980 },
  { display_name: 'Charlie', coins: 720 },
  { display_name: 'Diana', coins: 650 },
  { display_name: 'Eve', coins: 430 },
]

type FormState = {
  id: string | null
  display_name: string
  coins: string
  rank: string
}

const emptyForm = (): FormState => ({
  id: null,
  display_name: '',
  coins: '',
  rank: '',
})

export function LeaderboardSection({ userId }: { userId: string }) {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [form, setForm] = useState<FormState>(emptyForm())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const sortedEntries = useMemo(
    () => [...entries].sort((a, b) => b.coins - a.coins),
    [entries],
  )

  async function fetchEntries() {
    const { data, error } = await supabase
      .from('leaderboard')
      .select('*')
      .order('coins', { ascending: false })

    if (error) {
      setError(error.message)
      setLoading(false)
      return
    }

    const rows = (data ?? []) as LeaderboardEntry[]
    if (rows.length === 0) {
      const seedPayload = DEFAULT_LEADERBOARD.map((item, index) => ({
        user_id: userId,
        display_name: item.display_name,
        coins: item.coins,
        rank: index + 1,
      }))
      const { error: seedError } = await supabase.from('leaderboard').insert(seedPayload)
      if (seedError) setError(seedError.message)
      const seeded = await supabase.from('leaderboard').select('*').order('coins', { ascending: false })
      if (seeded.error) {
        setError(seeded.error.message)
        setEntries([])
      } else {
        setEntries((seeded.data ?? []) as LeaderboardEntry[])
      }
    } else {
      setEntries(rows)
    }
    setLoading(false)
  }

  useEffect(() => {
    fetchEntries().catch((err: Error) => setError(err.message)).finally(() => setLoading(false))
  }, [userId])

  function startEdit(entry: LeaderboardEntry) {
    setForm({
      id: entry.id,
      display_name: entry.display_name,
      coins: String(entry.coins),
      rank: entry.rank ? String(entry.rank) : '',
    })
  }

  function cancelEdit() {
    setForm(emptyForm())
  }

  async function saveEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)

    const payload = {
      user_id: userId,
      display_name: form.display_name.trim(),
      coins: Number(form.coins),
      rank: form.rank.trim() ? Number(form.rank) : null,
      updated_at: new Date().toISOString(),
    }

    if (!payload.display_name || Number.isNaN(payload.coins)) {
      setError('Enter a display name and valid coin count.')
      setSaving(false)
      return
    }

    const body = form.id ? { id: form.id, ...payload } : payload
    const { error } = await supabase.from('leaderboard').upsert(body, { onConflict: 'id' })

    if (error) {
      setError(error.message)
    } else {
      await fetchEntries()
      cancelEdit()
    }

    setSaving(false)
  }

  async function removeEntry(entry: LeaderboardEntry) {
    if (!window.confirm(`Delete leaderboard entry "${entry.display_name}"? This cannot be undone.`)) return
    setError(null)
    const snapshot = entries
    setEntries((current) => current.filter((item) => item.id !== entry.id))
    const { error } = await supabase.from('leaderboard').delete().eq('id', entry.id)
    if (error) {
      setError(error.message)
      setEntries(snapshot)
    }
  }

  return (
    <section className="panel-section">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Leaderboard</p>
          <h2>Editable top scores</h2>
        </div>
        <p className="muted">The Android app will display whatever is stored here.</p>
      </div>

      {error && <div className="error-box">{error}</div>}

      <form className="inline-form" onSubmit={saveEntry}>
        <input
          placeholder="Display name"
          value={form.display_name}
          onChange={(e) => setForm((curr) => ({ ...curr, display_name: e.target.value }))}
          required
        />
        <input
          placeholder="Coins"
          type="number"
          min="0"
          value={form.coins}
          onChange={(e) => setForm((curr) => ({ ...curr, coins: e.target.value }))}
          required
        />
        <input
          placeholder="Rank (optional)"
          type="number"
          value={form.rank}
          onChange={(e) => setForm((curr) => ({ ...curr, rank: e.target.value }))}
        />
        <button className="primary-button" type="submit" disabled={saving}>
          {saving ? 'Saving…' : form.id ? 'Update entry' : 'Add entry'}
        </button>
        {form.id && (
          <button className="secondary-button" type="button" onClick={cancelEdit}>
            Cancel
          </button>
        )}
      </form>

      {loading ? (
        <div className="empty-state compact">Loading leaderboard…</div>
      ) : (
        <div className="management-list">
          {sortedEntries.map((entry, index) => (
            <article className="management-row" key={entry.id}>
              <div className="management-details">
                <strong>
                  {index + 1}. {entry.display_name}
                </strong>
                <span>{entry.coins} coins</span>
                {entry.rank != null && <span>Stored rank: {entry.rank}</span>}
              </div>
              <div className="row-actions">
                <button className="secondary-button small-button" type="button" onClick={() => startEdit(entry)}>
                  Edit
                </button>
                <button className="secondary-button danger-button small-button" type="button" onClick={() => removeEntry(entry)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
