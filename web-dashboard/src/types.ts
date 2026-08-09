import type { Session, User } from '@supabase/supabase-js'

export type NotificationType = 'sms' | 'app'

export type Device = {
  id: string
  user_id: string
  device_name: string
  device_model: string | null
  push_token: string | null
  last_active: string
  created_at: string
}

export type NotificationRow = {
  id: string
  user_id: string
  device_id: string
  type: NotificationType
  app_package_name: string | null
  sender: string
  title: string | null
  body: string
  original_timestamp: string
  is_read: boolean
  created_at: string
  devices?: Pick<Device, 'device_name' | 'device_model'> | Pick<Device, 'device_name' | 'device_model'>[] | null
}

export type RewardOffer = {
  id: string
  user_id: string
  label: string
  description: string | null
  coin_cost: number
  is_active: boolean
  sort_order: number
  created_at: string
}

export type LeaderboardEntry = {
  id: string
  user_id: string
  display_name: string
  coins: number
  rank: number | null
  created_at: string
  updated_at: string
}

export type SpinStatus = {
  id: string
  user_id: string
  device_id: string
  last_spin_at: string | null
  is_unlocked: boolean
  created_at: string
  updated_at: string
}

export type AuthState = {
  session: Session | null
  user: User | null
  loading: boolean
}
