import { useState } from 'react';
import { Alert } from './Alert';
import { IconSearch } from './icons/Icons';

type CustomerLookupProps = {
  onLookup: (customerNumber: string) => Promise<void>;
  loading: boolean;
};

export function CustomerLookup({ onLookup, loading }: CustomerLookupProps) {
  const [customerNumber, setCustomerNumber] = useState('');
  const [error, setError] = useState<string | null>(null);

  const lookUpBtnDisable = loading && !error && !customerNumber.trim();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!customerNumber.trim()) {
      setError('Enter a customer number, e.g. 487-978493.');
      return;
    }
    try {
      await onLookup(customerNumber.trim());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not find that customer.');
    }
  }

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Find a customer</h2>
      </div>
      <form className="inline-form d-flex flex-column flex-sm-row align-items-sm-end" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="customer-number" className="form-label">Customer number</label>
          <input
            id="customer-number"
            type="text"
            className="form-control"
            placeholder="Add account number..."
            value={customerNumber}
            onChange={(e) => setCustomerNumber(e.target.value)}
          />
        </div>
        <button type="submit" className="btn btn-secondary" disabled={lookUpBtnDisable}>
          <IconSearch />
          {lookUpBtnDisable ? 'Searching…' : 'Look up'}
        </button>
      </form>
      {error && <Alert tone="error">{error}</Alert>}
    </section>
  );
}
