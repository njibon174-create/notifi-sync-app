import type { User } from '@supabase/supabase-js'
import { RewardOffersSection } from './RewardOffersSection'

type Props = {
  user: User
}

export function OffersPage({ user }: Props) {
  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Reward Offers</p>
          <h2>Coin redemption offers</h2>
          <p className="muted">Add, edit, toggle active/inactive, or reorder — changes appear in the Android app immediately.</p>
        </div>
      </div>
      <RewardOffersSection userId={user.id} />
    </div>
  )
}
