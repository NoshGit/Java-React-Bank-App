import { useContext } from 'react';
import { AccountsContext } from './AccountsContextCore';

export function useAccounts() {
  const ctx = useContext(AccountsContext);
  if (!ctx) throw new Error('useAccounts must be used within AccountsProvider');
  return ctx;
}
