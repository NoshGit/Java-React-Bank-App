import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import {
  IconDeposit,
  IconHistory,
  IconOverview,
  IconPay,
  IconReport,
  IconCopyright,
  IconStatus,
  IconTransfer,
  IconWithdraw,
} from './icons/Icons';

export type Page = 'dashboard' | 'deposit' | 'withdrawal' | 'status' | 'transfer' | 'pay' | 'history' | 'report';

const TELLER_NAV: Array<{ id: Page; label: string; icon: typeof IconOverview }> = [
  { id: 'dashboard', label: 'Dashboard', icon: IconOverview },
  { id: 'deposit', label: 'Deposit', icon: IconDeposit },
  { id: 'withdrawal', label: 'Withdrawal', icon: IconWithdraw },
  { id: 'transfer', label: 'Fund Transfer', icon: IconTransfer },
  { id: 'pay', label: 'Pay', icon: IconPay },
  { id: 'status', label: 'Account Status', icon: IconStatus },
  { id: 'history', label: 'Transaction History', icon: IconHistory },
  { id: 'report', label: 'Reports', icon: IconReport },
];

const CUSTOMER_NAV: Array<{ id: Page; label: string; icon: typeof IconOverview }> = [
  { id: 'dashboard', label: 'Dashboard', icon: IconOverview },
  { id: 'transfer', label: 'Fund Transfer', icon: IconTransfer },
  { id: 'pay', label: 'Pay', icon: IconPay },
  { id: 'history', label: 'Transaction History', icon: IconHistory },
];

export function Sidebar() {
  const { isTeller } = useAuth();
  const items = isTeller ? TELLER_NAV : CUSTOMER_NAV;

  return (
    <nav className="sidebar" aria-label="Primary">
      <div className="sidebar-brand">
        <span className="brand-mark" aria-hidden="true">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7.5V9H22V7.5L12 2Z" fill="currentColor" />
            <path d="M4 10V19H6V10H4Z" fill="currentColor" />
            <path d="M9 10V19H11V10H9Z" fill="currentColor" />
            <path d="M13 10V19H15V10H13Z" fill="currentColor" />
            <path d="M18 10V19H20V10H18Z" fill="currentColor" />
            <path d="M2 21H22V23H2V21Z" fill="currentColor" />
          </svg>
        </span>
        <span className="sidebar-brand-name">Dynamic Bank</span>
      </div>

      <div className="nav nav-pills flex-column gap-1">
        {items.map((item) => (
          <NavLink
            key={item.id}
            end
            to={`/${item.id}`}
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
          >
            {({ isActive }) => (
              <>
                <item.icon />
                <span>{item.label}</span>
                {isActive && <span className="sidebar-nav-dot" />}
              </>
            )}
          </NavLink>
        ))}
      </div>

      <div className="sidebar-footer">
        <div className="sidebar-trust">
          <IconCopyright />
          <span>2026 Dynamic Bank.</span>
        </div>
      </div>
    </nav>
  );
}
