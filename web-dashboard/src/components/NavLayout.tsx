import { type FC } from 'react'
import type { User } from '@supabase/supabase-js'
import { supabase } from '../lib/supabase'
import { NAV_ITEMS, type Page } from '../lib/nav'
import { NotificationsPage } from './NotificationsPage'
import { SpinPage } from './SpinPage'
import { LeaderboardPage } from './LeaderboardPage'
import { OffersPage } from './OffersPage'
import { WheelSettingsPage } from './WheelSettingsPage'
import { DevicesPage } from './DevicesPage'

type Props = {
  user: User
  currentPage: Page
  onNavigate: (page: Page) => void
}

export const NavLayout: FC<Props> = ({ user, currentPage, onNavigate }) => {
  async function logout() {
    await supabase.auth.signOut()
  }

  return (
    <div className="app-shell">
      {/* Desktop sidebar */}
      <nav className="sidebar" aria-label="Main navigation">
        <div className="sidebar-brand">
          <div className="brand-mark">NS</div>
          <div>
            <p className="sidebar-title">NotifiSync</p>
            <p className="sidebar-subtitle">{user.email}</p>
          </div>
        </div>

        <ul className="nav-list" role="list">
          {NAV_ITEMS.map((item) => (
            <li key={item.id}>
              <button
                className={`nav-item ${currentPage === item.id ? 'active' : ''}`}
                onClick={() => onNavigate(item.id)}
                type="button"
                aria-current={currentPage === item.id ? 'page' : undefined}
              >
                <span className="nav-icon">{item.icon}</span>
                <span className="nav-label">{item.label}</span>
              </button>
            </li>
          ))}
        </ul>

        <div className="sidebar-footer">
          <button className="nav-item logout-btn" onClick={logout} type="button">
            <span className="nav-icon">🚪</span>
            <span className="nav-label">Logout</span>
          </button>
        </div>
      </nav>

      {/* Mobile top bar */}
      <header className="mobile-topbar" role="banner">
        <div className="mobile-brand">NotifiSync</div>
        <button className="secondary-button small-button mobile-logout" onClick={logout} type="button">
          Logout
        </button>
      </header>

      {/* Mobile tab bar */}
      <nav className="mobile-tabbar" aria-label="Mobile navigation">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            className={`tab-item ${currentPage === item.id ? 'active' : ''}`}
            onClick={() => onNavigate(item.id)}
            type="button"
            aria-current={currentPage === item.id ? 'page' : undefined}
          >
            <span className="tab-icon">{item.icon}</span>
            <span className="tab-label">{item.label}</span>
          </button>
        ))}
      </nav>

      {/* Page content */}
      <main className="main-content">
        {currentPage === 'notifications' && <NotificationsPage user={user} />}
        {currentPage === 'spin' && <SpinPage user={user} />}
        {currentPage === 'leaderboard' && <LeaderboardPage user={user} />}
        {currentPage === 'offers' && <OffersPage user={user} />}
        {currentPage === 'wheel' && <WheelSettingsPage user={user} />}
        {currentPage === 'devices' && <DevicesPage user={user} />}
      </main>
    </div>
  )
}
