import type { User } from '@supabase/supabase-js'
import { LeaderboardSection } from './LeaderboardSection'

type Props = {
  user: User
}

export function LeaderboardPage({ user }: Props) {
  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Leaderboard</p>
          <h2>Top scores</h2>
          <p className="muted">Sorted by coins descending. Android app displays entries from here.</p>
        </div>
      </div>
      <LeaderboardSection userId={user.id} />
    </div>
  )
}
