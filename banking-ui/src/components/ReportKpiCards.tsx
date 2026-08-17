import type { ReactNode } from 'react';
import type { TransactionReportLine } from '../api/types';
import { formatCurrency } from '../utils/format';
import { TYPE_LABEL } from '../utils/chartColors';
import { IconAccounts, IconPay, IconReport, IconTransfer } from './icons/Icons';

type KpiCard = { label: string; value: string; icon: ReactNode };

export function ReportKpiCards({ lines }: { lines: TransactionReportLine[] }) {
  const totalCount = lines.reduce((sum, l) => sum + l.count, 0);
  const totalAmount = lines.reduce((sum, l) => sum + l.totalAmount, 0);
  const mostCommon = [...lines].sort((a, b) => b.count - a.count)[0];
  const averageSize = totalCount > 0 ? totalAmount / totalCount : 0;

  const cards: KpiCard[] = [
    { label: 'Total transactions', value: totalCount.toLocaleString(), icon: <IconReport /> },
    { label: 'Total volume', value: formatCurrency(totalAmount), icon: <IconTransfer /> },
    {
      label: 'Most common type',
      value: mostCommon
        ? (TYPE_LABEL[mostCommon.txnType as keyof typeof TYPE_LABEL] ?? mostCommon.txnType.replace(/_/g, ' '))
        : '—',
      icon: <IconAccounts />,
    },
    { label: 'Average transaction size', value: formatCurrency(averageSize), icon: <IconPay /> },
  ];

  return (
    <div className="row g-4">
      {cards.map((card) => (
        <div className="col-6 col-lg-3" key={card.label}>
          <div className="kpi-card card h-100">
            <div className="kpi-card-icon">{card.icon}</div>
            <div className="kpi-card-body">
              <span className="kpi-card-label">{card.label}</span>
              <span className="kpi-card-value">{card.value}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
