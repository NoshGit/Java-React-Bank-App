import { useState } from 'react';
import { ApiError, postPayment } from '../api/client';
import type { Account } from '../api/types';
import { Alert } from './Alert';
import { formatCurrency, maskAccountNumber } from '../utils/format';
import { IconPay } from './icons/Icons';

type PaymentFormProps = {
  sourceAccount: Account;
  onComplete: () => void;
};

export function PaymentForm({ sourceAccount, onComplete }: PaymentFormProps) {
  const [amount, setAmount] = useState('');
  const [reference, setReference] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);
  const showForm = sourceAccount.accountStatus !== "INACTIVE";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);

    const amountNumber = Number(amount);
    if (!Number.isFinite(amountNumber) || amountNumber <= 0) {
      setMessage({ tone: 'error', text: 'Enter an amount greater than zero.' });
      return;
    }
    if (!reference.trim()) {
      setMessage({ tone: 'error', text: 'A payment reference is required.' });
      return;
    }

    setSubmitting(true);
    try {
      const result = await postPayment(sourceAccount.accountId, {
        amount: amountNumber,
        reference: reference.trim(),
      });
      setMessage({
        tone: 'success',
        text: `Payment of ${formatCurrency(amountNumber)} sent (${result.status}).`,
      });
      setAmount('');
      setReference('');
      onComplete();
    } catch (e) {
      const text = e instanceof ApiError ? e.message : 'Payment failed. Please try again.';
      setMessage({ tone: 'error', text });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Pay someone</h2>
        <p className="panel-subtitle">
          From {maskAccountNumber(sourceAccount.accountNumber)} ({formatCurrency(sourceAccount.balance)} available)
        </p>
      </div>
      {
        showForm && (
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="payment-reference" className="form-label">Reference / payee</label>
              <input
                id="payment-reference"
                type="text"
                className="form-control"
                placeholder="e.g. Acme Utilities"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="payment-amount" className="form-label">Amount</label>
              <div className="input-group">
                <span className="input-group-text">$</span>
                <input
                  id="payment-amount"
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
              <IconPay />
              {submitting ? 'Sending…' : 'Send payment'}
            </button>
            {message && <Alert tone={message.tone}>{message.text}</Alert>}
          </form>
        )
      }
      {
        !showForm && <Alert tone="error">Cannot proceed with payments as account is "Inactive"</Alert>
      }

    </section>
  );
}
