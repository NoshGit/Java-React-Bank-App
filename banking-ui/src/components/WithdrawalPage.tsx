import { useState } from 'react';
import { ApiError, postWithdrawal } from '../api/client';
import { Alert } from './Alert';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { TransactionRow } from './TransactionRow';
import { formatCurrency, formatDateTime } from '../utils/format';
import { IconAlert, IconWithdraw } from './icons/Icons';
import { useAccounts } from '../state/useAccounts';

type WithdrawalPageProps = {
  onComplete: () => void;
};

export function WithdrawalPage({ onComplete }: WithdrawalPageProps) {
  const { accounts, selectedAccount, selectAccount, transactions } = useAccounts();
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);

  const recentWithdrawals = transactions.filter((t) => t.txnType === 'WITHDRAWAL').slice(0, 3);
  const insufficient = !!selectedAccount && Number(amount) > selectedAccount.balance;

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
      const result = await postWithdrawal(selectedAccount.accountId, { amount: amountNumber });
      setMessage({ tone: 'success', text: `Withdrawal of ${formatCurrency(amountNumber)} completed (${result.status}).` });
      setAmount('');
      onComplete();
    } catch (e) {
      const text = e instanceof ApiError ? e.message : 'Withdrawal failed. Please try again.';
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
      emptyDescription="Enter a customer number above, then choose one of their accounts to withdraw from."
      pickerTitle="Choose an account"
      pickerSubtitle="Select the account you want to debit"
      pickerLabel="Withdraw from"
    >
      <section className="panel card">
        <div className="panel-header">
          <h2>Withdraw funds</h2>
          <p className="panel-subtitle">Withdraw cash from an account securely</p>
        </div>

        <form onSubmit={handleSubmit}>
          {selectedAccount && (
            <div className="available-balance-row">
              <span>Available balance</span>
              <span className="account-number">{formatCurrency(selectedAccount.balance)}</span>
            </div>
          )}

          <div className="field">
            <label htmlFor="withdrawal-amount" className="form-label">Amount</label>
            <div className="input-group">
              <span className="input-group-text">$</span>
              <input
                id="withdrawal-amount"
                type="number"
                step="0.01"
                min="0.01"
                className={`form-control${insufficient && amount ? ' is-invalid' : ''}`}
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                disabled={!selectedAccount}
              />
            </div>
            {insufficient && amount && (
              <p className="error-message mt-2 d-flex align-items-center gap-1">
                <IconAlert /> Insufficient funds. Available: {formatCurrency(selectedAccount!.balance)}
              </p>
            )}
          </div>

          <button type="submit" className="btn btn-primary" disabled={!selectedAccount || insufficient || submitting}>
            <IconWithdraw />
            {submitting ? 'Processing…' : `Withdraw ${amount ? formatCurrency(Number(amount)) : 'funds'}`}
          </button>
          {message && <Alert tone={message.tone}>{message.text}</Alert>}
        </form>
      </section>

      {selectedAccount && recentWithdrawals.length > 0 && (
        <section className="panel card">
          <div className="panel-header">
            <h2>Recent withdrawals</h2>
          </div>
          <ul className="transaction-list">
            {recentWithdrawals.map((txn) => (
              <TransactionRow key={txn.txnId} txn={txn} meta={<span className="transaction-date">{formatDateTime(txn.txnDate)}</span>} />
            ))}
          </ul>
        </section>
      )}
    </AccountSelectionLayout>
  );
}
