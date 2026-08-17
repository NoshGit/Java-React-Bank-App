import { useEffect, useState } from 'react';
import type { Transaction } from '../api/types';
import { TransactionRow } from './TransactionRow';
import { Pagination } from './Pagination';
import { BalanceTrendChart } from './charts/BalanceTrendChart';
import { computeBalanceHistoryWithDates } from '../utils/balanceHistory';
import { formatDateTime } from '../utils/format';

type TransactionHistoryProps = {
  transactions: Transaction[];
  loading: boolean;
  error: string | null;
  currentBalance: number;
};

const PAGE_SIZE = 10;

export function TransactionHistory({ transactions, loading, error, currentBalance }: TransactionHistoryProps) {
  const [page, setPage] = useState(1);

  useEffect(() => {
    setPage(1);
  }, [transactions]);

  const pageCount = Math.max(1, Math.ceil(transactions.length / PAGE_SIZE));
  const pageItems = transactions.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  // Trend uses the *full* history, not just the current page, so the line reflects the
  // account's whole real trajectory rather than an arbitrary 10-row window.
  const trendPoints = computeBalanceHistoryWithDates(currentBalance, transactions);

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Recent activity</h2>
      </div>
      {loading && (
        <div className="skeleton-transaction-list">
          {[0, 1, 2].map((i) => (
            <div className="skeleton-line" style={{ width: '100%', height: 44 }} key={i} />
          ))}
        </div>
      )}
      {!loading && error && <p className="error-message">{error}</p>}
      {!loading && !error && transactions.length === 0 && (
        <p className="status-message">No transactions yet for this account.</p>
      )}
      {!loading && !error && transactions.length > 0 && (
        <>
          {trendPoints.length > 1 && <BalanceTrendChart points={trendPoints} />}
          <ul className="transaction-list">
            {pageItems.map((txn) => (
              <TransactionRow
                key={txn.txnId}
                txn={txn}
                meta={
                  <>
                    <span className="transaction-date">{formatDateTime(txn.txnDate)}</span>
                    {txn.description && <span className="transaction-desc">{txn.description}</span>}
                  </>
                }
              />
            ))}
          </ul>
          <Pagination
            page={page}
            pageCount={pageCount}
            onPageChange={setPage}
            totalItems={transactions.length}
            pageSize={PAGE_SIZE}
          />
        </>
      )}
    </section>
  );
}
