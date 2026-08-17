import type { Transaction } from '../api/types';
import { TransactionRow } from './TransactionRow';
import { formatDateTime } from '../utils/format';

export type RecentActivityItem = Transaction & { accountLabel: string };

export function RecentActivityWidget({ items }: { items: RecentActivityItem[] }) {
  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Recent activity</h2>
        <p className="panel-subtitle">Across all accounts</p>
      </div>
      {items.length === 0 ? (
        <p className="status-message">No transactions yet.</p>
      ) : (
        <ul className="transaction-list">
          {items.map((txn) => (
            <TransactionRow
              key={txn.txnId}
              txn={txn}
              meta={
                <span className="transaction-date">
                  {txn.accountLabel} · {formatDateTime(txn.txnDate)}
                </span>
              }
            />
          ))}
        </ul>
      )}
    </section>
  );
}
