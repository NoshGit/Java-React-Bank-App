import { formatCurrency } from '../utils/format';
import { IconEye, IconEyeOff, IconShield } from './icons/Icons';

type DashboardHeroCardProps = {
  totalBalance: number;
  activeCount: number;
  accountsLength: number;
  showBalance: boolean;
  isTeller: boolean;
  lookedUpCustomer: string | null;
  onToggleShowBalance: () => void;
};

export function DashboardHeroCard({
  totalBalance,
  activeCount,
  accountsLength,
  showBalance,
  isTeller,
  lookedUpCustomer,
  onToggleShowBalance,
}: DashboardHeroCardProps) {
  return (
    <div className="hero-card h-100">
      <div className="hero-card-label">Total balance</div>
      <div className="hero-card-amount-row">
        <div className="hero-card-amount">{showBalance ? formatCurrency(totalBalance) : '••••••••'}</div>
        <button
          type="button"
          className="hero-card-eye-btn"
          onClick={onToggleShowBalance}
          aria-label={showBalance ? 'Hide balance' : 'Show balance'}
        >
          {showBalance ? <IconEyeOff /> : <IconEye />}
        </button>
      </div>
      <div className="hero-card-meta">
        <IconShield />
        <span>
          {activeCount} of {accountsLength} account{accountsLength === 1 ? '' : 's'} active
        </span>
      </div>
    </div>
  );
}
