export type Page = 'notifications' | 'spin' | 'leaderboard' | 'offers' | 'wheel' | 'devices'

export const NAV_ITEMS: { id: Page; label: string; icon: string }[] = [
  { id: 'notifications', label: 'Notifications', icon: '🔔' },
  { id: 'spin',          label: 'Spin Control',  icon: '🎮' },
  { id: 'leaderboard',   label: 'Leaderboard',   icon: '🏆' },
  { id: 'offers',        label: 'Reward Offers',  icon: '🎁' },
  { id: 'wheel',         label: 'Wheel Settings', icon: '🎡' },
  { id: 'devices',       label: 'Devices',        icon: '📱' },
]
