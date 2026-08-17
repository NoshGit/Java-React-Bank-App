import type { Transaction, TransactionType } from '../api/types';

export type BreakdownEntry = { type: TransactionType; count: number; total: number };

/** Aggregates real COMPLETED transactions by type -- used for the Overview activity
 *  breakdown widget. Failed transactions are excluded since they never moved money. */
export function computeBreakdown(transactions: Transaction[]): BreakdownEntry[] {
  const byType = new Map<TransactionType, BreakdownEntry>();
  for (const txn of transactions) {
    if (txn.status !== 'COMPLETED') continue;
    const entry = byType.get(txn.txnType) ?? { type: txn.txnType, count: 0, total: 0 };
    entry.count += 1;
    entry.total += txn.amount;
    byType.set(txn.txnType, entry);
  }
  return [...byType.values()].sort((a, b) => b.total - a.total);
}
