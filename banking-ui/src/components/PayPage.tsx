import { useAccounts } from '../state/useAccounts';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { PaymentForm } from './PaymentForm';

type PayPageProps = {
  onComplete: () => void;
};

export function PayPage({ onComplete }: PayPageProps) {
  const { accounts, selectedAccount, selectAccount } = useAccounts();
  return (
    <AccountSelectionLayout
      accounts={accounts}
      selectedAccount={selectedAccount}
      onSelectAccount={selectAccount}
      emptyTitle="No accounts found"
      emptyDescription="We couldn&apos;t find any accounts on your profile yet."
      pickerTitle="Choose a source account"
      pickerLabel="Pay from"
    >
      {selectedAccount && <PaymentForm sourceAccount={selectedAccount} onComplete={onComplete} />}
    </AccountSelectionLayout>
  );
}
