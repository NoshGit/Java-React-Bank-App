import { useState } from 'react';
import { ApiError, postDeposit } from '../api/client';
import { Alert } from './Alert';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { TransactionRow } from './TransactionRow';
import { formatCurrency, formatDateTime } from '../utils/format';
import { IconDeposit, IconShield } from './icons/Icons';
import { useAccounts } from '../state/useAccounts';

const QUICK_AMOUNTS = [100, 500, 1000, 5000];

type DepositPageProps = {
  onComplete: () => void;
};

export function DepositPage({ onComplete }: DepositPageProps) {
  const { accounts, selectedAccount, selectAccount, transactions } = useAccounts();
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);

  const recentDeposits = transactions.filter((t) => t.txnType === 'DEPOSIT').slice(0, 3);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedAccount) return;
    setMessage(null);

    const amountNumber = Number(amount);
    if (!Number.isFinite(amountNumber) || amountNumber <= 0) {
      setMessage({ tone: 'error', text: 'Enter an amount greater than zero.' });
      return;
    }

    setSubmitting(true);
    try {
      const result = await postDeposit(selectedAccount.accountId, { amount: amountNumber });
      setMessage({ tone: 'success', text: `Deposit of ${formatCurrency(amountNumber)} completed (${result.status}).` });
      setAmount('');
      onComplete();
    } catch (e) {
      const text = e instanceof ApiError ? e.message : 'Deposit failed. Please try again.';
      setMessage({ tone: 'error', text });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AccountSelectionLayout
      accounts={accounts}
      selectedAccount={selectedAccount}
      onSelectAccount={selectAccount}
      emptyTitle="Look up a customer to begin"
      emptyDescription="Enter a customer number above, then choose one of their accounts to deposit into."
      pickerTitle="Choose an account"
      pickerSubtitle="Select the account you want to fund"
      pickerLabel="Deposit to"
    >
      <section className="panel card">
        <div className="panel-header">
          <h2>Deposit funds</h2>
          <p className="panel-subtitle">Add money to an account securely</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="deposit-amount" className="form-label">Amount</label>
            <div className="input-group">
              <span className="input-group-text">$</span>
              <input
                id="deposit-amount"
                type="number"
                step="0.01"
                min="0.01"
                className="form-control"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                disabled={!selectedAccount}
              />
            </div>
            <div className="quick-amount-row">
              {QUICK_AMOUNTS.map((qa) => (
                <button
                  key={qa}
                  type="button"
                  className="quick-amount-btn"
                  onClick={() => setAmount(qa.toString())}
                  disabled={!selectedAccount}
                >
                  ${qa.toLocaleString()}
                </button>
              ))}
            </div>
          </div>

          <div className="security-note">
            <IconShield />
            <p>This transaction is processed through the bank&apos;s real transfer service and recorded immediately.</p>
          </div>

          <button type="submit" className="btn btn-primary" disabled={!selectedAccount || submitting}>
            <IconDeposit />
            {submitting ? 'Processing…' : `Deposit ${amount ? formatCurrency(Number(amount)) : 'funds'}`}
          </button>
          {message && <Alert tone={message.tone}>{message.text}</Alert>}
        </form>
      </section>

      {selectedAccount && recentDeposits.length > 0 && (
        <section className="panel card">
          <div className="panel-header">
            <h2>Recent deposits</h2>
          </div>
          <ul className="transaction-list">
            {recentDeposits.map((txn) => (
              <TransactionRow key={txn.txnId} txn={txn} meta={<span className="transaction-date">{formatDateTime(txn.txnDate)}</span>} />
            ))}
          </ul>
        </section>
      )}
    </AccountSelectionLayout>
  );
}
