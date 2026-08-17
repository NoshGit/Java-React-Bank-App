import { useState, useEffect } from 'react';
import { AccountSelectionLayout } from './AccountSelectionLayout';
import { TransactionHistory } from './TransactionHistory';
import { transactionSign } from '../utils/transactionSign';
import { IconSearch } from './icons/Icons';
import { useAccounts } from '../state/useAccounts';
import { useAuth } from '../auth/AuthContext';

type Filter = 'all' | 'credit' | 'debit';

export function TransactionHistoryPage() {
  const { accounts, selectedAccount, selectAccount, transactions, transactionsLoading, transactionsError, loadTransactions, clearTransactions } = useAccounts();

  // When the selected account changes (pick a card), load that account's transactions.
  useEffect(() => {
    if (selectedAccount) {
      loadTransactions(selectedAccount.accountId);
    } else {
      clearTransactions();
    }
  }, [selectedAccount, loadTransactions, clearTransactions]);
  const { isTeller } = useAuth();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<Filter>('all');

  const filtered = transactions.filter((t) => {
    const matchesFilter =
      filter === 'all' || (filter === 'credit' ? transactionSign(t.txnType) > 0 : transactionSign(t.txnType) < 0);
    const q = search.trim().toLowerCase();
    const matchesSearch =
      q === '' || t.txnType.toLowerCase().includes(q) || (t.description ?? '').toLowerCase().includes(q);
    return matchesFilter && matchesSearch;
  });

  return (
    <AccountSelectionLayout
      accounts={accounts}
      selectedAccount={selectedAccount}
      onSelectAccount={selectAccount}
      emptyTitle={isTeller ? 'Look up a customer to begin' : 'No accounts found'}
      emptyDescription={
        isTeller
          ? 'Enter a customer number above, then choose one of their accounts to see its activity.'
          : "We couldn't find any accounts on your profile yet."
      }
      pickerTitle="Choose an account"
    >
      {selectedAccount && (
        <>
          <section className="panel card">
            <div className="history-controls">
              <div className="input-group history-search">
                <span className="input-group-text">
                  <IconSearch />
                </span>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search transactions…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>
              <div className="btn-group" role="group" aria-label="Filter transactions">
                {(['all', 'credit', 'debit'] as const).map((f) => (
                  <button
                    key={f}
                    type="button"
                    className={`btn btn-outline-primary text-capitalize${filter === f ? ' active' : ''}`}
                    onClick={() => setFilter(f)}
                  >
                    {f}
                  </button>
                ))}
              </div>
            </div>
          </section>

          <TransactionHistory
            transactions={filtered}
            loading={transactionsLoading}
            error={transactionsError}
            currentBalance={selectedAccount.balance}
          />
        </>
      )}
    </AccountSelectionLayout>
  );
}
