import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTransactions } from '../api/client';
import type { Account, Transaction } from '../api/types';
import { AccountTile } from './AccountTile';
import { DashboardHeroCard } from './DashboardHeroCard';
import { DashboardInsightsSection } from './DashboardInsightsSection';
import { QuickActions } from './QuickActions';
import type { QuickAction } from './QuickActions';
import { SkeletonAccountList } from './Skeleton';
import { computeBreakdown } from '../utils/breakdown';
import { maskAccountNumber } from '../utils/format';
import type { RecentActivityItem } from './RecentActivityWidget';
import { useAccounts } from '../state/useAccounts';
import { useAuth } from '../auth/AuthContext';
import './CustomerInfo.css';
import CustomerInfo from './CustomerInfo';

export function DashboardPage() {
  const navigate = useNavigate();
  const { accounts, accountsLoading, accountsError, lookedUpCustomer, selectedAccount, selectAccount, loadTransactions } = useAccounts();
  const { isTeller } = useAuth();
  const [showBalance, setShowBalance] = useState(true);

  const showEmptyTellerPrompt = isTeller && !lookedUpCustomer;

  const handleSelectFromDashboard = useCallback(
    (account: Account) => {
      selectAccount(account);
      loadTransactions(account.accountId);
      navigate('/history');
    },
    [selectAccount, loadTransactions, navigate],
  );

  const handleQuickAction = useCallback(
    (action: QuickAction) => {
      if (accounts.length === 0) return;
      const account = accounts.find((a) => a.accountId === selectedAccount?.accountId) ?? accounts[0];
      selectAccount(account);
      loadTransactions(account.accountId);
      navigate(`/${action}`);
    },
    [accounts, selectedAccount, selectAccount, loadTransactions, navigate],
  );
  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);
  const activeCount = accounts.filter((a) => a.accountStatus === 'ACTIVE').length;

  const [accountTxns, setAccountTxns] = useState<Record<number, Transaction[]>>({});

  useEffect(() => {
    if (accounts.length === 0) {
      setAccountTxns({});
      return;
    }
    let cancelled = false;
    Promise.all(
      accounts.map((account) =>
        getTransactions(account.accountId)
          .then((txns) => [account.accountId, txns] as const)
          .catch(() => [account.accountId, []] as const),
      ),
    ).then((pairs) => {
      if (!cancelled) setAccountTxns(Object.fromEntries(pairs));
    });
    return () => {
      cancelled = true;
    };
  }, [accounts]);

  const allTxns = Object.values(accountTxns).flat();
  const breakdownEntries = computeBreakdown(allTxns);

  const accountLabelById = new Map(
    accounts.map((a) => [a.accountId, `${a.accountType === 'CHECKING' ? 'Checking' : 'Savings'} ${maskAccountNumber(a.accountNumber)}`]),
  );
  const recentActivity: RecentActivityItem[] = [...allTxns]
    .sort((a, b) => new Date(b.txnDate).getTime() - new Date(a.txnDate).getTime())
    .slice(0, 5)
    .map((txn) => ({ ...txn, accountLabel: accountLabelById.get(txn.accountId) ?? '' }));

  return (
    <div className="overview-page">
      {showEmptyTellerPrompt && (
        <div className="empty-state card">
          <h2>Look up a customer to begin</h2>
          <p>Enter a customer number above to view their accounts and balances.</p>
        </div>
      )}

      {!showEmptyTellerPrompt && accountsLoading && (
        <>
          <div className="hero-card hero-card-skeleton">
            <div className="skeleton-line" style={{ width: 160, height: 14 }} />
            <div className="skeleton-line skeleton-line-lg" style={{ width: 220, height: 44, marginTop: 12 }} />
          </div>
          <SkeletonAccountList count={2} />
        </>
      )}

      {!showEmptyTellerPrompt && !accountsLoading && accountsError && <p className="error-message">{accountsError}</p>}

      {!showEmptyTellerPrompt && !accountsLoading && !accountsError && accounts.length === 0 && (
        <div className="empty-state card">
          <h2>No accounts found</h2>
          <p>
            {isTeller
              ? `No accounts are on file for ${lookedUpCustomer}.`
              : "We couldn't find any accounts on your profile yet."}
          </p>
        </div>
      )}

      {!showEmptyTellerPrompt && !accountsLoading && !accountsError && accounts.length > 0 && (
        <>
          {isTeller && (<CustomerInfo lookedUpCustomer={lookedUpCustomer} />
          )}
          <div className="row g-4 align-items-stretch">
            <div className="col-lg-8">
              <DashboardHeroCard
                totalBalance={totalBalance}
                activeCount={activeCount}
                accountsLength={accounts.length}
                showBalance={showBalance}
                isTeller={isTeller}
                lookedUpCustomer={lookedUpCustomer}
                onToggleShowBalance={() => setShowBalance((v) => !v)}
              />
            </div>

            <div className="col-lg-4">
              <div className="dashboard-quick-actions-panel h-100">
                <QuickActions isTeller={isTeller} onAction={handleQuickAction} />
              </div>
            </div>
          </div>

          <div className="dashboard-tiles-header">
            <h2>Accounts</h2>
          </div>
          <div className="row g-4">
            {accounts.map((account) => (
              <div className="col-sm-6 col-xl-4" key={account.accountId}>
                <AccountTile account={account} transactions={accountTxns[account.accountId]} onSelect={handleSelectFromDashboard} />
              </div>
            ))}
          </div>

          <DashboardInsightsSection
            breakdownEntries={breakdownEntries}
            recentActivity={recentActivity}
            allTransactions={allTxns}
          />
        </>
      )}
    </div>
  );
}
