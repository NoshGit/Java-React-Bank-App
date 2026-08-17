import type { Account, Transaction } from '../api/types';
import { Sparkline } from './Sparkline';
import { computeBalanceHistory } from '../utils/balanceHistory';
import { AccountCard } from './AccountCard';

type AccountTileProps = {
  account: Account;
  transactions?: Transaction[];
  onSelect: (account: Account) => void;
};

/** Dashboard tile showing a real balance-trend sparkline. `transactions` is fetched once
 *  at the Overview page level (and reused for the activity breakdown/recent-activity
 *  widgets) rather than each tile fetching its own copy. */
export function AccountTile({ account, transactions, onSelect }: AccountTileProps) {
  const history = transactions ? computeBalanceHistory(account.balance, transactions.slice(0, 8)) : null;

  const trendTone: 'positive' | 'negative' | 'neutral' =
    history && history.length > 1
      ? history[history.length - 1] >= history[0]
        ? 'positive'
        : 'negative'
      : 'neutral';

  return (
    <AccountCard
      account={account}
      onSelect={onSelect}
      className="dashboard-tile h-100"
      footer={
        history ? (
          <Sparkline values={history} tone={trendTone} />
        ) : (
          <div className="skeleton-line" style={{ width: 100, height: 28 }} />
        )
      }
    />
  );
}
