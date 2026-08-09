import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { supabase } from '../lib/supabase'
import type { RewardOffer } from '../types'

const DEFAULT_OFFERS = [
  { label: '10 min talktime', description: '1 day validity', coin_cost: 200 },
  { label: '30 min talktime', description: '1 day validity', coin_cost: 300 },
  { label: '60 min talktime', description: '1 day validity', coin_cost: 500 },
  { label: '100 MB data', description: '1 day validity', coin_cost: 800 },
  { label: '300 MB data', description: '1 day validity', coin_cost: 1200 },
  { label: 'Unlimited calls', description: '1 day validity', coin_cost: 2000 },
  { label: '1 GB data', description: '3 day validity', coin_cost: 3000 },
]

type OfferForm = {
  id: string | null
  label: string
  description: string
  coin_cost: string
  is_active: boolean
  sort_order: string
}

const emptyForm = (): OfferForm => ({
  id: null,
  label: '',
  description: '',
  coin_cost: '',
  is_active: true,
  sort_order: '0',
})

export function RewardOffersSection({ userId }: { userId: string }) {
  const [offers, setOffers] = useState<RewardOffer[]>([])
  const [form, setForm] = useState<OfferForm>(emptyForm())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const sortedOffers = useMemo(
    () => [...offers].sort((a, b) => a.sort_order - b.sort_order),
    [offers],
  )

  async function fetchOffers() {
    const { data, error } = await supabase
      .from('reward_offers')
      .select('*')
      .order('sort_order', { ascending: true })

    if (error) {
      setError(error.message)
      setLoading(false)
      return
    }

    const rows = (data ?? []) as RewardOffer[]
    if (rows.length === 0) {
      const seedPayload = DEFAULT_OFFERS.map((offer, index) => ({
        user_id: userId,
        label: offer.label,
        description: offer.description,
        coin_cost: offer.coin_cost,
        is_active: true,
        sort_order: index,
      }))
      const { error: seedError } = await supabase.from('reward_offers').insert(seedPayload)
      if (seedError) {
        setError(seedError.message)
      }
      const seeded = await supabase.from('reward_offers').select('*').order('sort_order', { ascending: true })
      if (seeded.error) {
        setError(seeded.error.message)
        setOffers([])
      } else {
        setOffers((seeded.data ?? []) as RewardOffer[])
      }
    } else {
      setOffers(rows)
    }
    setLoading(false)
  }

  useEffect(() => {
    fetchOffers().catch((err: Error) => setError(err.message)).finally(() => setLoading(false))
  }, [])

  function startEdit(offer: RewardOffer) {
    setForm({
      id: offer.id,
      label: offer.label,
      description: offer.description ?? '',
      coin_cost: String(offer.coin_cost),
      is_active: offer.is_active,
      sort_order: String(offer.sort_order),
    })
  }

  function cancelEdit() {
    setForm(emptyForm())
  }

  async function saveOffer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)

    const payload = {
      user_id: userId,
      label: form.label.trim(),
      description: form.description.trim() || null,
      coin_cost: Number(form.coin_cost),
      is_active: form.is_active,
      sort_order: Number(form.sort_order),
    }

    if (!payload.user_id || !payload.label || Number.isNaN(payload.coin_cost)) {
      setError('Fill in label and a valid coin cost.')
      setSaving(false)
      return
    }

    const body = form.id ? { id: form.id, ...payload } : payload
    const { error } = await supabase.from('reward_offers').upsert(body, { onConflict: 'id' })

    if (error) {
      setError(error.message)
    } else {
      await fetchOffers()
      cancelEdit()
    }

    setSaving(false)
  }

  async function removeOffer(offer: RewardOffer) {
    if (!window.confirm(`Delete offer "${offer.label}"? This cannot be undone.`)) return
    setError(null)
    const snapshot = offers
    setOffers((current) => current.filter((item) => item.id !== offer.id))
    const { error } = await supabase.from('reward_offers').delete().eq('id', offer.id)
    if (error) {
      setError(error.message)
      setOffers(snapshot)
    }
  }

  async function moveOffer(offer: RewardOffer, direction: -1 | 1) {
    const index = sortedOffers.findIndex((item) => item.id === offer.id)
    const target = sortedOffers[index + direction]
    if (!target) return
    const updatedCurrent = { ...offer, sort_order: target.sort_order }
    const updatedTarget = { ...target, sort_order: offer.sort_order }
    const { error } = await supabase.from('reward_offers').upsert([updatedCurrent, updatedTarget], { onConflict: 'id' })
    if (error) {
      setError(error.message)
      return
    }
    setOffers((current) =>
      current.map((item) => {
        if (item.id === updatedCurrent.id) return updatedCurrent
        if (item.id === updatedTarget.id) return updatedTarget
        return item
      }),
    )
  }

  return (
    <section className="panel-section">
      {error && <div className="error-box">{error}</div>}

      <form className="inline-form" onSubmit={saveOffer}>
        <input
          placeholder="Label"
          value={form.label}
          onChange={(e) => setForm((curr) => ({ ...curr, label: e.target.value }))}
          required
        />
        <input
          placeholder="Description"
          value={form.description}
          onChange={(e) => setForm((curr) => ({ ...curr, description: e.target.value }))}
        />
        <input
          placeholder="Coin cost"
          type="number"
          min="0"
          value={form.coin_cost}
          onChange={(e) => setForm((curr) => ({ ...curr, coin_cost: e.target.value }))}
          required
        />
        <input
          placeholder="Sort order"
          type="number"
          value={form.sort_order}
          onChange={(e) => setForm((curr) => ({ ...curr, sort_order: e.target.value }))}
        />
        <label className="toggle-row">
          <input
            type="checkbox"
            checked={form.is_active}
            onChange={(e) => setForm((curr) => ({ ...curr, is_active: e.target.checked }))}
          />
          Active
        </label>
        <button className="primary-button" type="submit" disabled={saving}>
          {saving ? 'Saving…' : form.id ? 'Update offer' : 'Add offer'}
        </button>
        {form.id && (
          <button className="secondary-button" type="button" onClick={cancelEdit}>
            Cancel
          </button>
        )}
      </form>

      {loading ? (
        <div className="empty-state compact">Loading offers…</div>
      ) : (
        <div className="management-list">
          {sortedOffers.map((offer, index) => (
            <article className="management-row" key={offer.id}>
              <div className="management-details">
                <strong>
                  {offer.label} <span className={`badge ${offer.is_active ? 'active' : 'inactive'}`}>{offer.is_active ? 'Active' : 'Inactive'}</span>
                </strong>
                <span>{offer.description || 'No description'}</span>
                <span>{offer.coin_cost} coins</span>
              </div>
              <div className="row-actions">
                <button className="secondary-button small-button" type="button" onClick={() => moveOffer(offer, -1)} disabled={index === 0}>
                  ↑
                </button>
                <button className="secondary-button small-button" type="button" onClick={() => moveOffer(offer, 1)} disabled={index === sortedOffers.length - 1}>
                  ↓
                </button>
                <button className="secondary-button small-button" type="button" onClick={() => startEdit(offer)}>
                  Edit
                </button>
                <button className="secondary-button danger-button small-button" type="button" onClick={() => removeOffer(offer)}>
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
