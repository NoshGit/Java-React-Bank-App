import { useEffect, useState } from 'react';
import { ApiError, getTransactionReport } from '../api/client';
import type { TransactionReportLine, TransactionType } from '../api/types';
import type { BreakdownEntry } from '../utils/breakdown';
import { formatCurrency } from '../utils/format';
import { ReportKpiCards } from './ReportKpiCards';
import { TypeDonutChart } from './charts/TypeDonutChart';
import { TypeAmountBarChart } from './charts/TypeAmountBarChart';

export function TransactionReport() {
  const [lines, setLines] = useState<TransactionReportLine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedType, setSelectedType] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getTransactionReport()
      .then((data) => {
        if (!cancelled) setLines(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : 'Could not load the report.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const totalCount = lines.reduce((sum, line) => sum + line.count, 0);
  const entries: BreakdownEntry[] = lines.map((l) => ({
    type: l.txnType as TransactionType,
    count: l.count,
    total: l.totalAmount,
  }));

  return (
    <div className="report-page">
      {loading && <p className="status-message">Loading report…</p>}
      {error && <p className="error-message">{error}</p>}
      {!loading && !error && lines.length === 0 && <p className="status-message">No completed transactions yet.</p>}

      {!loading && !error && lines.length > 0 && (
        <>
          <ReportKpiCards lines={lines} />

          <div className="row g-4">
            <div className="col-lg-6">
              <section className="panel card h-100">
                <div className="panel-header">
                  <h2>Share by count</h2>
                  <p className="panel-subtitle">Which transaction types happen most often</p>
                </div>
                <TypeDonutChart entries={entries} metric="count" selected={selectedType} onSelect={setSelectedType} />
              </section>
            </div>
            <div className="col-lg-6">
              <section className="panel card h-100">
                <div className="panel-header">
                  <h2>Volume by type</h2>
                  <p className="panel-subtitle">Where the dollar amount is actually concentrated</p>
                </div>
                <TypeAmountBarChart entries={entries} selected={selectedType} onSelect={setSelectedType} />
              </section>
            </div>
          </div>

          <section className="panel card">
            <div className="panel-header">
              <h2>Transaction report</h2>
              <p className="panel-subtitle">
                Completed transactions by type, across all customers
                {selectedType && ' — click the highlighted row (or any chart segment) again to clear the filter'}
              </p>
            </div>
            <table className="report-table table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Count</th>
                  <th>Total amount</th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line) => (
                  <tr
                    key={line.txnType}
                    className={
                      selectedType
                        ? line.txnType === selectedType
                          ? 'report-row-highlighted'
                          : 'report-row-dimmed'
                        : undefined
                    }
                    onClick={() => setSelectedType(selectedType === line.txnType ? null : line.txnType)}
                  >
                    <td>{line.txnType.replace(/_/g, ' ')}</td>
                    <td>{line.count}</td>
                    <td>{formatCurrency(line.totalAmount)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td>Total</td>
                  <td>{totalCount}</td>
                  <td>{formatCurrency(lines.reduce((sum, l) => sum + l.totalAmount, 0))}</td>
                </tr>
              </tfoot>
            </table>
          </section>
        </>
      )}
    </div>
  );
}
