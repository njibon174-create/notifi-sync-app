import { useState } from 'react'
import type { User } from '@supabase/supabase-js'

type Props = {
  user: User
}

export function WheelSettingsPage({ user }: Props) {
  const [saved, setSaved] = useState(false)

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Wheel Settings</p>
          <h2>Spinning wheel segments</h2>
          <p className="muted">
            Configure coin values and labels shown on the Android wheel.
          </p>
        </div>
      </div>

      <div className="panel-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Coming soon</p>
            <h2>Wheel segment editor</h2>
          </div>
        </div>

        <div className="empty-state">
          <strong>Wheel segment customisation</strong>
          <span>
            A <code>wheel_segments</code> table and this editor will be added in the next sprint.
            The Android app currently uses hard-coded segment values from the reward offers table.
          </span>
          <span className="muted">
            Expected: add/remove segments, set coin value and label per segment, drag to reorder.
          </span>
        </div>
      </div>
    </div>
  )
}
