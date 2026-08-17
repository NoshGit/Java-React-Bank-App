export function SignInScreen() {
  return (
    <div className="sign-in-page">
      <section className="sign-in-card card">
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
        <p className="sign-in-subtitle">
          Secure access to your accounts, transfers, and payments.
        </p>
        <a href="/oauth2/authorization/bank-auth" className="btn btn-primary w-100 py-3 fw-semibold">
          Sign in to your account
        </a>
        {/* <div className="sign-in-trust-row">
          <span>256-bit encryption</span>
          <span aria-hidden="true">·</span>
          <span>OAuth 2.0 / OIDC</span>
          <span aria-hidden="true">·</span>
          <span>SOC 2 aligned</span>
        </div> */}
      </section>
    </div>
  );
}
