import { useState } from 'react';
import type { BreakdownEntry } from '../utils/breakdown';
import { TypeDonutChart } from './charts/TypeDonutChart';

export function ActivityBreakdown({ entries }: { entries: BreakdownEntry[] }) {
  const [selected, setSelected] = useState<string | null>(null);

  return (
    <section className="panel card">
      <div className="panel-header">
        <h2>Activity breakdown</h2>
        <p className="panel-subtitle">Completed transactions across your accounts</p>
      </div>
      {entries.length === 0 ? (
        <p className="status-message">No completed transactions yet.</p>
      ) : (
        <TypeDonutChart entries={entries} metric="count" selected={selected} onSelect={setSelected} />
      )}
    </section>
  );
}
