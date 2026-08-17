import type { Account } from '../api/types';
import { AccountCard } from './AccountCard';

type AccountPickerProps = {
  accounts: Account[];
  selectedAccountId: number | null;
  onSelect: (account: Account) => void;
  label?: string;
};

/** Compact horizontal account-selector used at the top of every dedicated action page
 *  (Deposit, Withdrawal, Transfer, Pay, Status, Transaction History) -- testUI-style
 *  "pick which account" cards, bound to whichever real accounts are already loaded
 *  (the customer's own, or a teller's currently looked-up customer's). */
export function AccountPicker({ accounts, selectedAccountId, onSelect, label = 'Select account' }: AccountPickerProps) {
  if (accounts.length === 0) return null;

  return (
    <div className="field">
      <label className="form-label">{label}</label>
      <div className="account-picker-grid">
        {accounts.map((account) => {
          const selected = account.accountId === selectedAccountId;
          return <AccountCard key={account.accountId} account={account} selected={selected} onSelect={onSelect} />;
        })}
      </div>
    </div>
  );
}
