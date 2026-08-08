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

export type AuthState = {
  session: Session | null
  user: User | null
  loading: boolean
}
