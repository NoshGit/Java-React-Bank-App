import type { ReactNode } from 'react';
import type { Account } from '../api/types';
import { StatusBadge } from './StatusBadge';
import { formatCurrency, maskAccountNumber } from '../utils/format';

type AccountCardProps = {
  account: Account;
  selected?: boolean;
  onSelect?: (account: Account) => void;
  className?: string;
  footer?: ReactNode;
};

export function AccountCard({ account, selected = false, onSelect, className, footer }: AccountCardProps) {
  const isDashboardVariant = className?.includes('dashboard-tile');
  const topClassName = isDashboardVariant ? 'dashboard-tile-top' : 'account-card-top';
  const balanceClassName = isDashboardVariant ? 'dashboard-tile-balance' : 'account-balance';
  const bottomClassName = isDashboardVariant ? 'dashboard-tile-bottom' : 'account-card-bottom';

  const content = (
    <>
      <div className={topClassName}>
        <span className="account-type-label">{account.accountType === 'CHECKING' ? 'Checking' : 'Savings'}</span>
        <StatusBadge status={account.accountStatus} />
      </div>
      <div className={balanceClassName}>{formatCurrency(account.balance)}</div>
      <div className={bottomClassName}>
        <span className="account-number">{maskAccountNumber(account.accountNumber)}</span>
        {footer}
      </div>
    </>
  );

  const classes = ['account-card', 'card', selected ? 'account-card-selected' : '', className].filter(Boolean).join(' ');

  if (onSelect) {
    return (
      <button type="button" className={classes} onClick={() => onSelect(account)} aria-pressed={selected}>
        {content}
      </button>
    );
  }

  return <div className={classes}>{content}</div>;
}
