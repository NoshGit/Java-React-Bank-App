import type { Transaction } from '../api/types';

const SIGN_BY_TYPE: Record<string, 1 | -1> = {
  DEPOSIT: 1,
  TRANSFER_IN: 1,
  WITHDRAWAL: -1,
  TRANSFER_OUT: -1,
  PAYMENT: -1,
};

export type BalancePoint = { date: string; balance: number };

/**
 * Reconstructs a real balance-over-time series from the current balance and transaction
 * history (most-recent-first, as bankapi returns it) -- not synthetic data. Walks backward:
 * each COMPLETED transaction's signed effect is undone to find the balance immediately after
 * (and, implicitly, before) it. FAILED transactions never touched the balance, so they're
 * skipped when computing the running total, but the point is still returned for that entry.
 *
 * Each returned point uses that transaction's real `txnDate` -- there is no synthetic leading
 * "before any of this" point, since it would have no real date to attach to.
 */
export function computeBalanceHistoryWithDates(currentBalance: number, transactions: Transaction[]): BalancePoint[] {
  const newestFirst = transactions;
  const points: BalancePoint[] = [];
  let running = currentBalance;

  for (const txn of newestFirst) {
    points.push({ date: txn.txnDate, balance: running });
    if (txn.status === 'COMPLETED') {
      const sign = SIGN_BY_TYPE[txn.txnType] ?? 0;
      running -= sign * txn.amount;
    }
  }

  return points.reverse();
}

/** Sparkline-friendly variant: just the balance values, oldest -> newest, plus one leading
 *  "balance before the earliest fetched transaction" point so short sparklines still show a
 *  visible trend line even with only 1-2 transactions. */
export function computeBalanceHistory(currentBalance: number, transactions: Transaction[]): number[] {
  const dated = computeBalanceHistoryWithDates(currentBalance, transactions);
  if (dated.length === 0) return [currentBalance];

  const earliestRunningBalance = (() => {
    let running = currentBalance;
    for (const txn of transactions) {
      if (txn.status === 'COMPLETED') {
        const sign = SIGN_BY_TYPE[txn.txnType] ?? 0;
        running -= sign * txn.amount;
      }
    }
    return running;
  })();

  return [earliestRunningBalance, ...dated.map((p) => p.balance)];
}
