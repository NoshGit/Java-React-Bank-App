import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { IconChevronDown, IconLogout } from './icons/Icons';
import { useTheme } from '../theme/ThemeContext';

type TopBarProps = {
  title: string;
  subtitle?: string;
};

export function TopBar({ title, subtitle }: TopBarProps) {
  const { user, isTeller } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const initials = user
    ? user.fullName
        .split(' ')
        .map((part) => part[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : '';

  const { theme, toggle } = useTheme();

  function handleSignOut() {
    window.location.href = '/logout';
  }

  return (
    <header className="topbar">
      <div>
        <h1 className="topbar-title">{title}</h1>
        {subtitle && <p className="topbar-subtitle">{subtitle}</p>}
      </div>

      <div className="topbar-controls" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        {/* Theme toggle: shown only when user is present (TopBar itself is only rendered when signed-in) */}
        {user && (
          <button
            type="button"
            className="theme-toggle"
            onClick={toggle}
            aria-pressed={theme === 'dark'}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            title={theme === 'dark' ? 'Light theme' : 'Dark theme'}
          >
            {theme === 'dark' ? '🌞' : '🌙'}
          </button>
        )}

        {user && (
          <div className="topbar-user-menu dropdown">
            <button
              type="button"
              className="topbar-user-trigger"
              onClick={() => setMenuOpen((open) => !open)}
              aria-expanded={menuOpen}
              aria-haspopup="menu"
            >
              <div className="user-summary">
                <span className="user-name">{user.fullName}</span>
                <span className={`role-chip role-chip-${isTeller ? 'teller' : 'customer'}`}>
                  {isTeller ? 'Teller' : 'Customer'}
                </span>
              </div>
              <div className="user-avatar" aria-hidden="true">
                {initials}
              </div>
              <IconChevronDown className="topbar-user-chevron" />
            </button>

            {menuOpen && (
              <>
                <button
                  type="button"
                  className="dropdown-backdrop"
                  aria-label="Close menu"
                  onClick={() => setMenuOpen(false)}
                />
                <div className="dropdown-menu dropdown-menu-end show" role="menu">
                  <button type="button" className="dropdown-item d-flex align-items-center gap-2" role="menuitem" onClick={handleSignOut}>
                    <IconLogout />
                    <span>Sign out</span>
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
