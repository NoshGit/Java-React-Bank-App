import { useState } from 'react';
import { ApiError, postTransfer } from '../api/client';
import type { Account } from '../api/types';
import { Alert } from './Alert';
import { formatCurrency, maskAccountNumber } from '../utils/format';
import { IconTransfer } from './icons/Icons';

type TransferFormProps = {
  sourceAccount: Account;
  accounts: Account[];
  onComplete: () => void;
};

export function TransferForm({ sourceAccount, accounts, onComplete }: TransferFormProps) {
  const [toAccountId, setToAccountId] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);

  const destinationOptions = accounts.filter((a) => a.accountId !== sourceAccount.accountId);

  const showForm = sourceAccount.accountStatus !== "INACTIVE";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);

    const amountNumber = Number(amount);
    if (!toAccountId || !Number.isFinite(amountNumber) || amountNumber <= 0) {
      setMessage({ tone: 'error', text: 'Choose a destination account and enter an amount greater than zero.' });
      return;
    }

    setSubmitting(true);
    try {
      const result = await postTransfer(sourceAccount.accountId, {
        toAccountId: Number(toAccountId),
        amount: amountNumber,
      });
      setMessage({
        tone: 'success',
        text: `Transfer of ${formatCurrency(amountNumber)} completed (${result.status}).`,
      });
      setToAccountId('');
      setAmount('');
      onComplete();
    } catch (e) {
      const text = e instanceof ApiError ? e.message : 'Transfer failed. Please try again.';
      setMessage({ tone: 'error', text });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Transfer between accounts</h2>
        <p className="panel-subtitle">
          From {maskAccountNumber(sourceAccount.accountNumber)} ({formatCurrency(sourceAccount.balance)} available)
        </p>
      </div>
      {
        showForm && (<form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="to-account" className="form-label">To account</label>
            <select id="to-account" className="form-select" value={toAccountId} onChange={(e) => setToAccountId(e.target.value)}>
              <option value="">Select destination…</option>
              {destinationOptions.map((a) => (
                <option key={a.accountId} value={a.accountId}>
                  {maskAccountNumber(a.accountNumber)} — {a.accountType} ({formatCurrency(a.balance)})
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="transfer-amount" className="form-label">Amount</label>
            <div className="input-group">
              <span className="input-group-text">$</span>
              <input
                id="transfer-amount"
                type="number"
                step="0.01"
                min="0.01"
                className="form-control"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            <IconTransfer />
            {submitting ? 'Processing…' : 'Transfer funds'}
          </button>
          {message && <Alert tone={message.tone}>{message.text}</Alert>}
        </form>)
      }

      {
        !showForm && <Alert tone="error">Unable to transfer funds as selected account is "Inactive"</Alert>
      }

    </section>
  );
}
