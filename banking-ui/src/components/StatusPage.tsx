import { useAccounts } from '../state/useAccounts';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { AccountStatusControl } from './AccountStatusControl';

export function StatusPage() {
  const { accounts, selectedAccount, selectAccount, updateAccount } = useAccounts();
  return (
    <AccountSelectionLayout
      accounts={accounts}
      selectedAccount={selectedAccount}
      onSelectAccount={selectAccount}
      emptyTitle="Look up a customer to begin"
      emptyDescription="Enter a customer number above, then choose one of their accounts to change its status."
      pickerTitle="Choose an account"
    >
      {selectedAccount && <AccountStatusControl account={selectedAccount} onUpdated={updateAccount} />}
    </AccountSelectionLayout>
  );
}
