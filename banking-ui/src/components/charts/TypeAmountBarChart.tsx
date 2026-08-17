import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { BreakdownEntry } from '../../utils/breakdown';
import { CHART_COLOR_BY_TYPE, TYPE_LABEL } from '../../utils/chartColors';
import { formatCurrency } from '../../utils/format';

type BarTooltipProps = {
  active?: boolean;
  payload?: Array<{ payload: BreakdownEntry }>;
};

function BarTooltip({ active, payload }: BarTooltipProps) {
  if (!active || !payload || !payload.length) return null;
  const entry = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-label">{TYPE_LABEL[entry.type] ?? entry.type}</div>
      <div className="chart-tooltip-value">{formatCurrency(entry.total)}</div>
      <div className="chart-tooltip-sub">
        {entry.count} transaction{entry.count === 1 ? '' : 's'}
      </div>
    </div>
  );
}

type TypeAmountBarChartProps = {
  entries: BreakdownEntry[];
  selected: string | null;
  onSelect: (type: string | null) => void;
};

/** Horizontal bar chart of dollar volume by type -- deliberately a different metric than
 *  TypeDonutChart's count-share, since the two can tell very different stories (a handful
 *  of large payments vs. many small deposits). Shares click-to-select with the donut. */
export function TypeAmountBarChart({ entries, selected, onSelect }: TypeAmountBarChartProps) {
  const data = [...entries].sort((a, b) => b.total - a.total);

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} layout="vertical" margin={{ top: 4, right: 20, left: 0, bottom: 4 }}>
        <XAxis
          type="number"
          tickFormatter={(v: number) => formatCurrency(v)}
          tick={{ fontSize: 11, fill: '#8a94a6' }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          type="category"
          dataKey="type"
          tickFormatter={(t: string) => TYPE_LABEL[t as keyof typeof TYPE_LABEL] ?? t}
          width={104}
          tick={{ fontSize: 12, fill: '#4a5568' }}
          axisLine={false}
          tickLine={false}
        />
        <Tooltip content={<BarTooltip />} cursor={{ fill: '#fafbfc' }} />
        <Bar
          dataKey="total"
          radius={[0, 6, 6, 0]}
          animationDuration={600}
          onClick={(d) => {
            const type = (d as unknown as BreakdownEntry).type;
            onSelect(selected === type ? null : type);
          }}
          cursor="pointer"
        >
          {data.map((entry) => (
            <Cell key={entry.type} fill={CHART_COLOR_BY_TYPE[entry.type]} opacity={selected && selected !== entry.type ? 0.3 : 1} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
