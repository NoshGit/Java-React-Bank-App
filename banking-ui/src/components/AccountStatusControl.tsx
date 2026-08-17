import { useState } from 'react';
import { ApiError, putAccountStatus } from '../api/client';
import type { Account } from '../api/types';
import { Alert } from './Alert';
import { StatusBadge } from './StatusBadge';
import { IconStatus } from './icons/Icons';

type AccountStatusControlProps = {
  account: Account;
  onUpdated: (account: Account) => void;
};

export function AccountStatusControl({ account, onUpdated }: AccountStatusControlProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nextStatus = account.accountStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

  async function handleToggle() {
    setError(null);
    setSubmitting(true);
    try {
      const updated = await putAccountStatus(account.accountId, { status: nextStatus });
      onUpdated(updated);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not update account status.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Account status</h2>
      </div>
      <div className="status-control-row">
        <StatusBadge status={account.accountStatus} />
        <button type="button" className="btn btn-secondary" onClick={handleToggle} disabled={submitting}>
          <IconStatus />
          {submitting ? 'Updating…' : nextStatus === 'ACTIVE' ? 'Activate account' : 'Deactivate account'}
        </button>
      </div>
      {error && <Alert tone="error">{error}</Alert>}
    </section>
  );
}
