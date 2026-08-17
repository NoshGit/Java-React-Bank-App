import type { Transaction } from '../api/types';
import type { RecentActivityItem } from './RecentActivityWidget';
import type { BreakdownEntry } from '../utils/breakdown';
import { ActivityBreakdown } from './ActivityBreakdown';
import { RecentActivityWidget } from './RecentActivityWidget';

type DashboardInsightsSectionProps = {
  breakdownEntries: BreakdownEntry[];
  recentActivity: RecentActivityItem[];
  allTransactions: Transaction[];
};

export function DashboardInsightsSection({ breakdownEntries, recentActivity, allTransactions }: DashboardInsightsSectionProps) {
  if (allTransactions.length === 0) return null;

  return (
    <div className="row g-4">
      <div className="col-lg-6">
        <ActivityBreakdown entries={breakdownEntries} />
      </div>
      <div className="col-lg-6">
        <RecentActivityWidget items={recentActivity} />
      </div>
    </div>
  );
}
