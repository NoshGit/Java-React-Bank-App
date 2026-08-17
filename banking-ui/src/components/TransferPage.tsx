import { useAccounts } from '../state/useAccounts';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { TransferForm } from './TransferForm';

type TransferPageProps = {
  onComplete: () => void;
};

export function TransferPage({ onComplete }: TransferPageProps) {
  const { accounts, selectedAccount, selectAccount } = useAccounts();
  return (
    <AccountSelectionLayout
      accounts={accounts}
      selectedAccount={selectedAccount}
      onSelectAccount={selectAccount}
      emptyTitle="No accounts found"
      emptyDescription="We couldn&apos;t find any accounts on your profile yet."
      pickerTitle="Choose a source account"
      pickerLabel="From account"
    >
      {selectedAccount && <TransferForm sourceAccount={selectedAccount} accounts={accounts} onComplete={onComplete} />}
    </AccountSelectionLayout>
  );
}
