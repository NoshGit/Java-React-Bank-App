import { useId } from 'react';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { BalancePoint } from '../../utils/balanceHistory';
import { formatCurrency, formatDate } from '../../utils/format';

type TrendTooltipProps = {
  active?: boolean;
  payload?: Array<{ payload: BalancePoint }>;
};

function TrendTooltip({ active, payload }: TrendTooltipProps) {
  if (!active || !payload || !payload.length) return null;
  const point = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-label">{formatDate(point.date)}</div>
      <div className="chart-tooltip-value">{formatCurrency(point.balance)}</div>
    </div>
  );
}

/** Real balance-over-time line for this account, built from its actual dated transaction
 *  history (see computeBalanceHistoryWithDates) -- not a synthetic/smoothed curve. */
export function BalanceTrendChart({ points }: { points: BalancePoint[] }) {
  const gradientId = useId();

  if (points.length < 2) return null;

  return (
    <ResponsiveContainer width="100%" height={180}>
      <AreaChart data={points} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#b98a2e" stopOpacity={0.28} />
            <stop offset="100%" stopColor="#b98a2e" stopOpacity={0} />
          </linearGradient>
        </defs>
        <XAxis
          dataKey="date"
          tickFormatter={(d: string) => formatDate(d)}
          tick={{ fontSize: 11, fill: '#8a94a6' }}
          axisLine={false}
          tickLine={false}
          minTickGap={40}
        />
        <YAxis
          tickFormatter={(v: number) => formatCurrency(v)}
          tick={{ fontSize: 11, fill: '#8a94a6' }}
          axisLine={false}
          tickLine={false}
          width={74}
        />
        <Tooltip content={<TrendTooltip />} />
        <Area
          type="monotone"
          dataKey="balance"
          stroke="#b98a2e"
          strokeWidth={2}
          fill={`url(#${gradientId})`}
          animationDuration={700}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
