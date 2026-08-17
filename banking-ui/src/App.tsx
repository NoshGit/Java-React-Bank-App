import { Suspense, lazy, useEffect, useCallback } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAccounts } from './state/useAccounts';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { CustomerLookup } from './components/CustomerLookup';
import { Alert } from './components/Alert';
import { useAuth } from './auth/AuthContext';
import type { Account } from './api/types';
import './App.css';
import { Landing } from './components/Landing';
import { SupportChat } from './components/SupportChat';

const DashboardPage = lazy(() => import('./components/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const DepositPage = lazy(() => import('./components/DepositPage').then((module) => ({ default: module.DepositPage })));
const WithdrawalPage = lazy(() => import('./components/WithdrawalPage').then((module) => ({ default: module.WithdrawalPage })));
const StatusPage = lazy(() => import('./components/StatusPage').then((module) => ({ default: module.StatusPage })));
const TransferPage = lazy(() => import('./components/TransferPage').then((module) => ({ default: module.TransferPage })));
const PayPage = lazy(() => import('./components/PayPage').then((module) => ({ default: module.PayPage })));
const TransactionHistoryPage = lazy(() =>
  import('./components/TransactionHistoryPage').then((module) => ({ default: module.TransactionHistoryPage })),
);
const TransactionReport = lazy(() => import('./components/TransactionReport').then((module) => ({ default: module.TransactionReport })));

const PAGE_TITLES: Record<string, string> = {
  dashboard: 'Dashboard',
  deposit: 'Deposit Funds',
  withdrawal: 'Withdraw Funds',
  status: 'Account Status',
  transfer: 'Fund Transfer',
  pay: 'Pay Someone',
  history: 'Transaction History',
  report: 'Transaction Report',
};

const KNOWN_ROUTES = new Set(Object.keys(PAGE_TITLES));

function getPageFromPath(pathname: string): string {
  const route = pathname.split('/')[1];
  return route && KNOWN_ROUTES.has(route) ? route : 'dashboard';
}

function getPageSubtitle(page: string, isTeller: boolean): string {
  switch (page) {
    case 'dashboard':
      return isTeller
        ? 'Look up a customer to see their balances and recent trend at a glance.'
        : 'Your balances and recent trend, at a glance.';
    case 'deposit':
      return "Add money to a customer's account.";
    case 'withdrawal':
      return "Withdraw cash from a customer's account.";
    case 'status':
      return "Activate or deactivate a customer's account.";
    case 'transfer':
      return 'Move money between your accounts.';
    case 'pay':
      return 'Send a payment from your account.';
    case 'history':
      return 'Search and review past activity.';
    case 'report':
      return 'Completed transactions by type, across all customers.';
    default:
      return '';
  }
}

function AppLayout({ handleCustomerLookup, lookupLoading }: {
  handleCustomerLookup: (customerNumber: string) => Promise<void>;
  lookupLoading: boolean;
}) {
  const { user, loading: authLoading, isTeller, isAccountHolder } = useAuth();
  const location = useLocation();

  if (authLoading) {
    return (
      <main className="app-main-plain">
        <p className="status-message">Checking sign-in state…</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="app-main-plain">
        <Landing />
      </main>
    );
  }

  if (!isTeller && !isAccountHolder) {
    return (
      <main className="app-main-plain">
        <Alert tone="error">Your account doesn't have a recognized role. Please contact support.</Alert>
      </main>
    );
  }

  const page = getPageFromPath(location.pathname);
  const subtitle = getPageSubtitle(page, isTeller);
  const showLookup = isTeller && page !== 'report';

  return (
    <div className="app">
      <Sidebar />
      <div className="app-body">
        <TopBar title={PAGE_TITLES[page]} subtitle={subtitle} />
        <main className="app-content">
          {showLookup && (
            <>
              <CustomerLookup onLookup={handleCustomerLookup} loading={lookupLoading} />
              <br />
            </>
          )}
          <Suspense fallback={<p className="status-message">Loading page…</p>}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
}

function LandingOrRedirect() {
  const { user, loading: authLoading, isTeller, isAccountHolder } = useAuth();

  if (authLoading) {
    return (
      <main className="app-main-plain">
        <p className="status-message">Checking sign-in state…</p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="app-main-plain">
        <Landing />
      </main>
    );
  }

  if (!isTeller && !isAccountHolder) {
    return (
      <main className="app-main-plain">
        <Alert tone="error">Your account doesn't have a recognized role. Please contact support.</Alert>
      </main>
    );
  }

  return <Navigate to="/dashboard" replace />;
}

function ForbiddenPage() {
  return (
    <div className="empty-state card">
      <h1>403</h1>
      <p>You do not have permission to access this page.</p>
    </div>
  );
}

function NotFoundPage() {
  return (
    <div className="empty-state card">
      <h1>404</h1>
      <p>The page you are looking for cannot be found.</p>
    </div>
  );
}

function AppInner() {
  const { user, isTeller, isAccountHolder } = useAuth();
  const { lookupLoading, lookedUpCustomer, selectedAccount, loadMyAccounts, refreshCustomerAccounts, setLookedUpCustomer, selectAccount, loadTransactions, clearTransactions } = useAccounts();

  useEffect(() => {
    if (user && isAccountHolder) {
      loadMyAccounts();
    }
  }, [user, isAccountHolder, loadMyAccounts]);

  const handleCustomerLookup = useCallback(async (customerNumber: string) => {
    await refreshCustomerAccounts(customerNumber);
    setLookedUpCustomer(customerNumber);
    // Clear selection/transactions
    selectAccount(null);
    clearTransactions();
  }, [refreshCustomerAccounts, setLookedUpCustomer, selectAccount, clearTransactions]);

  async function handleActionComplete() {
    const currentAccountId = selectedAccount?.accountId ?? null;

    let refreshed: Account[] = [];
    if (isAccountHolder) {
      refreshed = await loadMyAccounts();
    } else if (lookedUpCustomer) {
      refreshed = await refreshCustomerAccounts(lookedUpCustomer);
    }

    if (currentAccountId != null) {
      const match = refreshed.find((a) => a.accountId === currentAccountId);
      if (match) {
        selectAccount(match);
      }
      loadTransactions(currentAccountId);
    }
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingOrRedirect />} />
        <Route path="logged-out" element={<LandingOrRedirect />} />

        <Route
          element={
            <AppLayout
              handleCustomerLookup={handleCustomerLookup}
              lookupLoading={lookupLoading}
            />
          }
        >
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="deposit" element={isTeller ? <DepositPage onComplete={handleActionComplete} /> : <ForbiddenPage />} />
          <Route path="withdrawal" element={isTeller ? <WithdrawalPage onComplete={handleActionComplete} /> : <ForbiddenPage />} />
          <Route path="status" element={isTeller ? <StatusPage /> : <ForbiddenPage />} />
          <Route path="transfer" element={<TransferPage onComplete={handleActionComplete} />} />
          <Route path="pay" element={<PayPage onComplete={handleActionComplete} />} />
          <Route path="history" element={<TransactionHistoryPage />} />
          <Route path="report" element={isTeller ? <TransactionReport /> : <ForbiddenPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>

      {/* Customer-support FAQ assistant — available on every screen. */}
      <SupportChat />
    </BrowserRouter>
  );
}

export function App() {
  return <AppInner />;
}
