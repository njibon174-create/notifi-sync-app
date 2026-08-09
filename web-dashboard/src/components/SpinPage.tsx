import { useEffect, useState } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import type { Device } from '../types'
import { SpinControlSection } from './SpinControlSection'

type Props = {
  user: User
}

export function SpinPage({ user }: Props) {
  const [devices, setDevices] = useState<Device[]>([])

  useEffect(() => {
    supabase
      .from('devices')
      .select('*')
      .order('last_active', { ascending: false })
      .then(({ data }) => setDevices((data ?? []) as Device[]))
  }, [])

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <p className="eyebrow">Spin Control</p>
          <h2>24-hour cooldown &amp; unlocks</h2>
          <p className="muted">Unlock Spin lets the Android app spin immediately on its next check.</p>
        </div>
      </div>
      <SpinControlSection userId={user.id} devices={devices} />
    </div>
  )
}
