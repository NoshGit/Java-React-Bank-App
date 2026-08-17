export function SkeletonLine({ width = '100%' }: { width?: string }) {
  return <div className="skeleton-line" style={{ width }} />;
}

export function SkeletonAccountCard() {
  return (
    <div className="account-card skeleton-card card" aria-hidden="true">
      <div className="account-card-top">
        <SkeletonLine width="40%" />
        <SkeletonLine width="20%" />
      </div>
      <div className="skeleton-line skeleton-line-lg" style={{ width: '60%' }} />
      <div className="account-card-bottom">
        <SkeletonLine width="35%" />
        <SkeletonLine width="30%" />
      </div>
    </div>
  );
}

export function SkeletonAccountList({ count = 2 }: { count?: number }) {
  return (
    <ul className="account-card-list">
      {Array.from({ length: count }).map((_, i) => (
        <li key={i}>
          <SkeletonAccountCard />
        </li>
      ))}
    </ul>
  );
}
