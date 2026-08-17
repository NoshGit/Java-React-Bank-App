import type { ReactNode } from 'react';
import type { Transaction } from '../api/types';
import { StatusBadge } from './StatusBadge';
import { formatCurrency } from '../utils/format';
import { transactionSign } from '../utils/transactionSign';
import { IconArrowDownLeft, IconArrowUpRight } from './icons/Icons';

type TransactionRowProps = {
  txn: Transaction;
  meta: ReactNode;
};

export function TransactionRow({ txn, meta }: TransactionRowProps) {
  const sign = transactionSign(txn.txnType);
  const isFailed = txn.status === 'FAILED';

  return (
    <li className="transaction-row">
      <div className={`transaction-icon ${sign > 0 ? 'transaction-icon-credit' : 'transaction-icon-debit'}`}>
        {sign > 0 ? <IconArrowDownLeft /> : <IconArrowUpRight />}
      </div>
      <div className="transaction-main">
        <span className="transaction-type">{txn.txnType.replace(/_/g, ' ')}</span>
        {meta}
      </div>
      <div className="transaction-meta">
        <StatusBadge status={txn.status} />
        <span
          className={`transaction-amount ${isFailed ? 'transaction-amount-muted' : sign > 0 ? 'transaction-amount-credit' : 'transaction-amount-debit'}`}
        >
          {isFailed ? '' : sign > 0 ? '+' : '−'}
          {formatCurrency(txn.amount)}
        </span>
      </div>
    </li>
  );
}
