import { IconDeposit, IconHistory, IconShield, IconTransfer } from './icons/Icons';

const FEATURES = [
  {
    icon: IconDeposit,
    title: 'Deposit & Withdraw',
    body: 'Manage your cash instantly with real-time balance updates.',
  },
  {
    icon: IconTransfer,
    title: 'Fund Transfers',
    body: 'Send money to any account using just the account number.',
  },
  {
    icon: IconHistory,
    title: 'Transaction History',
    body: 'Track every transaction with detailed statements.',
  },
  {
    icon: IconShield,
    title: 'Bank-Grade Security',
    body: 'OAuth 2.0-secured sessions keep your accounts protected.',
  },
];

export function Landing() {
  return (
    <div className="landing-page">
      <section className="landing-hero">
        <span className="brand-mark brand-mark-lg" aria-hidden="true">
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7.5V9H22V7.5L12 2Z" fill="currentColor" />
            <path d="M4 10V19H6V10H4Z" fill="currentColor" />
            <path d="M9 10V19H11V10H9Z" fill="currentColor" />
            <path d="M13 10V19H15V10H13Z" fill="currentColor" />
            <path d="M18 10V19H20V10H18Z" fill="currentColor" />
            <path d="M2 21H22V23H2V21Z" fill="currentColor" />
          </svg>
        </span>
        <h1>Dynamic Bank</h1>
        <h5>Banking Made Simple &amp; Secure</h5>
        <p className="landing-hero-subtitle">
          Manage deposits, withdrawals, and fund transfers anytime, anywhere - backed by
          bank-grade authentication.
        </p>
        <a href="/oauth2/authorization/bank-auth" className="btn btn-secondary btn-md fw-semibold">
          Sign in to your account
        </a>
      </section>

      <div className="landing-features">
        {FEATURES.map((feature) => (
          <div className="feature-card card" key={feature.title}>
            <div className="feature-card-icon">
              <feature.icon />
            </div>
            <h3>{feature.title}</h3>
            <p>{feature.body}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
