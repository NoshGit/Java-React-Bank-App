import type { ReactNode } from 'react';
import type { Account } from '../api/types';
import { AccountPicker } from './AccountPicker';

type AccountSelectionLayoutProps = {
  accounts: Account[];
  selectedAccount: Account | null;
  onSelectAccount: (account: Account) => void;
  emptyTitle: string;
  emptyDescription: string;
  pickerTitle?: string;
  pickerSubtitle?: string;
  pickerLabel?: string;
  children: ReactNode;
};

export function AccountSelectionLayout({
  accounts,
  selectedAccount,
  onSelectAccount,
  emptyTitle,
  emptyDescription,
  pickerTitle = 'Choose an account',
  pickerSubtitle,
  pickerLabel = 'Select account',
  children,
}: AccountSelectionLayoutProps) {
  if (accounts.length === 0) {
    return (
      <div className="empty-state card">
        <h2>{emptyTitle}</h2>
        <p>{emptyDescription}</p>
      </div>
    );
  }

  return (
    <div className="action-page">
      <section className="panel card">
        <div className="panel-header">
          <h2>{pickerTitle}</h2>
          {pickerSubtitle && <p className="panel-subtitle">{pickerSubtitle}</p>}
        </div>
        <AccountPicker accounts={accounts} selectedAccountId={selectedAccount?.accountId ?? null} onSelect={onSelectAccount} label={pickerLabel} />
      </section>
      {children}
    </div>
  );
}
