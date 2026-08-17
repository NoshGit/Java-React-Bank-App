import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { BreakdownEntry } from '../../utils/breakdown';
import { CHART_COLOR_BY_TYPE, TYPE_LABEL } from '../../utils/chartColors';
import { formatCurrency } from '../../utils/format';

type DonutTooltipProps = {
  active?: boolean;
  payload?: Array<{ payload: BreakdownEntry }>;
  metric: 'count' | 'total';
};

function DonutTooltip({ active, payload, metric }: DonutTooltipProps) {
  if (!active || !payload || !payload.length) return null;
  const entry = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-label">{TYPE_LABEL[entry.type] ?? entry.type}</div>
      <div className="chart-tooltip-value">
        {metric === 'total' ? formatCurrency(entry.total) : `${entry.count} transaction${entry.count === 1 ? '' : 's'}`}
      </div>
    </div>
  );
}

type TypeDonutChartProps = {
  entries: BreakdownEntry[];
  metric: 'count' | 'total';
  selected: string | null;
  onSelect: (type: string | null) => void;
};

/** Donut + legend showing share-by-type, driven by real COMPLETED-transaction data.
 *  Clicking a slice or legend row selects that type (used to highlight the matching row
 *  in a table elsewhere on the page); clicking the same one again clears the selection. */
export function TypeDonutChart({ entries, metric, selected, onSelect }: TypeDonutChartProps) {
  const data = entries.map((e) => ({ ...e, value: metric === 'total' ? e.total : e.count }));

  function toggle(type: string) {
    onSelect(selected === type ? null : type);
  }

  return (
    <div className="chart-donut">
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="type"
            innerRadius="62%"
            outerRadius="92%"
            paddingAngle={3}
            animationDuration={600}
            onClick={(d) => toggle((d as unknown as BreakdownEntry).type)}
          >
            {data.map((entry) => (
              <Cell
                key={entry.type}
                fill={CHART_COLOR_BY_TYPE[entry.type]}
                opacity={selected && selected !== entry.type ? 0.3 : 1}
                cursor="pointer"
                stroke="none"
              />
            ))}
          </Pie>
          <Tooltip content={<DonutTooltip metric={metric} />} />
        </PieChart>
      </ResponsiveContainer>
      <ul className="chart-legend">
        {data.map((entry) => (
          <li
            key={entry.type}
            className={`chart-legend-item${selected && selected !== entry.type ? ' chart-legend-item-dim' : ''}`}
            onClick={() => toggle(entry.type)}
          >
            <span className="chart-legend-dot" style={{ background: CHART_COLOR_BY_TYPE[entry.type] }} />
            <span className="chart-legend-label">{TYPE_LABEL[entry.type] ?? entry.type}</span>
            <span className="chart-legend-value">{metric === 'total' ? formatCurrency(entry.total) : entry.count}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
